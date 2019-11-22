package com.kcibald.services.fronting

import com.kcibald.services.fronting.controllers.MasterConfigSpec
import com.kcibald.services.fronting.controllers.common.CommonAPIRouter
import com.kcibald.services.fronting.utils.SharedObjects
import com.uchuhimo.konf.Config
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle

object FrontingServiceVerticle : CoroutineVerticle() {

    override suspend fun start() {
        val config = Config { addSpec(MasterConfigSpec) }
            .from.json.resource("config.json")

        val shared = initializeBasicObjects(config)

        val router = Router.router(vertx)
        CommonAPIRouter.routeAPIEndpoint(router, shared)
        CommonAPIRouter.routeHTMLContent(router, shared)

        val port = config[MasterConfigSpec.httpPort]
        vertx
            .createHttpServer()
            .requestHandler(router::handle)
            .listenAwait(port)

        super.start()
    }

    private fun initializeBasicObjects(config: Config): SharedObjects {
        val secretKey = config[MasterConfigSpec.RecaptchaSiteKey]
        return SharedObjects.createDefault(
            config,
            vertx,
            RecaptchaV3Client(secretKey),
            jwtAuthFactory(config)
        )
    }

    private fun jwtAuthFactory(config: Config): JWTAuth {
        val json = JsonObject(config[MasterConfigSpec.Authentication.JwtAuthConfig])
        return JWTAuth.create(vertx, JWTAuthOptions(json))
    }

    override suspend fun stop() {
        super.stop()
    }
}
