package com.kcibald.services.fronting.objs.responses

import io.vertx.core.http.HttpServerResponse

data class CombinedResponse(
    private val first: Response,
    private val second: Response
) : Response {
    override fun apply(response: HttpServerResponse) {
        first.apply(response)
        if (!response.ended())
            second.apply(response)
    }
}

operator fun Response.plus(other: Response) = CombinedResponse(this, other)
operator fun Response.plus(other: TerminateResponse) = CombinedTerminateResponse(this, other)

data class CombinedTerminateResponse(
    private val first: Response,
    private val second: TerminateResponse
) : TerminateResponse {
    override fun apply(response: HttpServerResponse) {
        first.apply(response)
        second.apply(response)
    }
}

@Suppress("UNUSED_PARAMETER")
operator fun TerminateResponse.plus(ignored: Response): Nothing = throw IllegalArgumentException("wrong order")