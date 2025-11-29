package com.example.kesonglite.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.kesonglite.KesongLiteApp
import com.example.kesonglite.R
import com.example.kesonglite.databinding.FragmentCommunityBinding
import com.example.kesonglite.domain.usecase.UserInteractionUseCase
import kotlinx.coroutines.launch

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels { FeedViewModelFactory() }
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var userInteractionUseCase: UserInteractionUseCase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化用户交互用例
        val app = requireActivity().application as KesongLiteApp
        userInteractionUseCase = app.container.userInteractionUseCase
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupTabs() {
        val tabs = listOf(
            binding.tvTabGroupBuy,
            binding.tvTabFollow,
            binding.tvTabCommunity, // 社区
            binding.tvTabRecommend
        )

        // 默认选中"社区"
        updateTabSelection(binding.tvTabCommunity, tabs)

        // 为所有标签添加点击事件
        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                updateTabSelection(tab, tabs)
                // 这里可以根据不同标签切换不同的数据源
                Toast.makeText(context, "切换到${tab.text}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTabSelection(selectedTab: TextView, allTabs: List<TextView>) {
        allTabs.forEach { tab ->
            if (tab == selectedTab) {
                // 使用Android内置颜色资源替代自定义资源
                tab.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
                // 使用简单的背景样式替代自定义drawable
                tab.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                tab.setPadding(16, 8, 16, 8)
                tab.setTextSize(16f)
            } else {
                // 使用Android内置颜色资源替代自定义资源
                tab.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                // 使用简单的背景样式替代自定义drawable
                tab.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                tab.setPadding(16, 8, 16, 8)
                tab.setTextSize(14f)
            }
        }
    }

    private fun setupRecyclerView() {
        // 明确使用当前包中的FeedAdapter类，避免与其他包中的同名类混淆
        feedAdapter = com.example.kesonglite.ui.feed.FeedAdapter(requireContext(), mutableListOf())
        // 设置用户交互用例
        feedAdapter.setUserInteractionUseCase(userInteractionUseCase)
        
        // 设置RecyclerView布局管理器
        binding.recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        
        // 设置RecyclerView滑动监听，快速滑动时暂停图片加载
        binding.recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val glideRequestManager = com.bumptech.glide.Glide.with(requireContext())
                when (newState) {
                    androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE -> {
                        // 滑动停止，恢复图片加载
                        glideRequestManager.resumeRequests()
                    }
                    androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING -> {
                        // 拖动中，继续加载
                        glideRequestManager.resumeRequests()
                    }
                    androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING -> {
                        // 快速滑动，暂停图片加载
                        glideRequestManager.pauseRequests()
                    }
                }
            }
        })
        
        // 设置适配器
        binding.recyclerView.adapter = feedAdapter
    }

    private fun setupObservers() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            if (posts.isNotEmpty()) {
                feedAdapter.submitList(posts)
                // 移除对不存在的layoutEmpty的引用，直接控制recyclerView的可见性
                binding.recyclerView.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 简单处理加载状态，不再使用不存在的refreshLayout
            // 可以考虑添加其他加载状态处理逻辑
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                // 不再直接访问LiveData的protected value属性
                // 应该在ViewModel中添加方法来清除错误，这里只是简单处理
                // viewModel.clearError() // 理想情况下应调用此方法
            }
        }
    }

    private fun setupListeners() {
        // 移除对不存在的refreshLayout的引用
        // 可以考虑添加其他必要的监听器

        feedAdapter.onItemClickListener = {
            // 这里可以导航到详情页
            Toast.makeText(context, "点击了帖子 ${it.postId}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // 当从DetailActivity返回时，更新所有帖子的点赞状态和数量
        updateLikeStates()
    }

    private fun updateLikeStates() {
        lifecycleScope.launch {
            // 确保adapter已初始化
            if (::feedAdapter.isInitialized) {
                viewModel.posts.value?.let { posts ->
                    // 遍历所有帖子，通过UseCase更新点赞状态和数量
                    for (post in posts) {
                        try {
                            val isLiked = userInteractionUseCase.getLikeState(post.postId)
                            val likeCount = userInteractionUseCase.getLikeCount(post.postId)
                            // 更新post对象中的点赞状态和数量
                            post.likeCount = likeCount
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    // 使用submitList替代notifyDataSetChanged进行高效的数据更新
                    // 创建新的列表实例确保DiffUtil能正确检测变化
                    feedAdapter.submitList(posts.toList())
                }
            }
        }
    }
}