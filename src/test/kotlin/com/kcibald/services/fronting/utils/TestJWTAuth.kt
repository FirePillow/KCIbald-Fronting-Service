package com.kcibald.services.fronting.utils

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.jwt.JWTOptions
import org.junit.jupiter.api.Assertions.fail

abstract class TestJWTAuth : JWTAuth {
    override fun generateToken(claims: JsonObject?, options: JWTOptions?): String = fail()
    override fun generateToken(claims: JsonObject?): String = generateToken(claims, null)
    override fun authenticate(authInfo: JsonObject?, resultHandler: Handler<AsyncResult<User>>) {
        resultHandler.handle(Future.failedFuture(AssertionError()))
        fail<Unit>()
    }
}