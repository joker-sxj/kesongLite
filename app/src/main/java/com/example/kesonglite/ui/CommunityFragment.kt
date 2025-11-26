package com.example.kesonglite.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.kesonglite.databinding.FragmentCommunityBinding
import com.example.kesonglite.R
import com.example.kesonglite.utils.PersistenceManager

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels()
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs(binding)
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    fun setupTabs(binding: FragmentCommunityBinding) {
        val tabs = listOf(
            binding.tvTabBeijing,
            binding.tvTabGroupBuy,
            binding.tvTabFollow,
            binding.tvTabCommunity, // 社区
            binding.tvTabRecommend
        )

        // 默认选中"社区"
        updateTabSelection(binding.tvTabCommunity, tabs)

        tabs.forEach { tabView ->
            tabView.setOnClickListener {
                if (tabView.id == R.id.tvTabCommunity) {
                    // 仅"社区"支持切换/加载数据
                    updateTabSelection(tabView, tabs)
                    // TODO: 触发 RecyclerView 加载"社区"数据
                    Toast.makeText(context, "已切换到社区", Toast.LENGTH_SHORT).show()
                } else {
                    // 其他 Tab 仅提示，无需支持功能
                    Toast.makeText(context, "Tab: ${tabView.text} (功能暂不支持)", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // 搜索图标交互
        binding.ivSearchIcon.setOnClickListener {
             Toast.makeText(context, "搜索交互", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTabSelection(selectedTab: TextView, allTabs: List<TextView>) {
        allTabs.forEach { tab ->
            // 使用更新的 setTextAppearance 方法切换样式（无Context参数）
            if (tab == selectedTab) {
                tab.setTextAppearance(R.style.TopTabTextStyle_Selected)
            } else {
                tab.setTextAppearance(R.style.TopTabTextStyle)
            }
        }
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(requireContext(), mutableListOf())
        binding.recyclerView.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = feedAdapter
            addItemDecoration(GridSpacingItemDecoration(2, 8, true)) // 假设 GridSpacingItemDecoration 存在
        }
    }

    private fun setupObservers() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            feedAdapter.updateData(posts)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            // TODO: 处理 Loading/Error State Views
        }

        viewModel.isError.observe(viewLifecycleOwner) { isError ->
            if (isError) {
                // TODO: 显示错误提示
            }
        }
    }

    private fun setupListeners() {
        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshFeed()
        }

        // 上滑 LoadMore
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as StaggeredGridLayoutManager
                val lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null)
                val totalItemCount = layoutManager.itemCount
                
                // 检查是否滑动到倒数第5个位置
                val threshold = 5
                if (lastVisibleItemPositions.any { it >= totalItemCount - threshold }) {
                    viewModel.fetchFeed()
                }
            }
        })
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
        // 确保adapter已初始化
        if (::feedAdapter.isInitialized) {
            viewModel.posts.value?.let { posts ->
                // 遍历所有帖子，从持久化存储更新点赞状态和数量
                posts.forEachIndexed { index, post ->
                    val isLiked = PersistenceManager.isLiked(requireContext(), post.postId)
                    val likeCount = PersistenceManager.getLikeCount(requireContext(), post.postId)
                    
                    // 更新post对象中的点赞状态和数量
                    post.likeCount = likeCount
                    
                    // 只在RecyclerView可见范围内更新UI，避免不必要的性能开销
                    val viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(index) as? FeedAdapter.FeedViewHolder
                    viewHolder?.let {
                        it.binding.ivLike.setImageResource(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
                        it.binding.tvLikeCount.text = likeCount.toString()
                    }
                }
                // 可选：强制刷新适配器
                // feedAdapter.notifyDataSetChanged()
            }
        }
    }
}
