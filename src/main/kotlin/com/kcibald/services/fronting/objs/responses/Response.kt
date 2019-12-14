package com.kcibald.services.fronting.objs.responses

import io.vertx.core.http.HttpServerResponse

interface Response {
    fun apply(response: HttpServerResponse)
}

/**
 * This response is guaranteed to terminate a response (HttpServerResponse.end) if not chunked
 */
interface TerminateResponse: Response

interface ResponseBonus: Response