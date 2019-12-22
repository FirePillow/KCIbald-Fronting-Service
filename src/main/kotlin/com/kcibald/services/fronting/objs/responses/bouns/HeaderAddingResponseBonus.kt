package com.kcibald.services.fronting.objs.responses.bouns

import com.kcibald.services.fronting.objs.responses.ResponseBonus
import io.vertx.core.http.HttpServerResponse

data class HeaderAddingResponseBonus(private val headersMapped: Map<String, String>) : ResponseBonus {

    constructor(vararg header: Pair<String, String>) : this(header.toMap())
    constructor(headers: List<Pair<String, String>>) : this(*headers.toTypedArray())

    override fun apply(response: HttpServerResponse) {
        response.headers().addAll(headersMapped)
    }
}