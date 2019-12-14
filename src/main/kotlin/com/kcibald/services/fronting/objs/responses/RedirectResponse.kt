package com.kcibald.services.fronting.objs.responses

import io.vertx.core.http.HttpServerResponse

class RedirectResponse private constructor(
    private val url: String,
    private val status: Int
) : TerminateResponse {

    constructor(url: String, temporaryRedirect: Boolean = true) : this(url, if (temporaryRedirect) 307 else 301)

    override fun apply(response: HttpServerResponse) {
        response
            .setStatusCode(status)
            .putHeader("Location", url)
            .end()
    }
}