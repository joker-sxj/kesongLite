package com.example.kesonglite.core.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    /**
     * 格式化时间戳为指定格式的字符串。
     * 规则：
     * - 24h内：HH:mm
     * - 7天内：x天前
     * - 其余：MM-dd
     * 假设时间戳为秒级，如果实际为毫秒级，请移除 * 1000
     */
    fun format(timestamp: Long): String {
        val timeInMillis = timestamp * 1000L // 假设时间戳为秒级
        val now = System.currentTimeMillis()
        val diff = now - timeInMillis
        
        val oneDay = 24 * 60 * 60 * 1000L
        val sevenDays = 7 * oneDay
        
        return when {
            diff < oneDay -> {
                // 24h内，显示具体时间
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeInMillis))
            }
            diff < sevenDays -> {
                // 7天内，显示为 x天前
                "${diff / oneDay}天前"
            }
            else -> {
                // 其余显示为具体日期
                SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timeInMillis))
            }
        }
    }
}