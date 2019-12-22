package com.kcibald.services.fronting.utils

import com.kcibald.objects.Attachment
import com.kcibald.objects.Comment
import com.kcibald.objects.Post
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf

fun Post.json(): JsonObject {
    val attachments = JsonArray(this.attachments.map(Attachment::json))
    val commentQM = this.comments.queryMark
    val comments = JsonArray(this.comments.currentContent.map(Comment::json))
    return jsonObjectOf(
        "attachments" to attachments,
        "comments" to comments,
        "comment_query_mark" to commentQM,
        "title" to this.title,
        "url_key" to this.urlKey,
        "region_url_key" to this.sourceRegionURLKey,
        "author" to this.author.json(),
        "create_time_stamp" to this.createTimestamp,
        "content" to this.content,
        "comment_count" to this.commentCount
    )
}

fun Attachment.json(): JsonObject = jsonObjectOf(
    "title" to this.name,
    "link" to this.file
)

fun Comment.json(): JsonObject = jsonObjectOf(
    "comment_id" to this.id,
    "author" to this.author.json(),
    "create_time_stamp" to this.createTimestamp,
    "content" to this.content,
    "attachments" to this.attachments,
    "replies" to JsonArray()
)