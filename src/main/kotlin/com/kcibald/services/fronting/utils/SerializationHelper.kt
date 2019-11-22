package com.kcibald.services.fronting.utils

import com.kcibald.objects.User
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf

fun User.json(): JsonObject = jsonObjectOf(
    "user_name" to this.userName,
    "url_key" to this.urlKey
)