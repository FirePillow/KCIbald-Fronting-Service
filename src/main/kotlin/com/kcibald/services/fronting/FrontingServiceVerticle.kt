package com.kcibald.services.fronting

import com.kcibald.services.fronting.controllers.common.CommonAPIRouter
import com.kcibald.services.fronting.utils.SharedObjects
import com.uchuhimo.konf.Config
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle

object FrontingServiceVerticle : CoroutineVerticle() {

    override suspend fun start() {
        val config = Config { addSpec(com.kcibald.services.fronting.controllers.Config) }
            .from.json.resource("config.json")

        val shared = initializeBasicObjects(config)

        val router = Router.router(vertx)
        CommonAPIRouter.routeAPIEndpoint(router, shared)
        CommonAPIRouter.routeHTMLContent(router, shared)

        val port = config[com.kcibald.services.fronting.controllers.Config.httpPort]
        vertx
            .createHttpServer()
            .requestHandler(router::handle)
            .listenAwait(port)

        super.start()
    }

    private fun initializeBasicObjects(config: Config): SharedObjects {
        SharedObjects.createDefault(
            config,
            vertx,
//            TODO: finish recaptcha Configuration and jwtAuth Configuration
            RecaptchaV3Client(""),
            JWTAuth.create(vertx, JWTAuthOptions())
        )
        TODO()
    }

    override suspend fun stop() {
        super.stop()
    }
}
