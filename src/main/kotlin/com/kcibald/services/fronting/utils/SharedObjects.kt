package com.kcibald.services.fronting.utils

import com.uchuhimo.konf.Config
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.core.Vertx
import io.vertx.ext.auth.jwt.JWTAuth

interface SharedObjects {
    val config: Config
    val vertx: Vertx
    val recaptchaClient: RecaptchaV3Client?
    val jwtAuth: JWTAuth

    companion object {
        fun createDefault(
            config: Config,
            vertx: Vertx,
            // null if disabled
            recaptchaClient: RecaptchaV3Client?,
            jwtAuth: JWTAuth
        ): SharedObjects {
            data class SharedObjectsImpl(
                override val config: Config,
                override val vertx: Vertx,
                override val recaptchaClient: RecaptchaV3Client?,
                override val jwtAuth: JWTAuth
            ) : SharedObjects

            return SharedObjectsImpl(
                config, vertx, recaptchaClient, jwtAuth
            )
        }
    }
}