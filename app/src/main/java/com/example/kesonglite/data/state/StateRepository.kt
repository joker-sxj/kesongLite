package com.example.kesonglite.data.state

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 本地状态持久化工具
 * 用于保存点赞/关注等用户交互状态
 */
class StateRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_interaction_state", Context.MODE_PRIVATE)

    /**
     * 保存帖子的点赞状态
     */
    fun saveLikeState(postId: String, isLiked: Boolean) {
        sharedPreferences.edit {
            putBoolean("like_$postId", isLiked)
        }
    }

    /**
     * 获取帖子的点赞状态
     */
    fun getLikeState(postId: String): Boolean {
        return sharedPreferences.getBoolean("like_$postId", false)
    }

    /**
     * 保存用户的关注状态
     */
    fun saveFollowState(userId: String, isFollowing: Boolean) {
        sharedPreferences.edit {
            putBoolean("follow_$userId", isFollowing)
        }
    }

    /**
     * 获取用户的关注状态
     */
    fun getFollowState(userId: String): Boolean {
        return sharedPreferences.getBoolean("follow_$userId", false)
    }

    /**
     * 清除所有状态数据
     */
    fun clearAllStates() {
        sharedPreferences.edit {
            clear()
        }
    }
}