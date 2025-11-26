package com.example.kesonglite.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * 用户本地数据源
 * 负责管理用户交互状态，如点赞、关注等
 */
class UserLocalDataSource(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "user_interaction_state"
    }
    
    /**
     * 检查帖子是否被点赞
     */
    fun isPostLiked(postId: String): Boolean {
        return prefs.getBoolean("like_$postId", false)
    }
    
    /**
     * 设置帖子点赞状态
     */
    fun setPostLiked(postId: String, isLiked: Boolean) {
        prefs.edit().putBoolean("like_$postId", isLiked).apply()
    }
    
    /**
     * 切换帖子点赞状态
     */
    fun togglePostLiked(postId: String): Boolean {
        val currentState = isPostLiked(postId)
        val newState = !currentState
        setPostLiked(postId, newState)
        return newState
    }
    
    /**
     * 获取帖子的点赞数量
     */
    fun getLikeCount(postId: String): Int {
        return prefs.getInt("like_count_$postId", 0) // 默认值为0
    }
    
    /**
     * 设置帖子的点赞数量
     */
    fun setLikeCount(postId: String, likeCount: Int) {
        prefs.edit().putInt("like_count_$postId", likeCount).apply()
    }
    
    /**
     * 检查作者是否被关注
     */
    fun isUserFollowed(userId: String): Boolean {
        return prefs.getBoolean("follow_$userId", false)
    }
    
    /**
     * 设置作者关注状态
     */
    fun setUserFollowed(userId: String, isFollowed: Boolean) {
        prefs.edit().putBoolean("follow_$userId", isFollowed).apply()
    }
    
    /**
     * 切换用户关注状态
     */
    fun toggleUserFollowed(userId: String): Boolean {
        val currentState = isUserFollowed(userId)
        val newState = !currentState
        setUserFollowed(userId, newState)
        return newState
    }
}