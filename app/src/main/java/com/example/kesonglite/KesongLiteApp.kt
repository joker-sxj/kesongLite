package com.example.kesonglite

import android.app.Application
import com.example.kesonglite.di.AppContainer

/**
 * 应用程序类
 * 初始化全局依赖容器
 */
class KesongLiteApp : Application() {
    
    // 全局依赖容器
    lateinit var container: AppContainer
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化依赖容器
        container = AppContainer(applicationContext)
        
        // 冷启动时重置应用状态
        com.example.kesonglite.data.state.AppStateMemory.coldStartReset()
    }
}