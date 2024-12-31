package me.lightless.burp.web

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.lightless.burp.web.controller.signRuleRoutes

fun startWebServer(wait: Boolean): NettyApplicationEngine {
    val httpServer = embeddedServer(Netty, port = 3336, host = "127.0.0.1", module = Application::defaultModule)
    httpServer.start(wait)
    return httpServer.engine
}

fun Application.defaultModule() {
    install(CallLogging)
    install(CORS) {
        allowCredentials = true
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)

        // for local develop
        allowHost("127.0.0.1:3000")
        allowHost("localhost:3000")
        allowHost("127.0.0.1:5173")
        allowHost("localhost:5173")

        // for product
        allowHost("127.0.0.1:3336")
        allowHost("localhost:3336")
    }
    install(ContentNegotiation) {
        jackson()
    }
    install(Compression)
    routing {
        get("/status") {
            call.respondText("ok")
        }

        route("/api/signRule") {
            signRuleRoutes()
        }

        staticResources("", "dist", index = "index.html") {
            preCompressed(CompressedFileType.BROTLI)
            default("index.html")
        }
    }
}
