package com.example.kesonglite.utils

import android.content.Context

object PersistenceManager {
    private const val PREF_NAME = "kesong_prefs"
    private const val KEY_MUTED = "is_muted"

    /**
     * 检查帖子是否被点赞
     */
    fun isLiked(context: Context, postId: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("like_$postId", false)
    }

    /**
     * 设置帖子点赞状态
     */
    fun setLiked(context: Context, postId: String, isLiked: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("like_$postId", isLiked).apply()
    }

    /**
     * 获取帖子的点赞数量
     */
    fun getLikeCount(context: Context, postId: String): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("like_count_$postId", 0) // 默认值为0
    }

    /**
     * 设置帖子的点赞数量
     */
    fun setLikeCount(context: Context, postId: String, likeCount: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt("like_count_$postId", likeCount).apply()
    }

    /**
     * 检查是否处于静音状态
     */
    fun isMuted(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MUTED, false) // 默认非静音
    }

    /**
     * 设置静音状态
     */
    fun setMuted(context: Context, isMuted: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MUTED, isMuted).apply()
    }
    
    /**
     * 检查作者是否被关注
     */
    fun isFollowed(context: Context, userId: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("follow_$userId", false)
    }

    /**
     * 设置作者关注状态
     */
    fun setFollowed(context: Context, userId: String, isFollowed: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("follow_$userId", isFollowed).apply()
    }
}
