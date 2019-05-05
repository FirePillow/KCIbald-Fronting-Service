package com.kcibald.services.fronting.objs

import com.kcibald.services.fronting.formatToString
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

interface Response {
    fun apply(response: HttpServerResponse)
}

sealed class StatusOnlyResponse(private val statusCode: Int) : Response {
    override fun apply(response: HttpServerResponse) {
        response
            .setStatusCode(statusCode)
            .end()
    }
}

object BadRequestResponse : StatusOnlyResponse(400)

object EmptyResponse : StatusOnlyResponse(204)

@Suppress("NOTHING_TO_INLINE")
//for possible interpretation and better chaining
inline fun RoutingContext.responseWith(_responses: Response) = _responses.apply(this.response())

object InternalErrorResponse : Response {

    private val body = json {
        obj(
            "success" to false,
            "type" to "INTERNAL_ERROR"
        )
    }.formatToString()

    override fun apply(response: HttpServerResponse) {
        response
            .setStatusCode(500)
            .end(body)
    }

}
