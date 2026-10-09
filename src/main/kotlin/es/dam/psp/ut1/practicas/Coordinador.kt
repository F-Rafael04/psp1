package es.dam.psp.ut1.practicas

import es.dam.psp.ut1.Jvm
import java.io.File
import kotlin.time.measureTime

data class Resultado(
    val fichero: String,
    val lineas: Long,
    val palabras: Long,
    val caracteres: Long,
    val masFrecuente: String
)

const val CLASE_HIJO = "es.dam.psp.ut1.practicas.ContadorKt"

// 4.a: arrancar el hijo con el fichero como entrada.
fun lanzarContador(fichero: File): Process {
    return Jvm.proceso(CLASE_HIJO)
        .redirectInput(fichero)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
}

// 4.b: leer la salida, esperar y construir el resultado.
fun recogerResultado(fichero: File, hijo: Process): Resultado {
    val salida = hijo.inputStream.bufferedReader().use {
        it.readText()
    }

    val codigo = hijo.waitFor()

    if (codigo != 0) {
        throw IllegalStateException(
            "Error al procesar ${fichero.name}: código de salida $codigo"
        )
    }

    val partes = salida.trimEnd('\r', '\n').split(";", limit = 4)

    if (partes.size != 4) {
        throw IllegalStateException(
            "Salida incorrecta para ${fichero.name}: $salida"
        )
    }

    val lineas = partes[0].toLongOrNull()
    val palabras = partes[1].toLongOrNull()
    val caracteres = partes[2].toLongOrNull()

    if (lineas == null || palabras == null || caracteres == null) {
        throw IllegalStateException(
            "Recuentos incorrectos para ${fichero.name}: $salida"
        )
    }

    return Resultado(
        fichero = fichero.name,
        lineas = lineas,
        palabras = palabras,
        caracteres = caracteres,
        masFrecuente = partes[3]
    )
}

// 4.c: lanzar un hijo y recoger su resultado antes del siguiente.
fun secuencial(ficheros: List<File>): List<Resultado> {
    return ficheros.map { fichero ->
        val hijo = lanzarContador(fichero)
        recogerResultado(fichero, hijo)
    }
}

// 4.d: lanzar todos los hijos antes de recoger los resultados.
fun concurrente(ficheros: List<File>): List<Resultado> {
    val hijos = ficheros.map { fichero ->
        fichero to lanzarContador(fichero)
    }

    return hijos.map { (fichero, hijo) ->
        recogerResultado(fichero, hijo)
    }
}

fun main() {
    val ficheros = File("datos")
        .listFiles { f -> f.isFile && f.extension == "txt" }
        ?.sortedBy { it.name }

    if (ficheros.isNullOrEmpty()) {
        println("No hay ficheros en datos/. Ejecuta antes GeneradorDatos.kt")
        return
    }

    println(
        "Procesadores lógicos disponibles: " +
                Runtime.getRuntime().availableProcessors()
    )

    lateinit var r1: List<Resultado>
    lateinit var r2: List<Resultado>

    val t1 = measureTime { r1 = secuencial(ficheros) }
    val t2 = measureTime { r2 = concurrente(ficheros) }

    // 4.e: comprobar que ambos modos producen el mismo resultado.
    check(r1 == r2) {
        "Los resultados secuenciales y concurrentes no coinciden."
    }

    println("\nLos dos modos producen los mismos resultados.\n")

    println(
        "%-22s %12s %12s %14s %s".format(
            "FICHERO", "LÍNEAS", "PALABRAS",
            "CARACTERES", "MÁS FRECUENTE"
        )
    )

    r1.forEach { resultado ->
        println(
            "%-22s %12d %12d %14d %s".format(
                resultado.fichero,
                resultado.lineas,
                resultado.palabras,
                resultado.caracteres,
                resultado.masFrecuente
            )
        )
    }

    println(
        "%-22s %12d %12d %14d %s".format(
            "TOTAL",
            r1.sumOf { it.lineas },
            r1.sumOf { it.palabras },
            r1.sumOf { it.caracteres },
            "-"
        )
    )

    println("\nSecuencial:  $t1")
    println("Concurrente: $t2")

    // 4.f: tiempos y aceleración para la tabla de las tres pruebas.
    println("Secuencial (ms): ${t1.inWholeMilliseconds}")
    println("Concurrente (ms): ${t2.inWholeMilliseconds}")
    println("Aceleración: %.2f".format(t1 / t2))
}