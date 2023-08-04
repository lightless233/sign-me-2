import me.lightless.burp.Logger
import me.lightless.burp.models.connectDatabase
import me.lightless.burp.web.startWebServer
import java.io.PrintWriter
import javax.script.ScriptEngineManager


@Suppress("UNUSED_PARAMETER")
fun main(args: Array<String>) {
    Logger.stdout = PrintWriter(System.out, true)

    val engines = ScriptEngineManager().engineFactories
    for (f in engines) {
        Logger.debug(f.languageName + " " + f.engineName + " " + f.names.toString())
    }

    // Connect to database
    try {
        val dbFile = connectDatabase()
        Logger.info("Connect database: $dbFile")
    } catch (ex: Exception) {
        Logger.fatal("Connect to database failed. Error: $ex\n${ex.stackTraceToString()}")
        return
    }

    // Start HTTP Server
    try {
        startWebServer(true)
    } catch (ex: Exception) {
        Logger.fatal("Start WebServer failed. Error: $ex\n${ex.stackTraceToString()}")
        return
    }
}