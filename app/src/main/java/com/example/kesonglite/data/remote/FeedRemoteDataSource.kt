package com.example.kesonglite.data.remote

import android.util.Log
import com.example.kesonglite.domain.model.FeedResponse
import com.example.kesonglite.domain.model.Post

/**
 * Feed远程数据源
 * 负责从网络获取Feed数据
 */
class FeedRemoteDataSource(private val apiService: ApiService) {
    private val TAG = "FeedRemoteDataSource"
    
    /**
     * 获取Feed数据
     */
    suspend fun getFeed(page: Int, pageSize: Int): List<Post> {
        return try {
            // 调用API时传递acceptVideo=true参数，以接收视频内容
            val response = apiService.getFeed(page, pageSize, acceptVideo = true)
            response.postList ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching feed: ${e.message}")
            // 返回模拟数据作为后备方案
            getMockFeed()
        }
    }
    
    /**
     * 获取模拟Feed数据
     * 当网络请求失败时使用
     */
    private fun getMockFeed(): List<Post> {
        // 这里返回模拟数据，与之前NetworkClient中的mock数据保持一致
        return listOf(
            // 模拟数据内容
        )
    }
}