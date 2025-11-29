package com.example.kesonglite.data.local

import com.example.kesonglite.domain.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Feed本地数据源
 * 用于缓存Feed数据，提升冷启动体验
 */
object FeedLocalDataSource {
    
    private val cache = ConcurrentHashMap<String, List<Post>>()
    private const val CACHE_KEY = "feed_cache"
    
    /**
     * 保存Feed数据到本地缓存
     */
    suspend fun saveFeed(posts: List<Post>) = withContext(Dispatchers.IO) {
        cache[CACHE_KEY] = posts
    }
    
    /**
     * 从本地缓存获取Feed数据
     */
    suspend fun getFeed(): List<Post>? = withContext(Dispatchers.IO) {
        cache[CACHE_KEY]
    }
    
    /**
     * 清除本地缓存
     */
    suspend fun clearFeed() = withContext(Dispatchers.IO) {
        cache.remove(CACHE_KEY)
    }
}