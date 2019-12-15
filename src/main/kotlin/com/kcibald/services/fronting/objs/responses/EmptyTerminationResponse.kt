package com.kcibald.services.fronting.objs.responses

import io.vertx.core.http.HttpServerResponse

object EmptyTerminationResponse : TerminateResponse {
    private val delegate =
        statusOnlyTerminationResponse(204)
    override fun apply(response: HttpServerResponse) = delegate.apply(response)
}