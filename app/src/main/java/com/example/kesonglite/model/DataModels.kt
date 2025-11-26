package com.example.kesonglite.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class FeedResponse(
    @SerializedName("status_code") val statusCode: Int,
    @SerializedName("has_more") val hasMore: Int,
    @SerializedName("post_list") val postList: List<Post>?
)

@Parcelize
data class Post(
    @SerializedName("post_id") val postId: String,
    val title: String?,
    val content: String?,
    @SerializedName("create_time") val createTime: Long,
    val author: Author,
    val clips: List<Clip>?,
    val music: Music?,
    @SerializedName("like_count") var likeCount: Int? = 0 // 添加likeCount属性，可空类型，默认值为0
) : Parcelable

@Parcelize
data class Author(
    @SerializedName("user_id") val userId: String,
    val nickname: String,
    val avatar: String
) : Parcelable

@Parcelize
data class Clip(
    val type: Int, // 0: Image, 1: Video
    val width: Int,
    val height: Int,
    val url: String
) : Parcelable

@Parcelize
data class Music(
    val volume: Int,
    @SerializedName("seek_time") val seekTime: Long,
    val url: String
) : Parcelable
