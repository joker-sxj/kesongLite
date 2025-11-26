package com.example.kesonglite.ui.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kesonglite.KesongLiteApp
import com.example.kesonglite.domain.usecase.GetFeedUseCase

/**
 * FeedViewModel工厂类
 * 负责创建FeedViewModel实例并注入依赖
 */
class FeedViewModelFactory : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            val context = KesongLiteApp().applicationContext
            val app = context.applicationContext as KesongLiteApp
            
            // 从AppContainer获取依赖
            return FeedViewModel(app.container.getFeedUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}