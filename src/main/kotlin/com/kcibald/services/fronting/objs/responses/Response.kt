package com.kcibald.services.fronting.objs.responses

import io.vertx.core.http.HttpServerResponse

interface Response {
    fun apply(response: HttpServerResponse)
}
