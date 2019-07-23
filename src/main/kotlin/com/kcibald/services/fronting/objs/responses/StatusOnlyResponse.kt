package com.kcibald.services.fronting.objs.responses

import io.vertx.core.http.HttpServerResponse

abstract class StatusOnlyResponse(private val statusCode: Int) : Response {
    final override fun apply(response: HttpServerResponse) {
        response
            .setStatusCode(statusCode)
            .end()
    }
}
