package com.kcibald.services.fronting.objs.responses

import com.kcibald.services.fronting.objs.responses.bouns.StatusResponseBonus
import io.vertx.core.http.HttpServerResponse

abstract class StatusOnlyResponse(private val statusCode: Int) : TerminateResponse {
    final override fun apply(response: HttpServerResponse) {
        response
            .setStatusCode(statusCode)
            .end()
    }
}

fun statusOnlyTerminationResponse(statusCode: Int) = StatusResponseBonus(statusCode) + TerminationResponse