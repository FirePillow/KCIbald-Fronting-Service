package com.kcibald.services.fronting.objs.responses

import io.vertx.core.http.HttpServerResponse
import java.util.concurrent.TimeUnit

class SingleFileResponse(
    private val filePath: String,
    private val maxAgeInSeconds: Long = TimeUnit.DAYS.toSeconds(1)
) : Response {
    override fun apply(response: HttpServerResponse) {
        response.statusCode = 200
        val cacheControlValue = if (maxAgeInSeconds <= 0) {
            "no-cache, no-store, must-revalidate"
        } else {
            "public, max-age=$maxAgeInSeconds"
        }
        response.putHeader("Cache-Control", cacheControlValue)
        response.sendFile(filePath) {
            if (!it.succeeded())
                InternalErrorResponse.apply(response)
        }
    }
}