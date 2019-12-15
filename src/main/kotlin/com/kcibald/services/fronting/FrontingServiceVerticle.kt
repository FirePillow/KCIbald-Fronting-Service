package com.kcibald.services.fronting

import com.github.lalyos.jfiglet.FigletFont
import com.kcibald.objects.User
import com.kcibald.services.ServiceClient
import com.kcibald.services.fronting.controllers.MasterConfigSpec
import com.kcibald.services.fronting.controllers.common.CommonAPIRouter
import com.kcibald.services.fronting.controllers.misc.MiscRouter
import com.kcibald.services.fronting.controllers.user.UserAPIRouter
import com.kcibald.services.fronting.objs.entries.GroupingRouter
import com.kcibald.services.fronting.utils.RequestIDHandler
import com.kcibald.services.fronting.utils.SharedObjects
import com.kcibald.services.fronting.utils.VertxHelper
import com.kcibald.services.user.AuthenticationClient
import com.kcibald.services.user.AuthenticationResult
import com.kcibald.utils.d
import com.kcibald.utils.i
import com.kcibald.utils.w
import com.uchuhimo.konf.Config
import com.wusatosi.recaptcha.RecaptchaClient
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
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
        val shared = initializeBasicObject(config)

        val router = Router.router(vertx)
        initializeRouter(router)
        routeEndpoints(router, shared)

        registryVertxHttpServers(config, router)

        logger.i { "FrontingServiceVerticle started" }
        super.start()
    }

    private fun initializeRouter(router: Router) {
        router.route().handler(RequestIDHandler)
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
                .from.systemProperties()
        }
    }

    private fun routeEndpoints(router: Router, shared: SharedObjects) {
        logger.i { "Starting registering endpoints" }
        val groups = generateGroups()

        val apiRouter = Router.router(vertx)

        val corsHandler = CorsHandler.create(shared.config[MasterConfigSpec.CorsDomainPattern])
        corsHandler.allowCredentials(true)
        corsHandler.allowedMethods(setOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE))
        corsHandler.allowedHeaders(setOf("Content-Length", "Content-Type", "Accept", "X-GOTO-WORK", "X-STUDY-HARD"))
        corsHandler.maxAgeSeconds(300)
        apiRouter.route().handler(corsHandler)

        routeAPIEndpoint(groups, apiRouter, shared)
        router.mountSubRouter("/v1/", apiRouter)

        routeHTMLEndpoint(groups, router, shared)
        logger.i { "Endpoint registration completed" }
    }

    private fun generateGroups(): List<GroupingRouter> = listOf(
        CommonAPIRouter,
        UserAPIRouter,
        MiscRouter
    )

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

    private suspend fun registryVertxHttpServers(
        config: Config,
        router: Router
    ) {
        val serverConfig = config[MasterConfigSpec.VertxHttpServerConfig]
        val serverConfigInJson = JsonObject(serverConfig)
        logger.d { "registering vertx http server with config ${serverConfigInJson.encode()}" }
        val server = vertx
            .createHttpServer(HttpServerOptions(serverConfigInJson))
            .requestHandler(router)
            .listenAwait()
        logger.i { "listening at port: ${server.actualPort()}" }
    }

    private fun initializeBasicObject(config: Config): SharedObjects {
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
                logger.d { "Creating recaptcha-client" }
                RecaptchaClient.createUniversal(secretKey, config[MasterConfigSpec.Authentication.RecaptchaThreshold])
            }
        val shared = createInterceptedSharedObject(config, recaptchaClient)
        vertx.orCreateContext.put(VertxHelper.sharedObjVertxContextKey, shared)
        logger.d { "basic objects created" }
        return shared
    }

    private fun createInterceptedSharedObject(
        config: Config,
        recaptchaClient: RecaptchaClient?
    ): SharedObjects {
        return object : SharedObjects {
            override val config: Config
                get() = config
            override val recaptchaClient: RecaptchaClient?
                get() = recaptchaClient
            override val jwtAuth: JWTAuth
                get() = jwtAuthFactory(config)

            override fun checkServiceClientOverride(serviceName: String): ServiceClient? {
                if (serviceName == "auth") {
                    return testOnlyPublicAuthenticationClient()
                }
                return null
            }
        }
    }

    private fun testOnlyPublicAuthenticationClient() = object : AuthenticationClient {
        override val clientVersion: String
            get() = ""
        override val compatibleServiceVersion: String
            get() = ""

        override suspend fun verifyCredential(email: String, password: String): AuthenticationResult {
            if (email == "sb@kcibald.com" && password == "Mikesb123!!") {
                return AuthenticationResult.Success(
                    User.createDefault(
                        "mike",
                        "mike",
                        "avatars.kcibald.net",
                        "mike good good"
                    )
                )
            }

            if (email == "crash@kcibald.com") {
                throw Exception("simulating badbad result")
            }

            return AuthenticationResult.UserNotFound
        }
    }

    private fun jwtAuthFactory(config: Config): JWTAuth {
        val json = JsonObject(config[MasterConfigSpec.Authentication.JwtAuthConfig])
        logger.d { "creating jwtAuth" }
        val jwt = JWTAuth.create(vertx, JWTAuthOptions(json))
        logger.d { "created jwtAuth with config ${json.encode()}" }
        return jwt
    }

    override suspend fun stop() {
        logger.i { "FrontingServiceVerticle STOPPING" }
        super.stop()
    }
}
