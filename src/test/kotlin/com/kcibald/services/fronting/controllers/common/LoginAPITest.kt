package com.kcibald.services.fronting.controllers.common

import com.kcibald.objects.User
import com.kcibald.services.ServiceClient
import com.kcibald.services.fronting.controllers.MasterConfigSpec
import com.kcibald.services.fronting.utils.*
import com.kcibald.services.user.AuthenticationClient
import com.kcibald.services.user.AuthenticationResult
import com.uchuhimo.konf.Config
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.jwt.JWTOptions
import io.vertx.ext.web.Router
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.core.json.jsonObjectOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI
import java.net.http.HttpRequest

@ExtendWith(VertxExtension::class)
internal class LoginAPITest : HttpAPITestBase() {

    @BeforeEach
    fun setup(vertx: Vertx) = runBlocking {
        startUpServer(vertx)
        this@LoginAPITest.vertx = vertx
    }

    private lateinit var vertx: Vertx

    private val defaultConfig: Config = Config { addSpec(MasterConfigSpec.Authentication) }
        .from.json.resource("config.json")

    private fun initialize(
        authenticationClient: AuthenticationClient,
        config: Config = defaultConfig,
        jwtAuth: JWTAuth = object : TestJWTAuth() {},
        recaptchaV3Client: RecaptchaV3Client? = null
    ) {
        val router = Router.router(vertx)

        val sharedObjects = object : TestingSharedObject() {
            override val config: Config = config

            override val recaptchaClient: RecaptchaV3Client? = recaptchaV3Client

            override val jwtAuth: JWTAuth
                get() = jwtAuth

            override fun checkServiceClientOverride(serviceName: String): ServiceClient? {
                if (serviceName == "auth")
                    return authenticationClient
                return fail()
            }
        }

        Login.routeAPIEndpoint(router, sharedObjects)
        registryRouter(router)
    }

    private abstract class TestAuthenticationClient : AuthenticationClient {
        final override val clientVersion: String
            get() = "TESTING"
        final override val compatibleServiceVersion: String
            get() = "TESTING"
    }

    private val uri by lazy { URI.create("$domainWithoutSlash/login") }

    @Test
    fun normal() = runVertxCoroutinueContext(vertx) {
        val expectedEmail = "example@example.org"
        val expectedPassword = "egHR8vvfB34\$%u"

        val expectedToken = "tokenenenenne"
        val jwt = object : TestJWTAuth() {
            override fun generateToken(claims: JsonObject?, options: JWTOptions?): String = expectedToken
        }

        val expectedUser = randomUser()
        val client = object : TestAuthenticationClient() {
            override suspend fun verifyCredential(email: String, password: String): AuthenticationResult {
                assertEquals(expectedEmail, email)
                assertEquals(expectedPassword, password)
                return AuthenticationResult.Success(expectedUser)
            }
        }

        initialize(client, jwtAuth = jwt)

        val requestPayload = jsonObjectOf(
            "account" to expectedEmail,
            "password" to expectedPassword
        )

        val response = request(
            HttpRequest
                .newBuilder(uri)
                .POST(jsonBodyPublisher(requestPayload))
                .header("Content-Type", ContentTypes.JSON)
                .build()
        )

        val cookies = getCookiesFromResponse(response)
            .stream()
            .filter { it.name == defaultConfig[MasterConfigSpec.Authentication.CookieKey] }
            .findFirst()
            .orElseThrow(::AssertionError)

        assertEquals(expectedToken, cookies.value)
    }

    @Test
    fun malformed_no_account() = runVertxCoroutinueContext(vertx) {
        val authenticationClient = object : TestAuthenticationClient() {
            override suspend fun verifyCredential(email: String, password: String): AuthenticationResult {
                return fail()
            }
        }

        initialize(authenticationClient)

        val jsonPayload = jsonObjectOf(
            "password" to "egHR8vvfB34\$%u"
        )
        val response = request(
            HttpRequest
                .newBuilder(uri)
                .POST(jsonBodyPublisher(jsonPayload))
                .header("Content-Type", ContentTypes.JSON)
                .build()
        )

        assertBadRequestResponse(response)
    }

    @Test
    fun malformed_no_password() = runVertxCoroutinueContext(vertx) {
        val authenticationClient = object : TestAuthenticationClient() {
            override suspend fun verifyCredential(email: String, password: String): AuthenticationResult {
                return fail()
            }
        }

        initialize(authenticationClient)

        val jsonPayload = jsonObjectOf(
            "account" to "example@example.org"
        )

        val response = request(
            HttpRequest
                .newBuilder(uri)
                .POST(jsonBodyPublisher(jsonPayload))
                .build()
        )

        assertBadRequestResponse(response)
    }

    @Test
    fun not_found() = runVertxCoroutinueContext(vertx) {
        val client = object : TestAuthenticationClient() {
            override suspend fun verifyCredential(email: String, password: String): AuthenticationResult {
                return AuthenticationResult.UserNotFound
            }
        }
        initialize(client)

        val body = jsonObjectOf(
            "account" to "example@example.org",
            "password" to "egHR8vvfB34\$%u"
        )

        val response = request(
            HttpRequest
                .newBuilder(uri)
                .POST(jsonBodyPublisher(body))
                .header("Content-Type", ContentTypes.JSON)
                .build()
        )

        assertEquals(403, response.statusCode())
        assertEquals("PASSWORD_OR_USERNAME_INCORRECT", response.body().getString("type"))

        Unit
    }

    @Test
    @Disabled("not finished")
    fun user_baned() {
        TODO()
    }

    private fun randomUser(): User {
        val userName = "test-user-${System.nanoTime()}"
        return User.createDefault(
            userName,
            userName,
            "avatarfile",
            "test-signature-${System.nanoTime()}"
        )
    }

}