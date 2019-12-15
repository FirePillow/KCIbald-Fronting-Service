package com.kcibald.services.fronting.objs.responses.bouns

import com.kcibald.services.fronting.objs.responses.ResponseBonus
import io.vertx.core.http.Cookie
import io.vertx.core.http.HttpServerResponse

class CookieAddingResponseBonus(
    private vararg val cookies: Cookie
) : ResponseBonus {

    constructor(cookies: List<Cookie>) : this(*cookies.toTypedArray())

    override fun apply(response: HttpServerResponse) {
        for (cookie in cookies) {
            response.addCookie(cookie)
        }
    }
}