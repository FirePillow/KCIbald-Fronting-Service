package com.kcibald.services.fronting.controllers.post

import com.kcibald.objects.*
import com.kcibald.services.PageableFetchConfig
import com.kcibald.services.Result
import com.kcibald.services.kcibald.KCIBALDClient
import com.kcibald.services.kcibald.URLKey
import com.kcibald.utils.PageableCollection
import org.junit.jupiter.api.Assertions

abstract class TestingKcibaldClient : KCIBALDClient {
    override suspend fun createCommentUnderPost(
        regionUrlKey: URLKey,
        postUrlKey: URLKey,
        content: String,
        replyTo: String,
        attachments: List<Attachment>
    ): Result<Comment> = Assertions.fail()

    override suspend fun createPost(
        regionUrlKey: URLKey,
        title: String,
        content: String,
        attachments: List<Attachment>
    ): Result<Post> = Assertions.fail()

    override suspend fun deletePost(regionUrlKey: URLKey, postUrlKey: URLKey): Result<Post> = Assertions.fail()

    override suspend fun describePost(
        regionUrlKey: URLKey,
        postUrlKey: URLKey,
        commentFetchConfig: PageableFetchConfig
    ): Result<Post> = Assertions.fail()

    override suspend fun describeRegion(
        regionUrlKey: URLKey,
        topPostFetchConfig: PageableFetchConfig
    ): Result<Region> = Assertions.fail()

    override suspend fun listCommentsUnderPost(
        regionUrlKey: URLKey,
        postUrlKey: URLKey,
        fetchConfig: PageableFetchConfig
    ): Result<PageableCollection<Comment>> = Assertions.fail()

    override suspend fun listPostsUnderRegion(
        regionUrlKey: URLKey,
        fetchConfig: PageableFetchConfig
    ): Result<PageableCollection<MinimizedPost>> = Assertions.fail()

    override val clientVersion: String
        get() = "TESTING"
    override val compatibleServiceVersion: String
        get() = "TESTING"
}
