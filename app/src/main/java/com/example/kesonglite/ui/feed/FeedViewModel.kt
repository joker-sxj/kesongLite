package com.example.kesonglite.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kesonglite.domain.model.Post
import com.example.kesonglite.domain.usecase.GetFeedUseCase
import kotlinx.coroutines.launch

class FeedViewModel(
    private val getFeedUseCase: GetFeedUseCase
) : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var hasMore = true
    private var isFetching = false

    init {
        fetchFeed(isRefresh = true)
    }

    fun fetchFeed(isRefresh: Boolean = false) {
        if (isFetching || (!hasMore && !isRefresh)) return
        
        isFetching = true
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // 使用UseCase获取数据
                val page = if (isRefresh) 0 else (_posts.value?.size ?: 0) / 20
                val result = getFeedUseCase.invoke(page, 20)
                
                // 处理Result对象
                if (result.isSuccess) {
                    val newPosts = result.getOrElse { emptyList() }
                    // 如果没有更多数据，则停止加载
                    hasMore = newPosts.isNotEmpty()
                    
                    val currentList = if (isRefresh) emptyList() else _posts.value.orEmpty()
                    _posts.value = currentList + newPosts
                } else {
                    _error.value = "获取数据失败"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
                isFetching = false
            }
        }
    }
    
    fun refreshFeed() {
        hasMore = true // 重置 hasMore 状态以允许刷新
        fetchFeed(isRefresh = true)
    }
}