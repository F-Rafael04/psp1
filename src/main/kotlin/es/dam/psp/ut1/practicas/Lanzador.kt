package es.dam.psp.ut1.practicas

import es.dam.psp.ut1.Jvm
import java.io.File
import java.util.concurrent.TimeUnit
// Necesitarás además: import java.io.File y import java.util.concurrent.TimeUnit

/**
 * Lanzador.kt · Práctica 3 · Lanzador de órdenes. Completa los TODO 3.x (letras de la práctica).
 *
 * Apuntes UT-1, apartados 5.1 (ProcessBuilder, Jvm.comandoShell), 5.3 (leer la salida),
 * 6.1 (waitFor con límite, destroy) y 6.2 (no bloquearse leyendo).
 * Cómo ejecutarlo: triángulo ▶ junto a main, sin argumentos; escribe las órdenes en la consola
 * de IntelliJ (p. ej. "echo hola", "ls noexiste", "sleep 10" o, en Windows, "ping -n 11 127.0.0.1").
 *
 * Bucle: pide una orden al usuario, la ejecuta como proceso hijo y muestra
 *   - su salida (stdout y stderr juntas), con cada línea numerada (3.a),
 *   - su código de salida y el tiempo que ha tardado en milisegundos (3.b).
 * Si la orden tarda más de TIMEOUT_S segundos, se termina el proceso y se avisa (3.c).
 * Cada ejecución se añade a historial.txt con el formato:  orden;codigo;milisegundos (3.d)
 * La orden "salir" termina el programa.
 */



const val TIMEOUT_S = 5L

fun main() {
    println(
        "Lanzador PSP · sistema: ${System.getProperty("os.name")}" +
                " · escribe 'salir' para terminar"
    )

    while (true) {
        print("> ")
        val orden = readlnOrNull()?.trim() ?: break

        if (orden == "salir") break
        if (orden.isEmpty()) continue

        // 3.c: juntar stdout y stderr en un fichero temporal.
        val temporal = File.createTempFile("lanzador", ".txt")

        try {
            val builder = ProcessBuilder(Jvm.comandoShell(orden))
                .redirectErrorStream(true)
                .redirectOutput(temporal)

            // 3.b: medir desde el arranque del proceso.
            val inicio = System.nanoTime()
            val proceso = builder.start()

            // Este lanzador no envía entrada al hijo.
            proceso.outputStream.close()

            // 3.c: esperar un máximo de cinco segundos.
            val terminado = proceso.waitFor(
                TIMEOUT_S,
                TimeUnit.SECONDS
            )

            val codigo: Int

            if (terminado) {
                codigo = proceso.exitValue()
            } else {
                // Guardamos los descendientes antes de terminar al padre.
                val hijos = mutableListOf<ProcessHandle>()
                val descendientes = proceso.descendants()

                try {
                    descendientes.forEach { hijos.add(it) }
                } finally {
                    descendientes.close()
                }

                // Solicitar la terminación de hijos y padre.
                hijos.asReversed().forEach { it.destroy() }
                proceso.destroy()

                // Forzar la terminación de los que sigan vivos.
                hijos.asReversed().forEach {
                    if (it.isAlive) {
                        it.destroyForcibly()
                    }
                }

                if (proceso.isAlive) {
                    proceso.destroyForcibly()
                }

                proceso.waitFor()
                codigo = -1
            }

            val milisegundos =
                (System.nanoTime() - inicio) / 1_000_000

            // 3.a: mostrar la salida con líneas numeradas.
            temporal.useLines { lineas ->
                lineas.forEachIndexed { indice, linea ->
                    println("${indice + 1}: $linea")
                }
            }

            if (!terminado) {
                println(
                    "Tiempo límite de $TIMEOUT_S segundos superado. " +
                            "Se ha terminado el proceso y sus descendientes."
                )
            }

            // 3.b: mostrar código y tiempo.
            println("[código $codigo · $milisegundos ms]")

            // 3.d: añadir la ejecución al historial.
            File("historial.txt").appendText(
                "$orden;$codigo;$milisegundos${System.lineSeparator()}"
            )
        } finally {
            // Borrar el fichero temporal al finalizar.
            if (!temporal.delete()) {
                temporal.deleteOnExit()
            }
        }
    }

    println("Hasta luego")
}