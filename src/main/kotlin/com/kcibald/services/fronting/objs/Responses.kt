package com.kcibald.services.fronting.objs

import io.vertx.ext.web.RoutingContext

fun RoutingContext.fail(response: FailureResponse) = response.apply(this)

interface Response {
    fun apply(routingContext: RoutingContext)
}

interface StatusOnlyResponse: Response {
    fun statusCode(): Int

    override fun apply(routingContext: RoutingContext) {
        routingContext
            .response()
            .setStatusCode(statusCode())
            .end()
    }
}

interface FailureResponse : Response

object BadRequestResponse : FailureResponse {
    override fun apply(routingContext: RoutingContext) {
        routingContext
            .response()
            .setStatusCode(400)
            .end()
    }
}

object EmptyResponse: Response {
    override fun apply(routingContext: RoutingContext) {
        routingContext
            .response()
            .setStatusCode(204)
            .end()
    }
}

