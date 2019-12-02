package com.kcibald.services.fronting.utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.listenAwait
import kotlinx.coroutines.future.asDeferred
import org.junit.jupiter.api.Assertions.*
import org.slf4j.LoggerFactory
import java.net.HttpCookie
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.properties.Delegates

internal abstract class HttpAPITestBase {

    init {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
        configGlobalLogLevel(Level.ALL)
    }

    private fun configGlobalLogLevel(level: Level) {
        val root =
            LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.level = level
    }


    private var server: HttpServer? = null
    private var port by Delegates.notNull<Int>()
    private var currentHandler: Handler<HttpServerRequest> = Handler {}

    protected suspend fun startUpServer(
        vertx: Vertx,
        httpClientAppender: (HttpClient.Builder) -> HttpClient.Builder = Function.identity<HttpClient.Builder>()::apply
    ) {
        val server =
            vertx
                .createHttpServer()
                .requestHandler { request ->
                    this.currentHandler.handle(request)
                }
                .listenAwait(0)
        port = server.actualPort()
        this.server = server

        httpClient = HttpClient.newBuilder().let(httpClientAppender).build()
    }

    protected fun registryRouter(router: Router) {
        if (server != null)
            currentHandler = router
        else
            fail("call startUp first")
    }

    private lateinit var httpClient: HttpClient

    protected val domainWithoutSlash
        get() = "http://localhost:$port"

    protected suspend fun request(request: HttpRequest): HttpResponse<JsonObject> =
        httpClient
            .sendAsync(request, JsonBodyHandler)
            .asDeferred()
            .await()

    protected fun getCookiesFromResponse(response: HttpResponse<*>): List<HttpCookie> =
        response
            .headers()
            .allValues("Set-Cookie")
            .stream()
            .flatMap { HttpCookie.parse(it).stream() }
            .collect(Collectors.toUnmodifiableList())

    protected fun assertBadRequestResponse(response: HttpResponse<JsonObject>) {
        assertEquals(400, response.statusCode())
        assertNull(response.body())
    }
}