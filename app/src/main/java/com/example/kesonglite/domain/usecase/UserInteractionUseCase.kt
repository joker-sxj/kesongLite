package com.example.kesonglite.domain.usecase

import com.example.kesonglite.domain.repository.UserInteractionRepository

/**
 * 用户交互用例
 * 负责处理用户的点赞、关注等交互操作
 */
class UserInteractionUseCase(private val repository: UserInteractionRepository) {
    
    /**
     * 切换帖子点赞状态
     * @param postId 帖子ID
     * @return 新的点赞状态
     */
    suspend fun toggleLike(postId: String): Result<Boolean> {
        return try {
            val newState = repository.toggleLike(postId)
            Result.success(newState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取帖子点赞状态
     * @param postId 帖子ID
     * @return 点赞状态
     */
    suspend fun getLikeState(postId: String): Boolean {
        return repository.getLikeState(postId)
    }
    
    /**
     * 更新点赞数量
     * @param postId 帖子ID
     * @param count 点赞数量
     */
    suspend fun updateLikeCount(postId: String, count: Int) {
        repository.updateLikeCount(postId, count)
    }
    
    /**
     * 获取点赞数量
     * @param postId 帖子ID
     * @return 点赞数量
     */
    suspend fun getLikeCount(postId: String): Int {
        return repository.getLikeCount(postId)
    }
    
    /**
     * 切换用户关注状态
     * @param userId 用户ID
     * @return 新的关注状态
     */
    suspend fun toggleFollow(userId: String): Result<Boolean> {
        return try {
            val newState = repository.toggleFollow(userId)
            Result.success(newState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取用户关注状态
     * @param userId 用户ID
     * @return 关注状态
     */
    suspend fun getFollowState(userId: String): Boolean {
        return repository.getFollowState(userId)
    }
}