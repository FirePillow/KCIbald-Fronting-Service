package com.kcibald.services.fronting.objs.responses

import io.vertx.core.http.HttpServerResponse

object TerminationResponse: TerminateResponse {
    override fun apply(response: HttpServerResponse) {
        response.end()
    }
}