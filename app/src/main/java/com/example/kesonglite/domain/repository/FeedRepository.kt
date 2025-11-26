package com.example.kesonglite.domain.repository

import com.example.kesonglite.domain.model.Post

/**
 * Feed数据仓库接口
 * 定义获取Feed数据的抽象方法
 */
interface FeedRepository {
    
    /**
     * 获取Feed数据
     * @param page 页码
     * @param pageSize 每页数量
     * @return 帖子列表
     */
    suspend fun getFeed(page: Int, pageSize: Int): List<Post>
    
    /**
     * 获取单个帖子详情
     * @param postId 帖子ID
     * @return 帖子详情
     */
    suspend fun getPostById(postId: String): Post?
    
    /**
     * 刷新Feed数据
     * @return 最新的帖子列表
     */
    suspend fun refreshFeed(): List<Post>
}