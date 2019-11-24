package com.kcibald.services.fronting.objs.responses

import io.vertx.kotlin.core.json.jsonObjectOf

object NotFoundResponseInJson : JsonResponse(
    jsonObjectOf("success" to false, "type" to "NOT_FOUND"),
    404
)