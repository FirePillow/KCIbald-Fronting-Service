package com.kcibald.services.fronting.utils

import com.kcibald.services.ServiceClient
import com.uchuhimo.konf.Config
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.core.Handler
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import org.junit.jupiter.api.Assertions.fail

internal abstract class TestingSharedObject : SharedObjects {
    override val config: Config
        get() = fail()
    override val recaptchaClient: RecaptchaV3Client?
        get() = fail()
    override val jwtAuth: JWTAuth
        get() = fail()

    override fun checkServiceClientOverride(serviceName: String): ServiceClient? = fail()
    override fun checkHandlerIntercept(handlerName: String): Handler<RoutingContext>? = fail()
}

internal abstract class NoAuthTestingSharedObject : TestingSharedObject() {
    override fun checkHandlerIntercept(handlerName: String): Handler<RoutingContext>? {
        if (handlerName == "auth") {
            return Handler { it.next() }
        }
        return fail()
    }
}