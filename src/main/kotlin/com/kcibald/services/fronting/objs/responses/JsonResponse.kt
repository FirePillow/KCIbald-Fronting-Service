package com.kcibald.services.fronting.objs.responses

import com.kcibald.services.fronting.utils.ContentTypes
import com.kcibald.services.fronting.utils.formatToString
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject

open class JsonResponse(
    response: JsonObject,
    private val statusCode: Int = 200,
    vararg headers: Pair<String, String> = emptyArray()
) : Response {

    private val processedHeaders: Map<String, String> = headers.toMap()

    private val payload = Buffer.buffer(response.formatToString())

    override fun apply(response: HttpServerResponse) {
        response.statusCode = statusCode
        response.putHeader("Content-Type", ContentTypes.JSON)
        response.headers().addAll(processedHeaders)
        response.end(payload)
    }
}