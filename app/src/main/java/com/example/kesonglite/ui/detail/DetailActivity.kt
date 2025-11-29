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
import com.example.kesonglite.R

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
        
        // 设置自定义转场动画
        window.sharedElementEnterTransition = android.transition.TransitionInflater.from(this)
            .inflateTransition(R.transition.hero_image_transition)
        window.sharedElementReturnTransition = android.transition.TransitionInflater.from(this)
            .inflateTransition(R.transition.hero_image_reverse_transition)
        
        // 设置其他元素的淡入淡出动画
        window.enterTransition = android.transition.Fade().apply {
            duration = 300
        }
        window.exitTransition = android.transition.Fade().apply {
            duration = 300
        }
        
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
        // 安全获取音乐URL，如果不存在则直接返回
        val musicUrl = post.music?.url ?: return
        
        // 优化：如果mediaPlayer已存在且正在播放相同的音乐，则不需要重新初始化
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            // 只需确保音量状态正确即可
            mediaPlayer?.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
            return
        }
        
        // 确保在设置新的音乐源之前释放已存在的MediaPlayer实例
        releaseMediaPlayer()
        
        // 创建新的MediaPlayer实例
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(musicUrl)
                prepareAsync()
                setOnPreparedListener { 
                    isLooping = true // 设置为循环播放
                    if (isMuted) setVolume(0f, 0f) else setVolume(1f, 1f)
                    // 检查活动状态，避免在不活跃状态下开始播放
                    if (!isFinishing && !isDestroyed) {
                        start()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    true // 返回true表示我们已经处理了错误
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up MediaPlayer: ${e.message}")
                releaseMediaPlayer() // 发生错误时释放资源
            }
        }
    }

    private fun setupMuteButton() {
        isMuted = AppStateMemory.isMuted(postId)
        updateMuteIcon()
        
        binding.btnMute.setOnClickListener {
            isMuted = !isMuted
            AppStateMemory.setMuted(postId, isMuted)
            updateMuteIcon()
            
            // 控制视频静音
            if (mediaPlayer != null) {
                val volume = if (isMuted) 0f else 1f
                mediaPlayer?.setVolume(volume, volume) // 添加右声道音量参数
            }
            
            // 控制自动轮播
            if (isMuted) {
                stopAutoScroll() // 静音时停止自动轮播
            } else {
                // 取消静音时恢复自动轮播
                val clipCount = binding.viewPager.adapter?.itemCount ?: 0
                if (clipCount > 1 && !isAutoScrollPausedByManualSwipe) {
                    startAutoScroll(clipCount)
                }
            }
        }
    }

    private fun updateMuteIcon() {
        binding.btnMute.setImageResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
    }

    private fun setupBottomBarInteractions(post: Post) {
        // 从持久化存储获取点赞状态和数量
        val isLiked = userLocalDataSource.isPostLiked(post.postId)
        // 如果post.likeCount为null，则从持久化存储获取点赞数
        if (post.likeCount == null) {
            post.likeCount = userLocalDataSource.getLikeCount(post.postId)
        }
        updateLikeStatus(isLiked, post)
        
        // 从持久化存储获取收藏状态和数量
        val isFavorited = userLocalDataSource.isPostFavorited(post.postId)
        // 如果post.favoriteCount为null，则从持久化存储获取收藏数
        if (post.favoriteCount == null) {
            post.favoriteCount = userLocalDataSource.getFavoriteCount(post.postId)
        }
        updateFavoriteIcon(isFavorited)

        // 点赞交互
        binding.ivDetailLikeBottom.setOnClickListener {
            val currentPost = post
            if (currentPost == null) return@setOnClickListener
            toggleLike(currentPost)
        }
        
        // 收藏交互
        binding.ivDetailFavoriteBottom.setOnClickListener {
            val currentPost = post
            if (currentPost == null) return@setOnClickListener
            toggleFavorite(currentPost)
        }

        // 分享交互
        binding.ivDetailShareBottom.setOnClickListener {
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
            binding.ivDetailLikeBottom.setImageResource(android.R.drawable.ic_input_add)
        } catch (e: Exception) {
            Log.w(TAG, "ivDetailLike not found", e)
        }
        // 不再尝试访问tvLikeCount，避免Unresolved reference错误
    }
    
    private fun updateFavoriteIcon(isFavorited: Boolean) {
        // 更新收藏图标，使用星星图标
        try {
            binding.ivDetailFavoriteBottom.setImageResource(
                if (isFavorited) android.R.drawable.star_on else android.R.drawable.star_off
            )
        } catch (e: Exception) {
            Log.w(TAG, "ivDetailFavoriteBottom not found", e)
        }
    }
    
    private fun toggleFavorite(post: Post) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 获取当前收藏状态
                val currentFavorited = userLocalDataSource.isPostFavorited(post.postId)
                val newFavorited = !currentFavorited
                
                // 更新收藏状态
                userLocalDataSource.togglePostFavorited(post.postId)
                
                // 更新收藏数
                val favoriteCount = kotlin.math.max(0, (post.favoriteCount ?: 0) + (if (newFavorited) 1 else -1))
                userLocalDataSource.setFavoriteCount(post.postId, favoriteCount)
                post.favoriteCount = favoriteCount
                
                // 更新UI
                updateFavoriteIcon(newFavorited)
                
                Toast.makeText(this@DetailActivity, "收藏交互已触发", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite status", e)
                Toast.makeText(this@DetailActivity, "收藏操作失败", Toast.LENGTH_SHORT).show()
            }
        }
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
            // 设置ViewPager适配器，添加视频完播回调
            val adapter = DetailClipAdapter(clips) {
                // 视频完播后，自动切换到下一片段
                val currentPosition = binding.viewPager.currentItem
                if (currentPosition < clips.size - 1) {
                    binding.viewPager.setCurrentItem(currentPosition + 1, true)
                } else {
                    // 循环播放，回到第一片
                    binding.viewPager.setCurrentItem(0, true)
                }
            }
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
        // 只有在非静音状态下才启动自动轮播
        if (clipCount > 1 && !isMuted) {
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
            
            // 创建背景蒙层视图
            val backgroundView = View(this)
            backgroundView.setBackgroundColor(android.graphics.Color.BLACK)
            backgroundView.alpha = 0f
            
            // 添加背景和内容到swipeLayout
            swipeLayout.addView(backgroundView)
            swipeLayout.addView(child)
            content.addView(swipeLayout)
            
            // 设置swipeLayout的背景视图
            swipeLayout.setDragListener(object : SwipeToDismissLayout.OnDragListener {
                override fun onDragStart() {
                    // 拖动开始时，停止视频播放和自动滚动
                    stopPlaybackAndAutoScroll()
                }
                
                override fun onDrag(progress: Float) {
                    // 拖动过程中，可以根据进度调整其他UI元素
                    Log.d(TAG, "Dragging progress: $progress")
                    
                    // 调整其他元素的透明度
                    binding.tvDetailName.alpha = 1 - progress
                    binding.tvDetailTitle.alpha = 1 - progress
                    binding.tvDetailContent.alpha = 1 - progress
                    binding.btnBack.alpha = 1 - progress
                    binding.btnMute.alpha = 1 - progress
                    binding.bottomBar.alpha = 1 - progress
                }
                
                override fun onDragEnd(shouldFinish: Boolean) {
                    try {
                        if (shouldFinish) {
                            // 如果需要结束Activity，确保使用supportFinishAfterTransition
                            supportFinishAfterTransition()
                        } else {
                            // 如果不需要结束，恢复播放和轮播（如果当前非静音状态）
                            if (!isMuted) {
                                startPlaybackAndAutoScroll()
                            }
                            // 恢复UI元素透明度
                            binding.tvDetailName.alpha = 1f
                            binding.tvDetailTitle.alpha = 1f
                            binding.tvDetailContent.alpha = 1f
                            binding.btnBack.alpha = 1f
                            binding.btnMute.alpha = 1f
                            binding.bottomBar.alpha = 1f
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onDragEnd: ${e.message}", e)
                        // 降级处理：直接完成Activity
                        if (shouldFinish) {
                            finish()
                        }
                    }
                }
            })
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
        // 停止视频播放和音乐
        if (mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }
    }

    private fun startPlaybackAndAutoScroll() {
        try {
            // 恢复视频播放和音乐（根据静音状态）
            val currentPost = post
            val clips = currentPost?.clips
            if (currentPost != null && clips != null && clips.size > 1) {
                startAutoScroll(clips.size)
            }
            
            // 根据静音状态控制音乐播放
            try {
                mediaPlayer?.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                if (mediaPlayer?.isPlaying == false && !isFinishing && !isDestroyed) {
                    mediaPlayer?.start()
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error starting media player: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback and auto scroll: ${e.message}")
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