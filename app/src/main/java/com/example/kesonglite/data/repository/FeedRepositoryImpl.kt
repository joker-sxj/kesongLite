package com.example.kesonglite.data.repository

import com.example.kesonglite.data.local.FeedLocalDataSource
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
    
    // 内存缓存的帖子列表
    private var cachedPosts: List<Post> = emptyList()
    
    override suspend fun getFeed(page: Int, pageSize: Int): List<Post> {
        // 第一页数据优先从本地缓存获取，提升冷启动体验
        if (page == 0) {
            val localPosts = FeedLocalDataSource.getFeed()
            if (localPosts != null && localPosts.isNotEmpty()) {
                cachedPosts = localPosts
                return localPosts
            }
        }
        
        // 从网络获取数据
        val newPosts = remoteDataSource.getFeed(page, pageSize)
        
        // 缓存第一页数据到本地
        if (page == 0) {
            cachedPosts = newPosts
            FeedLocalDataSource.saveFeed(newPosts)
        } else {
            // 对于后续页面，追加到内存缓存中
            cachedPosts = cachedPosts + newPosts
        }
        
        return newPosts
    }
    
    override suspend fun getPostById(postId: String): Post? {
        // 先从内存缓存中查找
        cachedPosts.find { it.postId == postId }?.let { return it }
        
        // 内存缓存中没有则从本地缓存查找
        val localPosts = FeedLocalDataSource.getFeed()
        localPosts?.find { it.postId == postId }?.let { return it }
        
        // 缓存中没有则从网络获取
        // 这里简化处理，实际应该单独调用获取单个帖子的接口
        return null
    }
    
    override suspend fun refreshFeed(): List<Post> {
        // 重新从网络获取第一页数据
        val newPosts = getFeed(0, 20)
        // 更新本地缓存
        FeedLocalDataSource.saveFeed(newPosts)
        return newPosts
    }
}