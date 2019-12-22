package com.kcibald.services.fronting.objs.responses.bouns

import com.kcibald.services.fronting.objs.responses.ResponseBonus
import io.vertx.core.http.HttpServerResponse

data class StatusResponseBonus(private val statusCode: Int): ResponseBonus {
    override fun apply(response: HttpServerResponse) {
        response.statusCode = statusCode
    }
}