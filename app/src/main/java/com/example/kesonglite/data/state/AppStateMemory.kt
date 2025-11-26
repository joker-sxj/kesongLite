package com.example.kesonglite.data.state

object AppStateMemory {
    // key: postId, value: isMuted (true for Muted/Stopped, false for Playing/Auto-Scroll)
    private val playbackStates = mutableMapOf<String, Boolean>()
    
    // 初始为非静音
    fun isMuted(postId: String): Boolean = playbackStates[postId] ?: false
    
    fun setMuted(postId: String, isMuted: Boolean) {
        playbackStates[postId] = isMuted
    }
    
    // 模拟 APP 冷启动后重置
    fun coldStartReset() {
        playbackStates.clear()
    }
}