package com.example.kesonglite.data.user

import android.content.Context
import com.example.kesonglite.core.prefs.PrefsManager

/**
 * 用户相关本地数据源
 * 封装点赞、关注、静音等业务逻辑
 */
private const val KEY_MUTED = "is_muted"

class UserLocalDataSource(context: Context) {
    private val prefsManager = PrefsManager(context)

    /**
     * 检查帖子是否被点赞
     */
    fun isPostLiked(postId: String): Boolean {
        return prefsManager.getBoolean("like_$postId", false)
    }

    /**
     * 切换帖子点赞状态
     * @return 返回切换后的状态
     */
    fun togglePostLiked(postId: String): Boolean {
        val currentState = isPostLiked(postId)
        val newState = !currentState
        prefsManager.putBoolean("like_$postId", newState)
        return newState
    }

    /**
     * 获取帖子的点赞数量
     */
    fun getLikeCount(postId: String): Int {
        return prefsManager.getInt("like_count_$postId", 0)
    }

    /**
     * 设置帖子的点赞数量
     */
    fun setLikeCount(postId: String, likeCount: Int) {
        prefsManager.putInt("like_count_$postId", likeCount)
    }

    /**
     * 检查作者是否被关注
     */
    fun isUserFollowed(userId: String): Boolean {
        return prefsManager.getBoolean("follow_$userId", false)
    }

    /**
     * 切换作者关注状态
     * @return 返回切换后的状态
     */
    fun toggleUserFollowed(userId: String): Boolean {
        val currentState = isUserFollowed(userId)
        val newState = !currentState
        prefsManager.putBoolean("follow_$userId", newState)
        return newState
    }

    /**
     * 检查是否处于静音状态
     */
    fun isMuted(): Boolean {
        return prefsManager.getBoolean(KEY_MUTED, false)
    }

    /**
     * 设置静音状态
     */
    fun setMuted(isMuted: Boolean) {
        prefsManager.putBoolean(KEY_MUTED, isMuted)
    }
}