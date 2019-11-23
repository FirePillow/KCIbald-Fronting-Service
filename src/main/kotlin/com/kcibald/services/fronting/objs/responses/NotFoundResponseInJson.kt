package com.kcibald.services.fronting.objs.responses

import com.kcibald.services.fronting.utils.end
import io.vertx.core.http.HttpServerResponse
import io.vertx.kotlin.core.json.jsonObjectOf

object NotFoundResponseInJson: Response {
    private val jsonPayload = jsonObjectOf("success" to false, "type" to "NOT_FOUND")

    override fun apply(response: HttpServerResponse) {
        response.statusCode = 404
        response.end(jsonPayload)
    }
}