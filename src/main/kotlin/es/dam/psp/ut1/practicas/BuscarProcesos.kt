package es.dam.psp.ut1.practicas

fun main(args: Array<String>) {
    val filtro = args.firstOrNull()

    if (filtro.isNullOrBlank()) {
        println("Debes indicar una cadena de búsqueda. Ejemplo: java")
        return
    }

    println(
        "%-8s %-8s %-25s %-30s %-20s %s".format(
            "PID", "PPID", "USUARIO", "INICIO", "CPU TOTAL", "COMANDO"
        )
    )

    val procesos = ProcessHandle.allProcesses()

    try {
        procesos.forEach { p ->
            val info = p.info()
            val comando = info.command().orElse("")

            if (comando.contains(filtro, ignoreCase = true)) {
                val ppid = p.parent()
                    .map { it.pid().toString() }
                    .orElse("-")

                val usuario = info.user().orElse("?")

                val inicio = info.startInstant()
                    .map { it.toString() }
                    .orElse("?")

                val cpu = info.totalCpuDuration()
                    .map { it.toString() }
                    .orElse("?")

                println(
                    "%-8d %-8s %-25s %-30s %-20s %s".format(
                        p.pid(),
                        ppid,
                        usuario,
                        inicio,
                        cpu,
                        comando
                    )
                )
            }
        }
    } finally {
        procesos.close()
    }
}