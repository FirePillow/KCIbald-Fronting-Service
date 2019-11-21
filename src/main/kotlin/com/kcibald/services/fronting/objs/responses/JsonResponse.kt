package com.kcibald.services.fronting.objs.responses

import com.kcibald.services.fronting.end
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject

class JsonResponse(
    val response: JsonObject,
    val statusCode: Int = 200
): Response {
    override fun apply(response: HttpServerResponse) {
        response.statusCode = statusCode
        response.end(this.response)
    }
}