package com.example.kesonglite.data.repository

import com.example.kesonglite.data.remote.FeedRemoteDataSource
import com.example.kesonglite.domain.model.Post
import com.example.kesonglite.domain.repository.FeedRepository

/**
 * Feed数据仓库实现
 * 实现FeedRepository接口，整合本地和远程数据源
 */
class FeedRepositoryImpl(
    private val remoteDataSource: FeedRemoteDataSource
) : FeedRepository {
    
    // 缓存的帖子列表
    private var cachedPosts: List<Post> = emptyList()
    
    override suspend fun getFeed(page: Int, pageSize: Int): List<Post> {
        val newPosts = remoteDataSource.getFeed(page, pageSize)
        
        // 缓存第一页数据
        if (page == 0) {
            cachedPosts = newPosts
        } else {
            // 对于后续页面，追加到缓存中
            cachedPosts = cachedPosts + newPosts
        }
        
        return newPosts
    }
    
    override suspend fun getPostById(postId: String): Post? {
        // 先从缓存中查找
        cachedPosts.find { it.postId == postId }?.let { return it }
        
        // 缓存中没有则从网络获取
        // 这里简化处理，实际应该单独调用获取单个帖子的接口
        return null
    }
    
    override suspend fun refreshFeed(): List<Post> {
        // 重新获取第一页数据
        return getFeed(0, 20)
    }
}