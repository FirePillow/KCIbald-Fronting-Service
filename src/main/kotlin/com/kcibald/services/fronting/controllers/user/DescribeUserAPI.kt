package com.kcibald.services.fronting.controllers.user

import com.kcibald.objects.User
import com.kcibald.services.Result
import com.kcibald.services.fronting.objs.entries.APIEntry
import com.kcibald.services.fronting.objs.responses.InternalErrorResponse
import com.kcibald.services.fronting.objs.responses.JsonResponse
import com.kcibald.services.fronting.objs.responses.NotFoundResponseInJson
import com.kcibald.services.fronting.objs.responses.TerminateResponse
import com.kcibald.services.fronting.utils.*
import com.kcibald.services.user.DescribeUserClient
import com.kcibald.utils.d
import com.kcibald.utils.i
import com.kcibald.utils.w
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

object DescribeUserAPI : APIEntry {

    private lateinit var describeUserClient: DescribeUserClient
    private val logger = LoggerFactory.getLogger(DescribeUserAPI.javaClass)

    override fun routeAPIEndpoint(router: Router, sharedObjects: SharedObjects) {
        logger.d { "Mounting describe user API to router $router, path /u/:userKey/ and /me/" }
        this.describeUserClient = DescribeUserClient.createDefault(VertxHelper.currentVertx())
        routeDefaultEndPoint(router, sharedObjects)
        rerouteMeEndpoint(router, sharedObjects)
        logger.i { "Describe User API (/u/:userKey/ and /me/) registried" }
    }

    private fun routeDefaultEndPoint(
        router: Router,
        sharedObjects: SharedObjects
    ) {
        router
            .get("/u/:userKey/")
            .authenticated(StandardAuthenticationRejectResponse.API, sharedObjects.config, sharedObjects.jwtAuth)
            .produces(ContentTypes.JSON)
            .coroutineCoreHandler(::handleEvent)
        logger.i { "Registries /u/:userKey/ to router $router" }
    }

    private fun rerouteMeEndpoint(
        router: Router,
        sharedObjects: SharedObjects
    ) {
        router
            .get("/me/")
            .authenticated(StandardAuthenticationRejectResponse.API, sharedObjects.config, sharedObjects.jwtAuth)
            .produces(ContentTypes.JSON)
            .handler {
                val targetPath = "/u/${it.userUrlKey}"
                logger.d { "reroute /me/ to $targetPath" }
                it.reroute(targetPath)
            }
        logger.i { "Registries /me/ endpoint (reroute to /u/:userKey/) to router $router" }
    }

    private suspend fun handleEvent(context: RoutingContext): TerminateResponse {
        val urlKey = context.request().getParam("userKey")!!
        logger.d { "query user with urlKey: $urlKey" }
        val result = describeUserClient.describeUser(urlKey)
        logger.d { "result received for querying user: $result" }
        return when (result) {
            is Result.NotFound -> notFoundResponse(urlKey)
            is Result.Success -> successResponse(result.result)
            is Result.Failure -> failureResponse(urlKey, result)
        }
    }

    private fun notFoundResponse(urlKey: String): NotFoundResponseInJson {
        logger.d { "not found for urlKey $urlKey" }
        return NotFoundResponseInJson
    }

    private fun successResponse(result: User): TerminateResponse {
        logger.d { "successfully found user ($result) as requested" }
        val payload = result.json()
        return JsonResponse(payload)
    }

    private fun failureResponse(urlKey: String, result: Result.Failure<User>): TerminateResponse {
        logger.w { "failed to query request user with urlKey $urlKey, result: $result" }
        return InternalErrorResponse
    }

}