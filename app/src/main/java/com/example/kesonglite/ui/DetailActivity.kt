package com.example.kesonglite.ui

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
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.kesonglite.R
import com.example.kesonglite.databinding.ActivityDetailBinding
import com.example.kesonglite.data.AppStateMemory
import com.example.kesonglite.model.Post
import com.example.kesonglite.ui.common.SwipeToDismissLayout
import com.example.kesonglite.utils.PersistenceManager
import android.view.ViewGroup
import android.widget.FrameLayout

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
            try {
                // 安全检查：确保Activity还在前台且binding已初始化
                if (isFinishing || isDestroyed) return
                
                // 使用正确的viewPager引用
                val adapter = binding.viewPager.adapter
                if (adapter == null || adapter.itemCount <= 1) {
                    Log.d(TAG, "Auto scroll skipped: adapter not initialized or insufficient items")
                    return
                }
                
                val nextItem = if (binding.viewPager.currentItem == adapter.itemCount - 1) 0 else binding.viewPager.currentItem + 1
            binding.viewPager.setCurrentItem(nextItem, true) // 平滑切换
                
                // 只有在成功执行切换后才提交下一个任务
                if (!isFinishing && !isDestroyed) {
                    autoScrollHandler.postDelayed(this, 3000) // 3秒延迟
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in auto scroll runnable: ${e.message}")
                // 发生异常时确保停止所有可能的回调
                autoScrollHandler.removeCallbacks(this)
            }
        }
    }
    private var isAutoScrollPausedByManualSwipe = false // 手动打断标志
    // 存储页面切换监听器引用，避免重复注册
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    // 存储进度条视图引用，用于优化视图重用
    private val progressBarViews = mutableListOf<View>()
    // 记录转场名称
    private var transitionName: String = "hero_image"
    
    /**
     * 验证Post数据的完整性，确保关键字段都存在且有效
     */
    private fun validatePostData(post: Post): Boolean {
        // 增强的Post数据验证
        try {
            // 检查必要字段是否存在
            if (post.postId.isEmpty()) {
                Log.e(TAG, "Post ID is empty")
                return false
            }
            
            // 检查作者信息是否有效（author字段不可能为null）
            if (post.author.nickname.isNullOrEmpty()) {
                Log.e(TAG, "Author information is missing or invalid")
                return false
            }
            
            // 检查clips列表
            if (post.clips != null && post.clips.isNotEmpty()) {
                // 如果有clips，确保至少有一个有效的clip
                val validClips = post.clips.filter { clip ->
                    !clip.url.isNullOrEmpty()
                }
                // 至少需要有一个有效的clip
                if (validClips.isEmpty()) {
                    Log.e(TAG, "No valid clips found")
                    return false
                }
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating post data: ${e.message}")
            return false
        }
    }
    
    private fun handleInvalidData() {
        // 处理无效或缺失数据的情况，提供用户友好的错误展示
        try {
            // 即使数据无效，也显示基本界面
            setupViewPager() // 设置空的ViewPager，避免崩溃
            
            // 显示错误信息
            Toast.makeText(this, "内容数据异常，无法加载完整内容", Toast.LENGTH_LONG).show()
            
            // 设置错误状态下的UI
            try {
                // 尝试初始化基本UI元素
                binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up error UI: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleInvalidData: ${e.message}")
        }
    }
    
    // 提取媒体播放器释放逻辑为单独方法
    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media player: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 启用 Activity Transition
        window.requestFeature(android.view.Window.FEATURE_CONTENT_TRANSITIONS)
        
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 延迟转场，等待 ViewPager2 内容就绪
        postponeEnterTransition()

        try {
            // 安全获取Intent数据
            try {
                transitionName = intent?.getStringExtra("transition_name") ?: "hero_image_${postId}"
                
                // 安全地获取POST_DATA，增加额外错误捕获
                val postData = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("POST_DATA", Post::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra("POST_DATA")
                }
                
                // 额外验证数据完整性
                if (postData != null && validatePostData(postData)) {
                    post = postData
                    
                    // 提取postId作为全局标识
                    postId = post?.postId ?: ""
                    
                    // 设置转场名称，与FeedAdapter中的保持一致
                    transitionName = "hero_image_${postId}"
                    
                    // 配置转场动画
                    binding.viewPager.transitionName = transitionName
                    
                    // 初始化UI
                    setupUI(post!!)
                    setupMusic(post!!)
                    
                    // 确保clips列表不为空且只在有数据时调用setupVideoPlayback
                    val clips = post?.clips
                    if (clips.isNullOrEmpty()) {
                        Log.e(TAG, "No clips available for this post")
                        // 即使没有clips，也初始化进度条和页面切换逻辑
                        setupViewPager()
                        setupSwipeToDismissLayout()
                        setupBottomBarInteractions(post!!)
                    } else {
                        // 使用setupVideoPlayback替代setupViewPagerScrollListener
                        setupVideoPlayback(clips)
                        
                        // 确保只在初始化完成后才启动自动轮播
                        if (clips.size > 1) {
                            startAutoScroll(clips.size)
                        }
                        
                        setupSwipeToDismissLayout()
                        setupBottomBarInteractions(post!!)
                    }
                    
                    setupMuteButton()
                } else {
                    Log.e(TAG, "Invalid or incomplete post data received")
                    handleInvalidData()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Intent data: ${e.message}")
                handleInvalidData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in onCreate: ${e.message}")
            releaseMediaPlayer()
            Toast.makeText(this, "页面加载失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun setupUI(post: Post) {
        val clips = post.clips ?: emptyList()
        // Author Info
        binding.tvDetailName.text = post.author.nickname
        Glide.with(this).load(post.author.avatar).transform(CircleCrop()).into(binding.ivDetailAvatar)
        
        // Content
        binding.tvDetailTitle.text = post.title
        
        // 调用话题词高亮和点击功能
        // 这里使用模拟数据作为示例，实际项目中应根据post.content解析出真实的话题词位置
        val mockHashtags = mutableListOf<HashtagData>()
        
        // 模拟示例：寻找内容中的'#'标记并创建HashtagData
        val content = post.content ?: ""
        var startIndex = content.indexOf('#')
        while (startIndex != -1 && startIndex < content.length - 1) {
            val nextSpaceIndex = content.indexOf(' ', startIndex)
            val endIndex = if (nextSpaceIndex != -1) nextSpaceIndex else content.length
            if (endIndex > startIndex + 1) { // 确保话题词不为空
                mockHashtags.add(HashtagData(startIndex, endIndex))
            }
            startIndex = content.indexOf('#', endIndex)
        }
        
        setupContentWithHashtags(content, mockHashtags)
        // 使用新的日期格式化方法
        formatCreationDate(post.createTime)
        
        // Follow Button
        updateFollowButton(PersistenceManager.isFollowed(this, post.author.userId))
        binding.btnFollow.setOnClickListener {
            val newState = !PersistenceManager.isFollowed(this, post.author.userId)
            PersistenceManager.setFollowed(this, post.author.userId, newState)
            updateFollowButton(newState)
        }

        // 设置延迟启动转场，确保无论是否有图片都能启动转场
        binding.viewPager.postDelayed({
            startPostponedEnterTransition()
        }, if (post.clips?.isNotEmpty() == true) 300 else 100) // 有图片时给更多加载时间，无图片时快速启动
        
        // Adjust ViewPager Height based on first image aspect ratio
        val firstClip = post.clips?.firstOrNull()
        if (firstClip != null) {
            binding.viewPager.post {
               val width = binding.viewPager.width
               val ratio = firstClip.height.toFloat() / firstClip.width.toFloat()
               val height = (width * ratio).toInt()
               // 宽高比限制在 3:4 ~ 16:9 之间
               val minHeight = (width * 0.75f).toInt() // 4:3
               val maxHeight = (width * 1.777f).toInt() // 16:9
               
               binding.viewPager.layoutParams.height = height.coerceIn(minHeight, maxHeight)
               binding.viewPager.requestLayout()
            }
        }

        // 移除重复的自动轮播启动逻辑，在onCreate中统一处理
        
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        // Handle Mute - 使用新的状态管理
        // 移除重复调用，setupMuteButton只在onCreate开始时调用一次
        
        // 设置进度条
        setupProgressBar(clips.size)
        
        // 移除原有的简化进度条回调，因为setupProgressBar中已经设置了回调
    }

    private fun updateFollowButton(isFollowed: Boolean) {
        binding.btnFollow.text = if (isFollowed) "已关注" else "关注"
        // TODO: 样式调整
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

    // 实现startAutoScroll方法，使用Handler进行自动轮播
    private fun startAutoScroll(clipCount: Int) {
        // 安全检查：确保Activity状态正常
        if (isFinishing || isDestroyed) return
        
        // 只有在有多张图片且未静音时才开始自动轮播
        if (clipCount > 1 && !isMuted) {
            try {
                // 确保binding已初始化
                if (!::binding.isInitialized) {
                    Log.w(TAG, "Cannot start auto scroll: binding not initialized")
                    return
                }
                
                // 移除可能存在的回调，避免重复
                autoScrollHandler.removeCallbacks(autoScrollRunnable)
                
                // 启动自动轮播
                autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
                Log.d(TAG, "Auto scroll started with $clipCount clips")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting auto scroll: ${e.message}")
                // 发生异常时清理Handler
                autoScrollHandler.removeCallbacks(autoScrollRunnable)
            }
        }
    }
    
    // 提取停止自动轮播的逻辑为单独方法，便于多处调用
    private fun stopAutoScroll() {
        // 移除Handler回调
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }
    
    private fun updateMuteIcon() {
        try {
            binding.btnMute.setImageResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating mute icon: ${e.message}")
        }
    }

    // 设置静音按钮
    private fun setupMuteButton() {
        try {
            // 初始化图标状态
            updateMuteIcon()
            
            binding.btnMute.setOnClickListener {
                try {
                    // 切换静音状态
                    isMuted = !isMuted
                    updateMuteIcon()
                    
                    // 同步更新媒体播放器音量
                    mediaPlayer?.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                    
                    // 处理自动轮播状态
                    if (isMuted) {
                        // 静音时停止自动轮播
                        stopAutoScroll()
                        isAutoScrollPausedByManualSwipe = true
                    } else {
                        // 取消静音时，如果之前是手动暂停的，恢复自动轮播
                        if (isAutoScrollPausedByManualSwipe || !AppStateMemory.isMuted(postId)) {
                            val clipCount = binding.viewPager.adapter?.itemCount ?: 0
                            if (clipCount > 1) {
                                startAutoScroll(clipCount)
                                isAutoScrollPausedByManualSwipe = false
                                binding.viewPager.post {
                                    try {
                                        Toast.makeText(this@DetailActivity, "已恢复自动轮播", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        // 忽略Toast异常
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error toggling mute: ${e.message}")
                    Toast.makeText(this, "操作失败，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up mute button: ${e.message}")
        }
    }
    
    private fun stopPlaybackAndAutoScroll() {
        try {
            // 移除自动轮播回调
            stopAutoScroll()
            
            // 暂停背景音乐
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
                mediaPlayer?.setVolume(0f, 0f)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error pausing media player: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback and auto scroll: ${e.message}")
        }
    }
    
    private fun startPlaybackAndAutoScroll() {
        try {
            // 只有在非手动打断状态下才自动轮播 (或手动点击"取消静音"后恢复)
            val adapter = binding.viewPager.adapter
            if (!isAutoScrollPausedByManualSwipe && adapter != null && adapter.itemCount > 1) {
                try {
                    // 移除可能存在的回调，避免重复
                    autoScrollHandler.removeCallbacks(autoScrollRunnable)
                    autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting auto scroll: ${e.message}")
                }
            }
            // 恢复背景音乐
            try {
                mediaPlayer?.setVolume(1f, 1f)
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
    
    // 设置ViewPager页面切换监听，合并处理手动拖动打断轮播和进度条更新
    /**
     * 设置视频播放功能
     */
    private fun setupVideoPlayback(clips: List<com.example.kesonglite.model.Clip>) {
        // 处理完播后自动切换
        val onVideoCompletion = { 
            // 检查是否是最后一个片段
            val nextItem = if (binding.viewPager.currentItem < clips.size - 1) {
                binding.viewPager.currentItem + 1
            } else {
                // 最后一个片段完播，可以停止或回到第一页
                0
            }
            binding.viewPager.setCurrentItem(nextItem, true)
            // 自动切换后，下一个片段的播放会在 onPageSelected 中触发
        }

        // 设置DetailClipAdapter适配器
        val clipAdapter = DetailClipAdapter(clips, onVideoCompletion)
        binding.viewPager.adapter = clipAdapter
        
        // 注册页面切换监听器，处理视频播放控制
        removePageChangeCallback() // 先移除可能存在的监听器
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                // 更新进度条
                updateProgressBar(position, clips.size)
                
                // 停止所有View中的视频
                stopAllVideos()
                
                // 检查当前片段是否为视频
                val currentClip = clips[position]
                if (currentClip.type == 1 && !AppStateMemory.isMuted(postId)) {
                    // 找到当前ViewPager中的VideoView并开始播放
                    startVideoAtPosition(position)
                }
            }
            
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                
                // 处理手动滑动打断自动轮播的逻辑
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        // 手动拖动时暂停自动轮播
                        isAutoScrollPausedByManualSwipe = true
                        stopAutoScroll()
                    }
                    ViewPager2.SCROLL_STATE_IDLE -> {
                        // 停止滑动时，如果不是手动打断的，则重新开始自动轮播
                        if (!isAutoScrollPausedByManualSwipe && clips.size > 1 && !isMuted) {
                            startAutoScroll(clips.size)
                        } else {
                            isAutoScrollPausedByManualSwipe = false // 重置手动打断标志
                        }
                    }
                }
            }
        }
        
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback!!)
    }
    
    /**
     * 停止所有视频播放
     */
    private fun setupViewPager() {
        try {
            // 初始化空ViewPager，避免未初始化导致的崩溃
            val adapter = DetailClipAdapter(emptyList(), {}) // 使用空回调函数
            binding.viewPager.adapter = adapter
            
            // 初始化进度条
            setupProgressBar(1) // 即使没有内容，也设置一个进度条
            
            // 设置基础页面切换监听
            removePageChangeCallback()
            pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    // 对于空内容，position始终为0，无需特殊处理
                    updateProgressBar(0, 1)
                }
            }
            binding.viewPager.registerOnPageChangeCallback(pageChangeCallback!!)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up empty ViewPager: ${e.message}")
        }
    }
    
    private fun stopAllVideos() {
        try {
            // 遍历ViewPager的子View，停止所有视频播放
            val viewPagerLayout = binding.viewPager.getChildAt(0) as? ViewGroup
            if (viewPagerLayout != null) {
                for (i in 0 until viewPagerLayout.childCount) {
                    val child = viewPagerLayout.getChildAt(i)
                    val videoView = (child as? FrameLayout)?.findViewById<android.widget.VideoView>(R.id.videoView)
                    videoView?.apply {
                        if (isPlaying) {
                            pause()
                            seekTo(0)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping all videos: ${e.message}")
        }
    }
    
    /**
     * 从指定位置开始播放视频
     */
    private fun startVideoAtPosition(position: Int) {
        try {
            // 通过标签查找对应位置的视图
            val viewPagerLayout = binding.viewPager.getChildAt(0) as? ViewGroup
            if (viewPagerLayout != null) {
                for (i in 0 until viewPagerLayout.childCount) {
                    val child = viewPagerLayout.getChildAt(i)
                    if (child.tag == position) {
                        val videoView = (child as? FrameLayout)?.findViewById<android.widget.VideoView>(R.id.videoView)
                        videoView?.apply {
                            // 确保视频已准备好
                            if (currentPosition > 0 || isPlaying) {
                                seekTo(0)
                            }
                            start()
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting video at position $position: ${e.message}")
        }
    }
    
    private fun setupViewPagerScrollListener() {
        // 此方法已被setupVideoPlayback中的页面切换监听逻辑替代
        // 保留该方法以保持兼容性，但不执行任何操作
        Log.d(TAG, "setupViewPagerScrollListener called, but functionality is now handled in setupVideoPlayback")
    }

    override fun onPause() {
        super.onPause()
        try {
            // 暂停时保存静音状态到记忆中
            AppStateMemory.setMuted(postId, isMuted)
            
            // 暂停播放和轮播
            stopAutoScroll()
            
            // 暂停媒体播放器但不释放，以便快速恢复
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPause: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // 从记忆中恢复静音状态
            isMuted = AppStateMemory.isMuted(postId)
            updateMuteIcon()
            
            // 如果存在媒体播放器，更新音量
            if (mediaPlayer != null) {
                val volume = if (isMuted) 0f else 1f
                mediaPlayer?.setVolume(volume, volume)
                
                // 如果不是静音且不是手动暂停状态，恢复播放
                if (!isMuted && !isAutoScrollPausedByManualSwipe && !mediaPlayer!!.isPlaying) {
                    try {
                        mediaPlayer?.start()
                        // 同时启动自动轮播
                        val clipCount = binding.viewPager.adapter?.itemCount ?: 0
        if (clipCount > 1) {
                            startAutoScroll(clipCount)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming playback: ${e.message}")
                    }
                }
            } else {
                // 重新初始化媒体播放器（如果有音乐URL）
                post?.music?.url?.let { setupMusic(post!!) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            // 清理资源
            releaseMediaPlayer() // 完全释放媒体播放器资源
            stopAutoScroll() // 移除所有自动轮播回调
            
            // 移除页面变化监听
            removePageChangeCallback()
            
            // 清空缓存的进度条视图引用
            progressBarViews.clear()
            // 清空post引用，帮助垃圾回收
            post = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}")
        } finally {
            // 确保super.onDestroy()总是被调用
            super.onDestroy()
        }
    }

    private fun setupBottomBarInteractions(post: Post) {
        // 从持久化存储获取点赞状态和数量
        val isLiked = PersistenceManager.isLiked(this, post.postId)
        // 如果post.likeCount为null，则从持久化存储获取点赞数
        if (post.likeCount == null) {
            post.likeCount = PersistenceManager.getLikeCount(this, post.postId)
        }
        updateLikeIcon(isLiked)

        // 点赞交互
        binding.ivDetailLike.setOnClickListener {
            // post在这个位置是非空的
            val currentPost = post
            val currentLiked = PersistenceManager.isLiked(this, currentPost.postId)
            val newLiked = !currentLiked
            
            // 更新点赞数量，使用更简洁的方式处理null和负值情况
            if (newLiked != currentLiked) {
                currentPost.likeCount = kotlin.math.max(0, (currentPost.likeCount ?: 0) + if (newLiked) 1 else -1)
                // 持久化存储更新后的点赞数量
                currentPost.likeCount?.let { PersistenceManager.setLikeCount(this, currentPost.postId, it) }
            }
            
            // 持久化点赞状态
            PersistenceManager.setLiked(this, currentPost.postId, newLiked)
            
            // 更新UI
            updateLikeIcon(newLiked)
            
            Toast.makeText(this, "点赞交互已触发", Toast.LENGTH_SHORT).show()
        }

        // 分享交互
        binding.ivDetailShare.setOnClickListener {
            // post在这个位置是非空的
            val currentPost = post
            
            // 创建分享Intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, currentPost.title ?: "分享内容")
                // 构建分享内容
                val shareText = "${currentPost.title ?: ""} - ${currentPost.author.nickname}，快来看看吧！"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            
            // 启动分享选择器
            startActivity(Intent.createChooser(shareIntent, "分享到"))
            
            Toast.makeText(this, "分享交互已触发", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateLikeIcon(isLiked: Boolean) {
        binding.ivDetailLike.setImageResource(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
    }
    

    
    private fun setupProgressBar(clipCount: Int) {
        // 展示规则: 单图不展示，多图展示
        try {
            if (clipCount <= 1) {
                binding.progressIndicatorContainer.visibility = View.GONE
                progressBarViews.clear() // 清除缓存的视图引用
                return
            }

            binding.progressIndicatorContainer.visibility = View.VISIBLE
            
            // 优化视图创建逻辑，重用现有的视图
            optimizeProgressBarViews(clipCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up progress bar: ${e.message}")
            binding.progressIndicatorContainer.visibility = View.GONE
        }

        // 仅更新当前进度条状态，不再注册新的监听器
        // 监听器将统一在setupViewPagerScrollListener中注册
        updateProgressBar(0, clipCount)
    }
    
    // 移除页面切换监听器
    private fun removePageChangeCallback() {
        pageChangeCallback?.let {
            try {
                binding.viewPager.unregisterOnPageChangeCallback(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering page change callback: ${e.message}")
            }
            pageChangeCallback = null
        }
    }
    
    // 优化进度条视图的创建和重用
    private fun optimizeProgressBarViews(clipCount: Int) {
        // 移除所有子视图，但保留对它们的引用
        binding.progressIndicatorContainer.removeAllViews()
        
        // 计算需要新增的视图数量
        val existingCount = progressBarViews.size
        
        // 重用现有视图或创建新视图
        for (i in 0 until clipCount) {
            val progressView = if (i < existingCount) {
                // 重用现有视图
                progressBarViews[i]
            } else {
                // 创建新视图
                View(this).apply {
                    setBackgroundResource(R.drawable.bg_progress_bar)
                    progressBarViews.add(this)
                }
            }
            
            // 设置布局参数
            val params = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                1f // 等分宽度
            ).apply {
                // 添加分段间距
                marginEnd = if (i < clipCount - 1) resources.getDimensionPixelSize(R.dimen.progress_bar_gap) else 0
            }
            
            binding.progressIndicatorContainer.addView(progressView, params)
        }
        
        // 如果现有视图数量多于需要的数量，可以考虑清理多余的视图引用
        // 但为了简单起见，这里保留所有视图引用以便后续重用
    }

    private fun updateProgressBar(currentIndex: Int, totalCount: Int) {
        try {
            for (i in 0 until totalCount) {
                val progressView = binding.progressIndicatorContainer.getChildAt(i)
                if (progressView != null) {
                    // 设置进度颜色: 当前及之前的为实心白 (已读)，之后的为半透明白 (未读)
                    progressView.background.setTint(
                        ContextCompat.getColor(this, if (i <= currentIndex) R.color.white else R.color.white_30_opacity)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating progress bar: ${e.message}")
        }
    }
    
    // 设置 SwipeToDismissLayout 拖动监听器
    private fun setupSwipeToDismissLayout() {
        try {
            binding.swipeDismissLayout.setDragListener(object : SwipeToDismissLayout.OnDragListener {
                override fun onDragStart() {
                    try {
                        // 拖动开始时取消自动轮播和播放
                        stopPlaybackAndAutoScroll()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onDragStart: ${e.message}")
                    }
                }
                
                override fun onDrag(progress: Float) {
                    try {
                        // 拖动过程中，可以根据进度调整其他UI元素
                        Log.d(TAG, "Dragging progress: $progress")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onDrag: ${e.message}")
                    }
                }
                
                override fun onDragEnd(shouldFinish: Boolean) {
                    try {
                        if (shouldFinish) {
                            // 如果需要结束Activity，确保使用supportFinishAfterTransition
                            supportFinishAfterTransition()
                        } else {
                            // 如果不需要结束，恢复播放和轮播（如果当前非静音状态）
                            if (!AppStateMemory.isMuted(postId)) {
                                startPlaybackAndAutoScroll()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onDragEnd: ${e.message}")
                        // 降级处理：直接完成Activity
                        if (shouldFinish) {
                            finish()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up swipe to dismiss layout: ${e.message}")
        }
    }
    
    // 重写 onBackPressed 方法，确保使用转场动画退出
    override fun onBackPressed() {
        try {
            // 确保共享元素退出转场
            binding.viewPager.translationX = 0f
            binding.viewPager.translationY = 0f
            // 优先使用支持转场动画的finish方法
            supportFinishAfterTransition()
            // 使用推荐的OnBackPressedDispatcher替代已弃用的onBackPressed()
            onBackPressedDispatcher.onBackPressed()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onBackPressed: ${e.message}")
            // 降级处理：直接使用普通的finish
            finish()
        }
    }
    
    private fun setupContentWithHashtags(content: String, hashtags: List<HashtagData>) {
        try {
            // 使用binding代替findViewById，避免潜在的空指针异常
            binding.tvDetailContent.text = content

            if (hashtags.isNotEmpty()) {
                val spannableText = SpannableString(content)
                
                for (hashtag in hashtags) {
                    // 1. 高亮展示 (使用 purple_500)
                    spannableText.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(this, R.color.purple_500)),
                        hashtag.start,
                        hashtag.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    
                    // 2. 支持点击
                    spannableText.setSpan(
                        object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                val topic = content.substring(hashtag.start, hashtag.end)
                                Toast.makeText(widget.context, "跳转到话题页面: $topic", Toast.LENGTH_SHORT).show()
                                // TODO: 实现跳转到以"简洁"为基调的页面
                                // val intent = Intent(this@DetailActivity, SimpleTopicActivity::class.java).apply { putExtra("topic", topic) }
                                // startActivity(intent)
                            }
                            override fun updateDrawState(ds: TextPaint) {
                                super.updateDrawState(ds)
                                ds.isUnderlineText = false // 移除下划线
                            }
                        },
                        hashtag.start,
                        hashtag.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                
                binding.tvDetailContent.text = spannableText
                // 必须设置 MovementMethod 才能响应 ClickableSpan
                binding.tvDetailContent.movementMethod = LinkMovementMethod.getInstance()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up hashtag content: ${e.message}")
            // 降级处理：至少显示普通文本内容
            try {
                binding.tvDetailContent.text = content
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to display even plain content: ${e2.message}")
            }
        }
    }
    

    
    // 根据创建时间格式化日期显示
    private fun formatCreationDate(createTimeMillis: Long) {
        try {
            // 使用binding代替findViewById
            val tvDate = binding.tvDate
            val now = System.currentTimeMillis()
            val diff = now - createTimeMillis

            val formattedDate = when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}分钟前"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}小时前"
                diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}天前"
                else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(createTimeMillis))
            }

            tvDate.text = formattedDate
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: ${e.message}")
        }
    }

    // 辅助函数：判断日期是否为今天
    private fun Calendar.isToday(): Boolean {
        val today = Calendar.getInstance()
        return get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && 
               get(Calendar.YEAR) == today.get(Calendar.YEAR)
    }

    // 辅助函数：判断日期是否为昨天
    private fun Calendar.isYesterday(): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) && 
               get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
    }
}

// 用于存储话题词位置信息的数据类
data class HashtagData(val start: Int, val end: Int)
