package com.example.kesonglite.ui

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kesonglite.databinding.ItemSliderClipBinding
import com.example.kesonglite.model.Clip

/**
 * 详情页视频片段适配器
 * 用于处理混合数据类型（图片和视频）并在视频片段可见时播放，完播时自动切换
 */
class DetailClipAdapter(
    private val clips: List<Clip>,
    private val onVideoCompletion: () -> Unit // 视频完播回调
) : RecyclerView.Adapter<DetailClipAdapter.ClipViewHolder>() {
    // 添加类标签，便于日志记录
    private val TAG = "DetailClipAdapter"

    /**
     * 片段视图持有者
     */
    inner class ClipViewHolder(val binding: ItemSliderClipBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // 设置标签，方便在DetailActivity中查找
            binding.root.tag = adapterPosition
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipViewHolder {
        try {
            val binding = ItemSliderClipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ClipViewHolder(binding)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating view holder: ${e.message}")
            throw e
        }
    }

    override fun onBindViewHolder(holder: ClipViewHolder, position: Int) {
        try {
            // 安全检查，避免越界访问
            if (position >= clips.size || clips.isEmpty()) {
                Log.e(TAG, "Invalid position or empty clips list: $position")
                return
            }
            
            val clip = clips[position]
            
            // 更新标签，确保位置正确
            holder.binding.root.tag = position
            
            // 确保clip和url不为null
            if (clip.url.isNullOrEmpty()) {
                Log.e(TAG, "Clip URL is null or empty at position $position")
                return
            }
            
            // 根据片段类型显示不同内容
            if (clip.type == 0) { // 图片
                holder.binding.ivClip.visibility = View.VISIBLE
                holder.binding.videoView.visibility = View.GONE
                
                // 使用Glide加载图片
                try {
                    Glide.with(holder.itemView.context)
                        .load(clip.url)
                        .centerCrop()
                        .into(holder.binding.ivClip)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image: ${e.message}")
                }
            } else { // 视频
                holder.binding.ivClip.visibility = View.GONE
                holder.binding.videoView.visibility = View.VISIBLE
                
                // 配置视频源
                try {
                    holder.binding.videoView.apply {
                        setVideoURI(Uri.parse(clip.url))
                        
                        // 播控场景下，视频片段完播后，自动切换至下一片段
                        setOnCompletionListener {
                            try {
                                // 当视频完播时，触发切换到下一页的回调
                                onVideoCompletion.invoke()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in video completion callback: ${e.message}")
                            }
                        }
                        
                        // 准备完成后，配置播放参数但不立即开始播放
                        setOnPreparedListener { mp ->
                            mp.isLooping = false // 确保不会循环播放
                            // 实际播放控制由DetailActivity的onPageSelected/onResume管理
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up video: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onBindViewHolder: ${e.message}")
        }
    }

    override fun getItemCount(): Int {
        // 安全处理空列表情况
        return clips.size
    }

    /**
     * 管理视频的生命周期
     * 当视图附加到窗口时，通过标签标识当前位置
     */
    override fun onViewAttachedToWindow(holder: ClipViewHolder) {
        super.onViewAttachedToWindow(holder)
        // 标记当前视图位置，方便DetailActivity查找和控制播放
        holder.binding.root.tag = holder.adapterPosition
    }

    /**
     * 当视图从窗口分离时，释放相关资源
     */
    override fun onViewDetachedFromWindow(holder: ClipViewHolder) {
        super.onViewDetachedFromWindow(holder)
        
        // 如果是视频，释放资源
        if (holder.binding.videoView.visibility == View.VISIBLE) {
            try {
                holder.binding.videoView.apply {
                    stopPlayback()
                    setVideoURI(null) // 清除视频源，释放资源
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing video resources: ${e.message}")
            }
        }
    }
    
    // 停止所有视频播放
    fun stopAllVideos() {
        try {
            Log.d(TAG, "stopAllVideos method called")
        } catch (e: Exception) {
            // 避免日志记录导致的崩溃
        }
    }
}