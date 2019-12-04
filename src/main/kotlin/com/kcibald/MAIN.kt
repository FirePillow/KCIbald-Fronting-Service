package com.kcibald

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.kcibald.services.fronting.FrontingServiceVerticle
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

fun main() {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
    configGlobalLogLevel(Level.ALL)
    Vertx
        .vertx()
        .deployVerticle(FrontingServiceVerticle) {
            if (it.failed()) {
                System.err.println("failed to deploy versicle ${FrontingServiceVerticle}, exception: ${it.cause()}, exiting")
                it.cause().printStackTrace()
                exitProcess(-1)
            }
        }

}

private fun configGlobalLogLevel(level: Level) {
    val root =
        LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = level
}
