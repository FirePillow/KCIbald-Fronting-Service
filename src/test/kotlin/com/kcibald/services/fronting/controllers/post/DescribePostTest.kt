package com.kcibald.services.fronting.controllers.post

import com.kcibald.objects.Comment
import com.kcibald.objects.Post
import com.kcibald.objects.User
import com.kcibald.services.PageableFetchConfig
import com.kcibald.services.Result
import com.kcibald.services.ServiceClient
import com.kcibald.services.fronting.utils.*
import com.kcibald.services.kcibald.KCIBALDClient
import com.kcibald.services.kcibald.URLKey
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.junit5.VertxExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI
import java.net.http.HttpRequest

@ExtendWith(VertxExtension::class)
internal class DescribePostTest : HttpAPITestBase() {

    @BeforeEach
    fun setUp(vertx: Vertx) = runBlocking {
        startUpServer(vertx)
    }

    class KCIBaldSharedObjectHelper(private val kcibaldClient: KCIBALDClient) : NoAuthTestingSharedObject() {
        override fun checkServiceClientOverride(serviceName: String): ServiceClient? {
            return if (serviceName == "kcibald")
                kcibaldClient
            else
                fail()
        }
    }

    @Test
    fun normal(vertx: Vertx) = runVertxCoroutinueContext(vertx) {
        val expectedRegionKey = "region"
        val expectedPostKey = "title"

        val expectedPost = Post.createDefault(
            expectedPostKey,
            User.createDefault("name", "name", "file", "sign"),
            "content",
            expectedRegionKey,
            comments = listOf(
                Comment.createDefault(
                    1, User.createDefault(
                        "name2",
                        "name2",
                        "file2",
                        "sign2"
                    ), "content-comment"
                )
            )
        )

        val client = object : TestingKcibaldClient() {
            override suspend fun describePost(
                regionUrlKey: URLKey,
                postUrlKey: URLKey,
                commentFetchConfig: PageableFetchConfig
            ): Result<Post> {
                assertEquals(expectedRegionKey, regionUrlKey)
                assertEquals(expectedPostKey, postUrlKey)
                assertEquals(PageableFetchConfig(), commentFetchConfig)
                return Result.success(expectedPost)
            }
        }

        val router = Router.router(vertx)
        DescribePost.routeAPIEndpoint(router, KCIBaldSharedObjectHelper(client))
        registryRouter(router)

        val response = request(requestBuilder(expectedRegionKey, expectedPostKey))

        assertEquals(200, response.statusCode())
        assertEquals(expectedPost.json(), response.body())
    }

    @Test
    fun not_found(vertx: Vertx) = runVertxCoroutinueContext(vertx) {
        val client = object : TestingKcibaldClient() {
            override suspend fun describePost(
                regionUrlKey: URLKey,
                postUrlKey: URLKey,
                commentFetchConfig: PageableFetchConfig
            ): Result<Post> {
                return Result.notFound()
            }
        }

        val router = Router.router(vertx)
        DescribePost.routeAPIEndpoint(router, KCIBaldSharedObjectHelper(client))
        registryRouter(router)

        val response = request(requestBuilder("reg", "pos"))

        assertEquals(404, response.statusCode())
    }


    @Test
    fun failed(vertx: Vertx) = runVertxCoroutinueContext(vertx) {
        val client = object : TestingKcibaldClient() {
            override suspend fun describePost(
                regionUrlKey: URLKey,
                postUrlKey: URLKey,
                commentFetchConfig: PageableFetchConfig
            ): Result<Post> {
                return Result.failure("blah")
            }
        }

        val router = Router.router(vertx)
        DescribePost.routeAPIEndpoint(router, KCIBaldSharedObjectHelper(client))
        registryRouter(router)

        val response = request(requestBuilder("reg", "pos"))

        assertEquals(500, response.statusCode())
    }

    private fun requestBuilder(regionKey: String, postKey: String): HttpRequest = HttpRequest
        .newBuilder(URI.create("$domainWithoutSlash/r/$regionKey/p/$postKey/"))
        .GET()
        .header("accept", ContentTypes.JSON)
        .build()

}