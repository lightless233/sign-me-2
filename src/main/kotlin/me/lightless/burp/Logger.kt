package me.lightless.burp

import java.io.PrintWriter
import java.time.LocalDateTime

/**
 * 日志类
 */
object Logger {

    lateinit var stdout: PrintWriter

    private fun log(level: String, msg:String) {
        stdout.println("[${level.uppercase()}][${LocalDateTime.now()}] $msg")
    }

    fun debug(msg: String) {
        this.log("DEBUG", msg)
    }

    fun info(msg: String) {
        this.log("INFO", msg)
    }

    fun warn(msg: String) {
        this.log("WARN", msg)
    }

    fun error(msg: String) {
        this.log("ERROR", msg)
    }

    fun fatal(msg: String) {
        this.log("FATAL", msg)
    }
}