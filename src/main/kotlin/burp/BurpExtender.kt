package burp

import io.ktor.server.netty.*
import me.lightless.burp.HTTPListener
import me.lightless.burp.Logger
import me.lightless.burp.VERSION
import me.lightless.burp.models.connectDatabase
import me.lightless.burp.utils.BurpUtils
import me.lightless.burp.web.startWebServer
import java.io.PrintWriter
import javax.script.ScriptEngineManager

@Suppress("unused")
class BurpExtender : IBurpExtender, IExtensionStateListener {
    private lateinit var callbacks: IBurpExtenderCallbacks
    private lateinit var httpListener: HTTPListener
    private lateinit var webServer: NettyApplicationEngine

    override fun registerExtenderCallbacks(callbacks: IBurpExtenderCallbacks?) {
        if (callbacks == null) {
            return
        }

        this.callbacks = callbacks
        BurpUtils.burpCallbacks = callbacks

        // 初始化 logger
        Logger.stdout = PrintWriter(callbacks.stdout, true)
        Logger.info("SignMe2 Version: $VERSION, Burp Version: ${callbacks.burpVersion.joinToString()}, Author: lightless <lightless@foxmail.com>")

        // 插件名
        callbacks.setExtensionName("SignMe2 - $VERSION")

        // FOR DEBUG
        val engines = ScriptEngineManager().engineFactories
        Logger.info("engines size: ${engines.size}")
        for (f in engines) {
            Logger.info(f.languageName + " " + f.engineName + " " + f.names.toString())
        }

        // Connect Database
        try {
            val dbFile = connectDatabase()
            Logger.info("connect to database: $dbFile")
        } catch (ex: Exception) {
            Logger.fatal("Connect to database failed, exit. Error: $ex\n${ex.stackTraceToString()}")
            return
        }

        // Start WebServer
        try {
            this.webServer = startWebServer(false)
            Logger.info("Start WEB at http://localhost:3336/")
        } catch (ex: Exception) {
            Logger.fatal("Start WEB server failed, exit. Error: $ex\n${ex.stackTraceToString()}")
            return
        }

        // 设置 HTTP Listener
        this.httpListener = HTTPListener(callbacks)
        callbacks.registerHttpListener(this.httpListener)
        callbacks.registerExtensionStateListener(this)

        Logger.debug("registerExtenderCallbacks finished.")
    }

    override fun extensionUnloaded() {
        Logger.debug("extension unloaded called.")
        this.callbacks.removeHttpListener(this.httpListener)
        Logger.debug("Stop web server.")
        this.webServer.stop(1000, 5000)
        Logger.debug("SignMe2 stop.")
    }
}