package com.example.kesonglite.data.state

object AppStateMemory {
    // 全局静音状态，支持APP生命周期内生效
    private var globalMutedState: Boolean = false
    
    // 初始为非静音
    fun isMuted(postId: String): Boolean = globalMutedState
    
    fun setMuted(postId: String, isMuted: Boolean) {
        globalMutedState = isMuted
    }
    
    // APP 冷启动后重置
    fun coldStartReset() {
        globalMutedState = false
    }
}