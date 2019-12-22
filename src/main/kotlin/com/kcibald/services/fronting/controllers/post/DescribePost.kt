package com.kcibald.services.fronting.controllers.post

import com.kcibald.objects.Post
import com.kcibald.services.PageableFetchConfig
import com.kcibald.services.Result
import com.kcibald.services.fronting.objs.entries.APIEntry
import com.kcibald.services.fronting.objs.responses.*
import com.kcibald.services.fronting.objs.responses.bouns.ResponseTimeHeaderBonus
import com.kcibald.services.fronting.utils.*
import com.kcibald.services.kcibald.KCIBALDClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

object DescribePost : APIEntry {
    private val logger = LoggerFactory.getLogger(DescribePost.javaClass)

    override fun routeAPIEndpoint(router: Router, sharedObjects: SharedObjects) {
        val client = sharedObjects.checkServiceClientOverride("kcibald")!! as KCIBALDClient

        router
            .get("/r/:regionKey/p/:postKey/")
            .produces(ContentTypes.JSON)
            .authenticated(StandardAuthenticationRejectResponse.API, sharedObjects)
            .coroutineCoreHandler((Handling(client))::handleEvent)
    }

    private class Handling(
        private val client: KCIBALDClient
    ) {

        suspend fun handleEvent(routingContext: RoutingContext): TerminateResponse {
            val (regionKey, postKey) = getRegionAndPostKey(routingContext)
            val fetchConfig = getFetchConfig(routingContext)
            logger.debug("accepting request, regionKey=$regionKey postKey=$postKey fetchConfig=$fetchConfig")
            val (r, time) = withProcessTimeRecording { client.describePost(regionKey, postKey, fetchConfig) }
            logger.debug("retrieved post result=$r, time used=$time")
            val bonus = ResponseTimeHeaderBonus.fromNameAndTime("kcibald-client", time)
            val response = bonus + when (val postResult = r.getOrThrow()) {
                is Result.Success -> successResponse(postResult.result)
                is Result.NotFound -> notFoundResponse()
                is Result.Failure -> failureResponse(postResult.message)
            }
            logger.trace("response with: {}", response)
            return response
        }

        private fun notFoundResponse(): TerminateResponse {
            logger.debug("requested post not found")
            return NotFoundResponseInJson
        }

        private fun successResponse(post: Post): TerminateResponse {
            logger.debug("request success, post={}", post)
            return JsonResponse(post.json())
        }

        private fun failureResponse(message: String): TerminateResponse {
            logger.info("request failed from client, message={}", message)
            return InternalErrorResponse
        }

        private fun getRegionAndPostKey(context: RoutingContext): Pair<String, String> {
            val request = context.request()
            val regionKey = request.getParam("regionKey") ?: incompleteRequest()
            val postKey = request.getParam("postKey") ?: incompleteRequest()
            return regionKey to postKey
        }

        private fun getFetchConfig(ignored: RoutingContext): PageableFetchConfig {
            return PageableFetchConfig()
        }

    }
}