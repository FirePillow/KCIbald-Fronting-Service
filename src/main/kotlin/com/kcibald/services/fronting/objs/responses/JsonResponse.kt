package com.kcibald.services.fronting.objs.responses

import com.kcibald.services.fronting.utils.ContentTypes
import com.kcibald.services.fronting.utils.formatToString
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject

data class JsonResponse(
    private val response: JsonObject
) : TerminateResponse {
    override fun apply(response: HttpServerResponse) {
        response.putHeader("Content-Type", ContentTypes.JSON)
        response.end(Buffer.buffer(this.response.formatToString()))
    }
}