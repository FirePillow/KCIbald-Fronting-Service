package com.kcibald.services.fronting.utils

import com.kcibald.utils.d
import io.github.tsegismont.vertx.contextual.logging.ContextualData
import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext

object RequestIDHandler : Handler<RoutingContext> {

    private val logger =
        LoggerFactory.getLogger(RequestIDHandler.javaClass)

    private data class ThingsToHash(val path: String, val method: HttpMethod, val cookies: String?, val time: Long) {
        constructor(request: HttpServerRequest, time: Long) : this(
            request.path(),
            request.method(),
            request.getHeader("Cookie") ?: request.getHeader("cookie"),
            time
        )
    }

    override fun handle(event: RoutingContext) {
        val time = System.currentTimeMillis()
        val request = event.request()
        val hashCode = ThingsToHash(request, time).hashCode()
        val requestId = "$time-$hashCode"
        ContextualData.put("request-id", requestId)
        logger.d {
            "request inbound, path: ${request.path()}, method: ${request.method()}, " +
                    "headers: ${request.headers().joinToString("; ")}"
        }
        event.response().putHeader("x-request-id", requestId)
        event.addBodyEndHandler {
            logger.d { "request finished completely, response code: ${event.response().statusCode}" }
        }
        event.next()
    }

}