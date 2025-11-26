package com.example.kesonglite.core.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 基础的SharedPreferences管理类
 * 只负责键值对的读写，不包含业务逻辑
 */
class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kesong_prefs", Context.MODE_PRIVATE)

    /**
     * 读取布尔值
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    /**
     * 写入布尔值
     */
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    /**
     * 读取整数
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    /**
     * 写入整数
     */
    fun putInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    /**
     * 清除所有数据
     */
    fun clear() {
        prefs.edit { clear() }
    }
}