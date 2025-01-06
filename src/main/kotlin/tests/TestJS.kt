package tests

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess

fun main() {

    Thread.currentThread().contextClassLoader = Context::class.java.classLoader

    val engineContext = Context.newBuilder("js")
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLookup { true }
        .hostClassLoader(Context::class.java.classLoader)
        .option("js.ecmascript-version", "latest")
        .build()

    print(engineContext.eval("js", "1 + 1"))
}