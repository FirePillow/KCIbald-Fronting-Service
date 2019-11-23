package com.kcibald.services.fronting

import com.github.lalyos.jfiglet.FigletFont
import com.kcibald.services.fronting.controllers.MasterConfigSpec
import com.kcibald.services.fronting.controllers.common.CommonAPIRouter
import com.kcibald.services.fronting.controllers.user.UserAPIRouter
import com.kcibald.services.fronting.objs.entries.GroupingRouter
import com.kcibald.services.fronting.utils.SharedObjects
import com.kcibald.utils.d
import com.kcibald.utils.i
import com.kcibald.utils.w
import com.uchuhimo.konf.Config
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FrontingServiceVerticle : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(FrontingServiceVerticle::class.java)

    override suspend fun start() {
        printStartupBanner()

        logger.i { "starting FrontingServiceVerticle" }
        val config = loadConfigs()
        val shared = initializeBasicObjects(config)

        val router = Router.router(vertx)
        routeEndpoints(router, shared)

        registryVertxHttpServer(config, router::handle)

        logger.i { "FrontingServiceVerticle started" }
        super.start()
    }

    private fun printStartupBanner() {
        val banner = FigletFont.convertOneLine("KCIBALD")
        println(banner)
    }

    private suspend fun loadConfigs(): Config {
        val resourcePath = "config.json"
        logger.d { "loading config from resource path: $resourcePath" }
        return withContext(Dispatchers.IO) {
            Config { addSpec(MasterConfigSpec) }
                .from.json.resource(resourcePath)
        }
    }

    private fun routeEndpoints(router: Router, shared: SharedObjects) {
        logger.i { "Starting registering endpoints" }
        val groups = listOf(
            CommonAPIRouter,
            UserAPIRouter
        )

        routeAPIEndpoint(groups, router, shared)
        routeHTMLEndpoint(groups, router, shared)
        logger.i { "Endpoint registration completed" }
    }

    private fun routeAPIEndpoint(
        groupingRouters: List<GroupingRouter>,
        router: Router,
        shared: SharedObjects
    ) {
        logger.i { "Start registering API endpoints" }
        groupingRouters.forEach {
            it.routeAPIEndpoint(router, shared)
        }
        logger.i { "API endpoints registered" }
    }

    private fun routeHTMLEndpoint(
        groupingRouters: List<GroupingRouter>,
        router: Router,
        shared: SharedObjects
    ) {
        logger.i { "Start registering HTML endpoints" }
        groupingRouters.forEach {
            it.routeHTMLContent(router, shared)
        }
        logger.i { "HTML endpoints registered" }
    }

    private suspend fun registryVertxHttpServer(
        config: Config,
        routeHandle: (HttpServerRequest) -> Unit
    ) {
        val serverConfig = config[MasterConfigSpec.VertxHttpServerConfig]
        val serverConfigInJson = JsonObject(serverConfig)
        logger.d { "registering vertx http server with config ${serverConfigInJson.encode()}" }
        vertx
            .createHttpServer(HttpServerOptions(serverConfigInJson))
            .requestHandler(routeHandle)
            .listenAwait()
    }

    private fun initializeBasicObjects(config: Config): SharedObjects {
        logger.d { "initializing basic objects" }
        val secretKey = config[MasterConfigSpec.RecaptchaSiteKey]
        val recaptchaClient =
            if (secretKey.isEmpty()) {
                logger.w {
                    "Recaptcha secret key is empty, " +
                            "ASSUME THIS IS AN INSTRUCTION TO SKIP RECAPTCHA CHECKING, DO NOT DEPLOY THIS TO PRODUCTION"
                }
                null
            } else {
                logger.d { "Creating recaptcha-v3-client" }
                RecaptchaV3Client(secretKey)
            }
        val shared = SharedObjects.createDefault(
            config,
            vertx,
            recaptchaClient,
            jwtAuthFactory(config)
        )
        logger.d { "basic objects created" }
        return shared
    }

    private fun jwtAuthFactory(config: Config): JWTAuth {
        val json = JsonObject(config[MasterConfigSpec.Authentication.JwtAuthConfig])
        logger.d { "creating jwtAuth with config ${json.encode()}" }
        return JWTAuth.create(vertx, JWTAuthOptions(json))
    }

    override suspend fun stop() {
        logger.i { "FrontingServiceVerticle STOPPING" }
        super.stop()
    }
}
