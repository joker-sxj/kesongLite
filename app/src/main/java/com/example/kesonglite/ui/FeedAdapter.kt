package com.example.kesonglite.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.kesonglite.R
import com.example.kesonglite.databinding.ItemFeedCardBinding
import com.example.kesonglite.model.Post
import com.example.kesonglite.utils.PersistenceManager
import kotlin.math.max

class FeedAdapter(private val context: Context, private var posts: MutableList<Post>) :
    RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

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
        binding.tvAuthor.text = post.author.nickname?.takeIf { it.isNotEmpty() } ?: "匿名用户"
        
        // 确保有 clip 才能加载图片
        if (firstClip != null) {
            Glide.with(context).load(firstClip.url).into(binding.ivCover)
        } else {
            binding.ivCover.setImageDrawable(null) // 清空图片
        }
        
        Glide.with(context).load(post.author.avatar).transform(CircleCrop()).into(binding.ivAvatar)
        


        // 点赞逻辑 - 从持久化存储获取点赞状态和数量
        val isLiked = PersistenceManager.isLiked(context, post.postId)
        // 如果post.likeCount为null，则从持久化存储获取点赞数
        if (post.likeCount == null) {
            post.likeCount = PersistenceManager.getLikeCount(context, post.postId)
        }
        updateLikeState(binding, isLiked, post.likeCount)
        
        // 使用整个点赞区域作为点击目标
        binding.likeArea.setOnClickListener {
            val currentLiked = PersistenceManager.isLiked(context, post.postId)
            val newLiked = !currentLiked
            
            // 更新点赞数量，使用更简洁的方式处理null和负值情况
            if (newLiked != currentLiked) {
                post.likeCount = max(0, (post.likeCount ?: 0) + if (newLiked) 1 else -1)
            }
            
            // 持久化状态
            PersistenceManager.setLiked(context, post.postId, newLiked)
            // 同时持久化点赞数量
            post.likeCount?.let { PersistenceManager.setLikeCount(context, post.postId, it) }
            
            // 更新UI
            updateLikeState(binding, newLiked, post.likeCount)
        }

        // 4. 点击跳转 (带共享元素转场) - 添加错误处理
        binding.cardView.setOnClickListener {
            try {
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
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
    
    fun addData(newPosts: List<Post>) {
        val start = posts.size
        posts.addAll(newPosts)
        notifyItemRangeInserted(start, newPosts.size)
    }
}
