package com.kcibald.services.fronting.objs.responses.bouns

import com.kcibald.services.fronting.objs.responses.ResponseBonus
import io.vertx.core.http.HttpServerResponse

class HeaderAddingResponseBonus(
    vararg headers: Pair<String, String>
) : ResponseBonus {

    constructor(headers: List<Pair<String, String>>) : this(*headers.toTypedArray())

    private val headersMapped = headers.toMap()

    override fun apply(response: HttpServerResponse) {
        response.headers().addAll(headersMapped)
    }
}