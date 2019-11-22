package com.kcibald.services.fronting

import com.kcibald.services.fronting.controllers.MasterConfigSpec
import com.kcibald.services.fronting.controllers.common.CommonAPIRouter
import com.kcibald.services.fronting.utils.SharedObjects
import com.uchuhimo.konf.Config
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle

object FrontingServiceVerticle : CoroutineVerticle() {

    override suspend fun start() {
        val config = loadConfigs()

        val shared = initializeBasicObjects(config)

        val router = Router.router(vertx)
        routeEndpoints(router, shared)

        registryVertxHttpServer(config, router::handle)

        super.start()
    }

    private fun loadConfigs() = Config { addSpec(MasterConfigSpec) }
        .from.json.resource("config.json")

    private fun routeEndpoints(router: Router, shared: SharedObjects) {
        CommonAPIRouter.routeAPIEndpoint(router, shared)
        CommonAPIRouter.routeHTMLContent(router, shared)
    }

    private suspend fun registryVertxHttpServer(
        config: Config,
        routeHandle: (HttpServerRequest) -> Unit
    ) {
        val serverConfig = config[MasterConfigSpec.VertxHttpServerConfig]
        val serverConfigInJson = JsonObject(serverConfig)
        vertx
            .createHttpServer(HttpServerOptions(serverConfigInJson))
            .requestHandler(routeHandle)
            .listenAwait()
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
