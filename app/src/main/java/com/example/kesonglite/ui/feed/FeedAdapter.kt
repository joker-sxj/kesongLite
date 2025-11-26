package com.example.kesonglite.ui.feed

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.kesonglite.KesongLiteApp
import com.example.kesonglite.R
import com.example.kesonglite.data.local.UserLocalDataSource
import com.example.kesonglite.databinding.ItemFeedCardBinding
import com.example.kesonglite.domain.model.Post
import com.example.kesonglite.domain.usecase.UserInteractionUseCase
import com.example.kesonglite.ui.detail.DetailActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

class FeedAdapter(private var posts: MutableList<Post> = mutableListOf()) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    var onItemClickListener: ((Post) -> Unit)? = null
    private var userInteractionUseCase: UserInteractionUseCase? = null
    
    // 兼容构造函数
    constructor(context: Context, posts: MutableList<Post>) : this(posts) {
        // 初始化用户交互用例
        val app = context.applicationContext as KesongLiteApp
        userInteractionUseCase = app.container.userInteractionUseCase
    }
    
    // 更新数据列表 - 使用更高效的实现，支持null参数
    fun submitList(newPosts: List<Post>?) {
        // 保存旧数据的大小用于比较
        val oldSize = posts.size
        
        // 更新数据
        posts.clear()
        newPosts?.let { posts.addAll(it) }
        
        // 处理不同的情况以使用最高效的刷新方式
        if (newPosts == null || newPosts.isEmpty()) {
            // 如果新数据为空，使用批量移除通知
            if (oldSize > 0) {
                notifyItemRangeRemoved(0, oldSize)
            }
        } else if (oldSize == newPosts.size && oldSize > 0) {
            // 如果数据集大小相同且非空，尝试使用更高效的刷新方式
            // 检查是否所有元素都相同
            var allItemsSame = true
            for (i in 0 until oldSize) {
                if (posts[i].postId != newPosts[i].postId) {
                    allItemsSame = false
                    break
                }
            }
            
            if (allItemsSame) {
                // 只刷新所有可见项，而不是整个列表
                notifyItemRangeChanged(0, oldSize)
            } else {
                // 如果有元素ID不同，使用批量通知组合代替notifyDataSetChanged
                notifyItemRangeRemoved(0, oldSize)
                notifyItemRangeInserted(0, newPosts.size)
            }
        } else if (oldSize == 0) {
            // 如果旧数据为空，使用批量添加通知
            notifyItemRangeInserted(0, newPosts.size)
        } else {
            // 数据大小变化时，使用批量通知代替notifyDataSetChanged
            notifyItemRangeRemoved(0, oldSize)
            notifyItemRangeInserted(0, newPosts.size)
        }
    }
    
    // 设置用户交互用例
    fun setUserInteractionUseCase(useCase: UserInteractionUseCase) {
        this.userInteractionUseCase = useCase
    }

    inner class FeedViewHolder(val binding: ItemFeedCardBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * 更新点赞状态UI
     */
    private fun updateLikeState(binding: ItemFeedCardBinding, isLiked: Boolean, likeCount: Int?) {
        // 更新点赞图标
        binding.ivLike.setImageResource(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        // 更新点赞数量，确保安全处理null值
        binding.tvLikeCount.text = (likeCount ?: 0).toString()
    }

    /**
     * 设置封面图片的布局参数，应用指定的宽高比
     */
    private fun setCoverImageLayoutParams(binding: ItemFeedCardBinding, ratio: Float) {
        // 注意：这里需要确保 ivCover 的父布局是 ConstraintLayout
        val layoutParams = binding.ivCover.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        layoutParams?.dimensionRatio = "1:$ratio"
        binding.ivCover.layoutParams = layoutParams
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemFeedCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val post = posts[position]
        val binding = holder.binding
        val context = binding.root.context
        val userLocalDataSource = UserLocalDataSource(context)

        // 1. 动态计算宽高比 (限制在 3:4 到 4:3)
        val firstClip = post.clips?.firstOrNull()
        val ratio = if (firstClip != null && firstClip.width > 0) {
            val rawRatio = firstClip.height.toFloat() / firstClip.width.toFloat()
            rawRatio.coerceIn(0.75f, 1.33f) // 3:4 = 0.75, 4:3 = 1.33
        } else {
            1.33f // 默认使用 4:3
        }
        
        // 应用比例到 ImageView
        setCoverImageLayoutParams(binding, ratio)

        // 2. 加载内容，为文本添加空值检查和默认值
        binding.tvTitle.text = post.title?.takeIf { it.isNotEmpty() } ?: post.content?.takeIf { it.isNotEmpty() } ?: "无标题"
        binding.tvAuthor.text = post.author.nickname.takeIf { it.isNotEmpty() } ?: "匿名用户"
        
        // 确保有 clip 才能加载图片
        if (firstClip != null) {
            Glide.with(context).load(firstClip.url).into(binding.ivCover)
        } else {
            binding.ivCover.setImageDrawable(null) // 清空图片
        }
        
        Glide.with(context).load(post.author.avatar).transform(CircleCrop()).into(binding.ivAvatar)

        // 点赞逻辑 - 使用UseCase获取点赞状态
        val useCase = userInteractionUseCase
        if (useCase != null) {
            // 在后台线程获取点赞状态
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isLiked = useCase.getLikeState(post.postId)
                    val likeCount = useCase.getLikeCount(post.postId)
                    
                    // 更新post对象中的点赞数
                    if (post.likeCount == null) {
                        post.likeCount = likeCount
                    }
                    
                    // 在主线程更新UI
                    CoroutineScope(Dispatchers.Main).launch {
                        updateLikeState(binding, isLiked, post.likeCount)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // 使用整个点赞区域作为点击目标
            binding.likeArea.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // 使用UseCase切换点赞状态
                        val newLiked = useCase.toggleLike(post.postId).getOrDefault(false)
                        
                        // 更新点赞数量
                        val currentCount = post.likeCount ?: useCase.getLikeCount(post.postId)
                        val newCount = max(0, currentCount + if (newLiked) 1 else -1)
                        post.likeCount = newCount
                        
                        // 更新点赞数量到存储
                        useCase.updateLikeCount(post.postId, newCount)
                        
                        // 在主线程更新UI
                        CoroutineScope(Dispatchers.Main).launch {
                            updateLikeState(binding, newLiked, newCount)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            // 降级处理：如果UseCase未初始化，使用默认状态
            updateLikeState(binding, false, post.likeCount)
        }

        // 4. 点击跳转 (带共享元素转场) - 添加错误处理
        binding.cardView.setOnClickListener {
            try {
                // 调用外部点击监听器
                onItemClickListener?.invoke(post)
                
                // 或者直接跳转到详情页
                val intent = Intent(context, DetailActivity::class.java).apply {
                    putExtra("POST_DATA", post)
                }
                
                // 设置共享元素名称，使用postId确保唯一性
                binding.ivCover.transitionName = "hero_image_${post.postId}"
                
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    context as Activity,
                    binding.ivCover,
                    binding.ivCover.transitionName
                )
                context.startActivity(intent, options.toBundle())
            } catch (e: Exception) {
                // 捕获所有可能的异常，防止点击时应用崩溃
                android.widget.Toast.makeText(context, "跳转失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount() = posts.size
    

    
    fun updateData(newPosts: List<Post>) {
        // 保存旧数据的大小用于比较
        val oldSize = posts.size
        
        // 更新数据
        posts.clear()
        posts.addAll(newPosts)
        
        // 处理不同的情况以使用最高效的刷新方式
        if (newPosts.isEmpty()) {
            // 如果新数据为空，使用批量移除通知
            if (oldSize > 0) {
                notifyItemRangeRemoved(0, oldSize)
            }
        } else if (oldSize == 0) {
            // 如果旧数据为空，使用批量添加通知
            notifyItemRangeInserted(0, newPosts.size)
        } else if (oldSize == newPosts.size) {
            // 如果数据集大小相同，尝试使用更高效的刷新方式
            var allItemsSame = true
            for (i in newPosts.indices) {
                // 比较postId来确定是否为相同的帖子
                if (posts[i].postId != newPosts[i].postId) {
                    allItemsSame = false
                    break
                }
            }
            
            if (allItemsSame) {
                // 只刷新所有项，而不是整个列表
                notifyItemRangeChanged(0, newPosts.size)
            } else {
                // 如果有元素ID不同，使用更高效的批量通知组合
                notifyItemRangeRemoved(0, oldSize)
                notifyItemRangeInserted(0, newPosts.size)
            }
        } else {
            // 数据集大小变化时，使用更高效的批量通知组合
            notifyItemRangeRemoved(0, oldSize)
            notifyItemRangeInserted(0, newPosts.size)
        }
    }
    
    fun addData(newPosts: List<Post>) {
        val start = posts.size
        posts.addAll(newPosts)
        notifyItemRangeInserted(start, newPosts.size)
    }
}