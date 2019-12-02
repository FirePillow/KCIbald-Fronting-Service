package com.kcibald.services.fronting.utils

import com.kcibald.services.ServiceClient
import com.uchuhimo.konf.Config
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.core.Vertx
import io.vertx.ext.auth.jwt.JWTAuth
import org.junit.jupiter.api.Assertions

internal abstract class TestingSharedObject(
    override val vertx: Vertx
): SharedObjects {
    override val config: Config
        get() = Assertions.fail()
    override val recaptchaClient: RecaptchaV3Client?
        get() = Assertions.fail()
    override val jwtAuth: JWTAuth
        get() = Assertions.fail()

    override fun checkServiceClientOverride(serviceName: String): ServiceClient? = Assertions.fail()
}