package com.example.kesonglite.data.prefs

import android.content.Context

/**
 * 应用设置管理器
 * 负责管理应用级别的偏好设置，如静音状态等
 */
class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "kesong_prefs"
        private const val KEY_MUTED = "is_muted"
    }
    
    /**
     * 检查是否处于静音状态
     */
    fun isMuted(): Boolean {
        return prefs.getBoolean(KEY_MUTED, false) // 默认非静音
    }
    
    /**
     * 设置静音状态
     */
    fun setMuted(isMuted: Boolean) {
        prefs.edit().putBoolean(KEY_MUTED, isMuted).apply()
    }
}