package com.example.kesonglite.data.remote

import com.example.kesonglite.domain.model.FeedResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API服务接口
 * 定义所有网络请求的方法
 */
interface ApiService {
    
    /**
     * 获取Feed数据
     * @param page 页码
     * @param pageSize 每页数量
     * @return Feed响应数据
     */
    @GET("/api/feed")
    suspend fun getFeed(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
        @Query("accept_video_clip") acceptVideo: Boolean = true
    ): FeedResponse
    
    /**
     * 获取单个帖子详情
     * @param postId 帖子ID
     * @return 帖子详情响应
     */
    @GET("/api/post/detail")
    suspend fun getPostDetail(
        @Query("post_id") postId: String
    ): FeedResponse
}