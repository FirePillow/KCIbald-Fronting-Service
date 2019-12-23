package com.kcibald.services.fronting

import com.kcibald.objects.*
import com.kcibald.services.PageableFetchConfig
import com.kcibald.services.Result
import com.kcibald.services.ServiceClient
import com.kcibald.services.fronting.utils.SharedObjects
import com.kcibald.services.kcibald.KCIBALDClient
import com.kcibald.services.kcibald.URLKey
import com.kcibald.services.user.AuthenticationClient
import com.kcibald.services.user.AuthenticationResult
import com.kcibald.utils.PageableCollection
import com.kcibald.utils.toURLKey
import com.thedeanda.lorem.LoremIpsum
import com.uchuhimo.konf.Config
import com.wusatosi.recaptcha.RecaptchaClient
import io.vertx.core.Handler
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import java.util.*

internal fun createInterceptedSharedObject(
    config: Config,
    recaptchaClient: RecaptchaClient?,
    jwtAuth: JWTAuth
): SharedObjects {
    return object : SharedObjects {
        override val config: Config
            get() = config
        override val recaptchaClient: RecaptchaClient?
            get() = recaptchaClient
        override val jwtAuth: JWTAuth
            get() = jwtAuth

        override fun checkServiceClientOverride(serviceName: String): ServiceClient? {
            return when (serviceName) {
                "auth" -> {
                    testOnlyPublicAuthenticationClient()
                }
                "kcibald" -> {
                    testOnlyKcibaldClient()
                }
                else -> null
            }
        }

        override fun checkHandlerIntercept(handlerName: String): Handler<RoutingContext>? {
            return when (handlerName) {
                "auth" -> {
                    Handler { it.next() }
                }
                else -> null
            }
        }
    }
}

private fun testOnlyKcibaldClient(): KCIBALDClient = object : KCIBALDClient {
    override val clientVersion: String
        get() = ""
    override val compatibleServiceVersion: String
        get() = ""

    override suspend fun createCommentUnderPost(
        regionUrlKey: URLKey,
        postUrlKey: URLKey,
        content: String,
        replyTo: String,
        attachments: List<Attachment>
    ): Result<Comment> = throw NotImplementedError("not implemented")

    override suspend fun createPost(
        regionUrlKey: URLKey,
        title: String,
        content: String,
        attachments: List<Attachment>
    ): Result<Post> = throw NotImplementedError("not implemented")

    override suspend fun deletePost(regionUrlKey: URLKey, postUrlKey: URLKey): Result<Post> =
        throw NotImplementedError("not implemented")

    override suspend fun describePost(
        regionUrlKey: URLKey,
        postUrlKey: URLKey,
        commentFetchConfig: PageableFetchConfig
    ): Result<Post> = Result.success(supplyPost(postUrlKey))


    override suspend fun describeRegion(
        regionUrlKey: URLKey,
        topPostFetchConfig: PageableFetchConfig
    ): Result<Region> = throw NotImplementedError("not implemented")

    override suspend fun listCommentsUnderPost(
        regionUrlKey: URLKey,
        postUrlKey: URLKey,
        fetchConfig: PageableFetchConfig
    ): Result<PageableCollection<Comment>> = throw NotImplementedError("not implemented")

    override suspend fun listPostsUnderRegion(
        regionUrlKey: URLKey,
        fetchConfig: PageableFetchConfig
    ): Result<PageableCollection<MinimizedPost>> = throw NotImplementedError("not implemented")

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

private val lorem = LoremIpsum.getInstance()

private fun supplyAuthor(): User {
    val name = lorem.firstName
    return User.createDefault(
        name,
        name.toURLKey(),
        "https://api.adorable.io/avatars/285/abott@adorable.png",
        lorem.getParagraphs(1, 1)
    )
}

private fun supplyContent(): String = lorem.getParagraphs(1, 5)

private fun supplyPost(postUrlKey: URLKey): Post = Post.createDefault(
    postUrlKey,
    supplyAuthor(),
    supplyContent(),
    "ok",
    attachments = supplyAttachments(),
    comments = supplyComments()
)

private val random = Random()

fun supplyComments(): List<Comment> = (1..(random.nextInt(10))).map(::supplyCommentSingle)

private fun supplyCommentSingle(index: Int): Comment {
    return Comment.createDefault(
        index,
        supplyAuthor(),
        supplyContent(),
        attachments = supplyAttachments()
    )
}

private fun supplyAttachments(): List<Attachment> = (1..(random.nextInt(3))).map { supplyAttachmentSingle() }

fun supplyAttachmentSingle() = Attachment.createDefault(
    lorem.url,
    lorem.getWords(1, 3)
)
