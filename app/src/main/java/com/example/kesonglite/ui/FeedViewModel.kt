package com.example.kesonglite.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kesonglite.model.Post
import com.example.kesonglite.network.NetworkClient
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isError = MutableLiveData<Boolean>()
    val isError: LiveData<Boolean> = _isError

    private var hasMore = true
    private var isFetching = false

    init {
        fetchFeed(isRefresh = true)
    }

    fun fetchFeed(isRefresh: Boolean = false) {
        if (isFetching || (!hasMore && !isRefresh)) return
        
        isFetching = true
        _isLoading.value = true
        _isError.value = false

        viewModelScope.launch {
            try {
                val response = NetworkClient.service.getFeed(count = 10, acceptVideo = true)
                
                if (response.statusCode == 0 && response.postList != null) {
                    hasMore = response.hasMore == 1
                    val currentList = if (isRefresh) emptyList() else _posts.value.orEmpty()
                    _posts.value = currentList + response.postList
                } else {
                    _isError.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isError.value = true
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
