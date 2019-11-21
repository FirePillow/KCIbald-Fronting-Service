package com.kcibald.services.fronting.objs.responses

import com.kcibald.services.fronting.utils.formatToString
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerResponse
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

object InternalErrorResponse : Response {

    private val body = Buffer.buffer(json {
        obj(
            "success" to false,
            "type" to "INTERNAL_ERROR"
        )
    }.formatToString())

    override fun apply(response: HttpServerResponse) {
        response
            .setStatusCode(500)
            .end(body)
    }

}