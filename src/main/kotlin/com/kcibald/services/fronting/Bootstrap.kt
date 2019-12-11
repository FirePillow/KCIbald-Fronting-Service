package com.kcibald.services.fronting

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

object Bootstrap {
    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty(
            "vertx.logger-delegate-factory-class-name",
            "io.vertx.core.logging.SLF4JLogDelegateFactory"
        )

        configGlobalLogLevel(Level.ALL)

        Vertx
            .vertx()
            .deployVerticle(
                FrontingServiceVerticle
            ) { it: AsyncResult<String?> ->
                if (it.failed()) {
                    System.err.println(
                        "failed to deploy versicle $FrontingServiceVerticle, exception: ${it.cause()}, exiting"
                    )

                    it.cause().printStackTrace()
                    System.exit(-1)
                }
            }
    }

    private fun configGlobalLogLevel(level: Level) {
        val root =
            LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.level = level
    }
}