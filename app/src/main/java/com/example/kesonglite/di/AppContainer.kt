package com.example.kesonglite.di

import android.content.Context
import com.example.kesonglite.data.prefs.PrefsManager
import com.example.kesonglite.data.remote.ApiClient
import com.example.kesonglite.data.remote.FeedRemoteDataSource
import com.example.kesonglite.data.repository.FeedRepositoryImpl
import com.example.kesonglite.data.repository.UserInteractionRepositoryImpl
import com.example.kesonglite.domain.repository.FeedRepository
import com.example.kesonglite.domain.repository.UserInteractionRepository
import com.example.kesonglite.domain.usecase.GetFeedUseCase
import com.example.kesonglite.domain.usecase.UserInteractionUseCase

/**
 * 应用容器
 * 管理所有依赖注入
 */
class AppContainer(context: Context) {
    // 远程数据源
    private val feedRemoteDataSource = FeedRemoteDataSource(ApiClient.apiService)
    
    // 数据仓库
    val feedRepository: FeedRepository = FeedRepositoryImpl(feedRemoteDataSource)
    val userInteractionRepository: UserInteractionRepository = UserInteractionRepositoryImpl(context)
    
    // 用例
    val getFeedUseCase = GetFeedUseCase(feedRepository)
    val userInteractionUseCase = UserInteractionUseCase(userInteractionRepository)
    
    // 偏好设置管理器
    val prefsManager = PrefsManager(context)
}