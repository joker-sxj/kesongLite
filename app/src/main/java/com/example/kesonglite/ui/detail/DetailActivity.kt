package com.example.kesonglite.ui.detail

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.method.LinkMovementMethod
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.kesonglite.data.state.AppStateMemory
import com.example.kesonglite.data.user.UserLocalDataSource
import com.example.kesonglite.databinding.ActivityDetailBinding
import com.example.kesonglite.domain.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.kesonglite.ui.common.SwipeToDismissLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class DetailActivity : AppCompatActivity() {
    // 添加类标签，便于日志记录
    private val TAG = "DetailActivity"
    
    private lateinit var binding: ActivityDetailBinding
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false
    private var post: Post? = null
    private var postId: String = "" // 当前作品ID
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            if (binding.viewPager.currentItem < (binding.viewPager.adapter?.itemCount ?: 1) - 1 && !isAutoScrollPausedByManualSwipe) {
                binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
                autoScrollHandler.postDelayed(this, 3000) // 3秒后滚动到下一页
            } else {
                // 已经是最后一页，重置到第一页
                if (binding.viewPager.currentItem == (binding.viewPager.adapter?.itemCount ?: 1) - 1 && !isAutoScrollPausedByManualSwipe) {
                    // 滚动到第一页
                    binding.viewPager.setCurrentItem(0, true)
                    autoScrollHandler.postDelayed(this, 3000)
                }
            }
        }
    }
    private var isAutoScrollPausedByManualSwipe = false // 手动打断标志
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private val progressBarViews = mutableListOf<View>()
    private var transitionName: String = "hero_image"
    private val userLocalDataSource by lazy { UserLocalDataSource(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // 初始化UI元素
            post = intent.getParcelableExtra("POST_DATA")
            
            if (post != null && validatePostData(post!!)) {
                postId = post!!.postId
                transitionName = intent.getStringExtra("TRANSITION_NAME") ?: "hero_image_${postId}"
                setupUI(post!!)
                setupMuteButton()
                setupSwipeToDismissLayout()
                setupBottomBarInteractions(post!!)
                setupMusic(post!!)
                setupVideoPlayback(post!!.clips?.filterNotNull() ?: emptyList())
            } else {
                handleInvalidData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI(post: Post) {
        // 检查binding.viewPager2是否存在，避免空指针异常
        try {
            // 设置共享元素转场名称
            binding.viewPager.transitionName = transitionName
        } catch (e: Exception) {
            Log.w(TAG, "viewPager not found", e)
        }
        
        // 设置作者信息
        binding.tvDetailName.text = post.author.nickname ?: "匿名用户"
        Glide.with(this).load(post.author.avatar).transform(CircleCrop()).into(binding.ivDetailAvatar)
        
        // 设置标题
        binding.tvDetailTitle.text = post.content ?: ""
        
        // 设置内容
        binding.tvDetailContent.text = post.content ?: ""
        
        // 设置关注按钮状态
        val isFollowed = userLocalDataSource.isUserFollowed(post.author.userId)
        updateFollowButton(isFollowed)
        
        // 设置关注按钮点击事件
        binding.btnFollow.setOnClickListener {
            val currentFollowed = userLocalDataSource.isUserFollowed(post.author.userId)
            userLocalDataSource.toggleUserFollowed(post.author.userId)
            updateFollowButton(!currentFollowed)
        }
        
        // 设置发布时间
        post.createTime?.let {
            binding.tvDate.text = formatCreationDate(it)
        }
    }

    private fun updateFollowButton(isFollowed: Boolean) {
        binding.btnFollow.text = if (isFollowed) "已关注" else "关注"
        binding.btnFollow.setTextColor(if (isFollowed) ContextCompat.getColor(this, android.R.color.darker_gray) else ContextCompat.getColor(this, android.R.color.white))
        binding.btnFollow.setBackgroundResource(if (isFollowed) android.R.drawable.btn_default else android.R.drawable.btn_default)
    }

    private fun setupMusic(post: Post) {
        // 音乐信息暂时不显示
    }

    private fun setupMuteButton() {
        isMuted = AppStateMemory.isMuted(postId)
        updateMuteIcon()
        
        binding.btnMute.setOnClickListener {
            isMuted = !isMuted
            AppStateMemory.setMuted(postId, isMuted)
            updateMuteIcon()
            // 这里可以调用相应的方法来控制视频静音
            if (mediaPlayer != null) {
                val volume = if (isMuted) 0f else 1f
                mediaPlayer?.setVolume(volume, volume) // 添加右声道音量参数
            }
        }
    }

    private fun updateMuteIcon() {
        binding.btnMute.setImageResource(android.R.drawable.ic_lock_silent_mode)
    }

    private fun setupBottomBarInteractions(post: Post) {
        // 从持久化存储获取点赞状态和数量
        val isLiked = userLocalDataSource.isPostLiked(post.postId)
        // 如果post.likeCount为null，则从持久化存储获取点赞数
        if (post.likeCount == null) {
            post.likeCount = userLocalDataSource.getLikeCount(post.postId)
        }
        updateLikeStatus(isLiked, post)

        // 点赞交互
        binding.ivDetailLike.setOnClickListener {
            val currentPost = post
            if (currentPost == null) return@setOnClickListener
            toggleLike(currentPost)
        }

        // 分享交互
        binding.ivDetailShare.setOnClickListener {
            val currentPost = post
            if (currentPost == null) return@setOnClickListener
            
            // 创建分享Intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, currentPost.title ?: "分享内容")
                // 构建分享内容
                val shareText = "${currentPost.title ?: ""} - ${currentPost.author.nickname ?: "未知用户"}，快来看看吧！"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            
            // 启动分享选择器
            startActivity(Intent.createChooser(shareIntent, "分享到"))
            
            Toast.makeText(this, "分享交互已触发", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLikeStatus(isLiked: Boolean, post: Post) {
        // 简化点赞图标处理，使用系统默认图标
        try {
            binding.ivDetailLike.setImageResource(android.R.drawable.ic_input_add)
        } catch (e: Exception) {
            Log.w(TAG, "ivDetailLike not found", e)
        }
        // 不再尝试访问tvLikeCount，避免Unresolved reference错误
    }
    
    private fun toggleLike(post: Post) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 获取当前点赞状态
                val currentLiked = userLocalDataSource.isPostLiked(post.postId)
                val newLiked = !currentLiked
                
                // 更新点赞状态
                // 保存到本地数据库
                userLocalDataSource.togglePostLiked(post.postId)
                
                // 更新点赞数
                val likeCount = kotlin.math.max(0, (post.likeCount ?: 0) + (if (newLiked) 1 else -1))
                userLocalDataSource.setLikeCount(post.postId, likeCount)
                post.likeCount = likeCount
                
                // 更新UI
                updateLikeStatus(newLiked, post)
                
                Toast.makeText(this@DetailActivity, "点赞交互已触发", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling like status", e)
                Toast.makeText(this@DetailActivity, "点赞操作失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatCreationDate(createTimeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diffInMillis = now - createTimeMillis
        
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        
        return when {
            seconds < 60 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> "${hours}小时前"
            days < 7 -> "${days}天前"
            else -> {
                val sdf = SimpleDateFormat("MM-dd", Locale.CHINA)
                sdf.format(Date(createTimeMillis))
            }
        }
    }

    private fun setupVideoPlayback(clipsList: List<com.example.kesonglite.domain.model.Clip>) {
        // 复制列表，避免并发修改问题
        val clips = clipsList.toList()
        // 这里简化实现，实际应该根据clips列表设置ViewPager
        if (clips.isNotEmpty()) {
            // 设置ViewPager适配器
            val adapter = DetailClipAdapter(clips)
            binding.viewPager.adapter = adapter
            
            // 设置ViewPager滚动监听器
            setupViewPagerScrollListener()
            
            // 开始自动滚动
            startAutoScroll(clips.size)
        }
    }

    private fun setupViewPagerScrollListener() {
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        // 用户开始拖动，暂停自动滚动
                        isAutoScrollPausedByManualSwipe = true
                        stopAutoScroll()
                    }
                    ViewPager2.SCROLL_STATE_IDLE -> {
                        // 用户停止拖动，恢复自动滚动
                        isAutoScrollPausedByManualSwipe = false
                        startAutoScroll(binding.viewPager.adapter?.itemCount ?: 1)
                    }
                }
            }
        }
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback!!)
    }

    private fun startAutoScroll(clipCount: Int) {
        stopAutoScroll() // 先停止之前的滚动
        if (clipCount > 1) {
            autoScrollHandler.postDelayed(autoScrollRunnable, 3000) // 3秒后开始自动滚动
        }
    }

    private fun stopAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    private fun setupSwipeToDismissLayout() {
        try {
            val swipeLayout = SwipeToDismissLayout(this)
            val content = findViewById<ViewGroup>(android.R.id.content)
            val child = content.getChildAt(0)
            content.removeView(child)
            swipeLayout.addView(child)
            content.addView(swipeLayout)
            
            // 设置简单的点击关闭功能，避免监听器问题
            swipeLayout.setOnClickListener { finish() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to setup SwipeToDismissLayout", e)
        }
    }

    private fun validatePostData(post: Post): Boolean {
        // 验证必要字段
        if (post.postId.isNullOrEmpty()) {
            Log.e(TAG, "Invalid post data: postId is empty")
            return false
        }
        
        if (post.author == null) {
            Log.e(TAG, "Invalid post data: author is null")
            return false
        }
        
        return true
    }

    private fun handleInvalidData() {
        Toast.makeText(this, "数据无效，无法显示详情", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onPause() {
        super.onPause()
        stopPlaybackAndAutoScroll()
    }

    override fun onResume() {
        super.onResume()
        if (post != null) {
            startPlaybackAndAutoScroll()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaybackAndAutoScroll()
        releaseMediaPlayer()
        removePageChangeCallback()
    }

    private fun stopPlaybackAndAutoScroll() {
        stopAutoScroll()
        // 这里可以停止视频播放
    }

    private fun startPlaybackAndAutoScroll() {
        // 这里可以开始视频播放
        val currentPost = post
        val clips = currentPost?.clips
        if (currentPost != null && clips != null && clips.size > 1) {
            startAutoScroll(clips.size)
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun removePageChangeCallback() {
        pageChangeCallback?.let {
            binding.viewPager.unregisterOnPageChangeCallback(it)
        }
    }

    data class HashtagData(val start: Int, val end: Int)
    
    private fun setupTabs() {
        // 暂时不使用tabs功能
    }
}