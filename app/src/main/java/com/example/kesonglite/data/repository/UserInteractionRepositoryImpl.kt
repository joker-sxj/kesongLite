package com.example.kesonglite.data.repository

import android.content.Context
import com.example.kesonglite.data.local.UserLocalDataSource
import com.example.kesonglite.domain.repository.UserInteractionRepository

/**
 * 用户交互数据仓库实现
 * 实现UserInteractionRepository接口，使用UserLocalDataSource进行本地数据存储
 */
class UserInteractionRepositoryImpl(context: Context) : UserInteractionRepository {
    
    private val localDataSource = UserLocalDataSource(context)
    
    override suspend fun toggleLike(postId: String): Boolean {
        return localDataSource.togglePostLiked(postId)
    }
    
    override suspend fun getLikeState(postId: String): Boolean {
        return localDataSource.isPostLiked(postId)
    }
    
    override suspend fun updateLikeCount(postId: String, count: Int) {
        localDataSource.setLikeCount(postId, count)
    }
    
    override suspend fun getLikeCount(postId: String): Int {
        return localDataSource.getLikeCount(postId)
    }
    
    override suspend fun toggleFollow(userId: String): Boolean {
        // 使用UserLocalDataSource的方法
        // 注意：我们需要在UserLocalDataSource中添加toggleUserFollowed方法
        val currentState = localDataSource.isUserFollowed(userId)
        localDataSource.setUserFollowed(userId, !currentState)
        return !currentState
    }
    
    override suspend fun getFollowState(userId: String): Boolean {
        return localDataSource.isUserFollowed(userId)
    }
}