package com.kcibald.services.fronting.objs.responses

import io.vertx.kotlin.core.json.jsonObjectOf

object InternalErrorResponse : JsonResponse(
    jsonObjectOf(
        "success" to false,
        "type" to "INTERNAL_ERROR"
    ), 500
)