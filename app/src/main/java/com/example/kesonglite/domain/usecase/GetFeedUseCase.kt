package com.example.kesonglite.domain.usecase

import com.example.kesonglite.domain.model.Post
import com.example.kesonglite.domain.repository.FeedRepository

/**
 * 获取Feed数据的用例
 * 负责处理Feed数据的获取业务逻辑
 */
class GetFeedUseCase(private val repository: FeedRepository) {
    
    /**
     * 获取Feed数据
     * @param page 页码
     * @param pageSize 每页数量
     * @return 帖子列表
     */
    suspend operator fun invoke(page: Int = 0, pageSize: Int = 20): Result<List<Post>> {
        return try {
            val posts = repository.getFeed(page, pageSize)
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 刷新Feed数据
     */
    suspend fun refresh(): Result<List<Post>> {
        return invoke(0, 20)
    }
}