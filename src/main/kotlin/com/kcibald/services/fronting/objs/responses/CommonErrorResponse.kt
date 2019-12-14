package com.kcibald.services.fronting.objs.responses

import com.kcibald.services.fronting.objs.responses.bouns.StatusResponseBonus
import io.vertx.core.http.HttpServerResponse
import io.vertx.kotlin.core.json.jsonObjectOf

object BadRequestResponse : TerminateResponse {
    private val delegate = statusOnlyTerminationResponse(400)
    override fun apply(response: HttpServerResponse) = delegate.apply(response)
}

object InternalErrorResponse : TerminateResponse {
    private val delegate = StatusResponseBonus(500) + JsonResponse(
        jsonObjectOf(
            "success" to false,
            "type" to "INTERNAL_ERROR"
        )
    )
    override fun apply(response: HttpServerResponse) = delegate.apply(response)
}

object NotFoundResponseInJson : TerminateResponse {
    private val delegate =
        StatusResponseBonus(404) + JsonResponse(jsonObjectOf("success" to false, "type" to "NOT_FOUND"))
    override fun apply(response: HttpServerResponse) = delegate.apply(response)
}