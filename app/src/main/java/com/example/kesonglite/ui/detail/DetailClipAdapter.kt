package com.example.kesonglite.ui.detail

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kesonglite.databinding.ItemSliderClipBinding
import com.example.kesonglite.domain.model.Clip

/**
 * 详情页视频片段适配器
 * 用于处理混合数据类型（图片和视频）并在视频片段可见时播放，完播时自动切换
 */
class DetailClipAdapter(
    private val clips: List<Clip>,
    private val onVideoCompletion: () -> Unit = {} // 视频完播回调，提供默认实现
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
            val binding = holder.binding

            // 根据clip类型设置UI
            if (clip.type == 1) { // 视频
                binding.videoView.visibility = View.VISIBLE

                // 设置视频URI
                val videoUri = Uri.parse(clip.url)
                binding.videoView.setVideoURI(videoUri)

                // 设置视频完成监听
                binding.videoView.setOnCompletionListener {
                    Log.d(TAG, "Video completed at position $position")
                    // 视频播放完成后自动切换到下一页
                    if (position < clips.size - 1) {
                        // 这里可以通知DetailActivity滚动到下一页
                        onVideoCompletion.invoke()
                    }
                    // 循环播放
                    it.start()
                }

                // 设置错误监听
                binding.videoView.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Video playback error: what=$what, extra=$extra")
                    // 隐藏出错的视频视图
                    binding.videoView.visibility = View.GONE
                    return@setOnErrorListener true
                }

            } else { // 图片或其他类型 - 简化处理，暂时隐藏
                binding.videoView.visibility = View.GONE
                // 注意：这里原本有图片加载逻辑，但由于imageView不存在，暂时省略
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error binding view holder: ${e.message}")
        }
    }

    override fun getItemCount(): Int {
        return clips.size
    }

    override fun onViewAttachedToWindow(holder: ClipViewHolder) {
        super.onViewAttachedToWindow(holder)
        // 当视图可见时，开始播放视频
        if (holder.binding.videoView.visibility == View.VISIBLE) {
            holder.binding.videoView.start()
        }
    }

    override fun onViewDetachedFromWindow(holder: ClipViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // 当视图不可见时，停止播放视频
        if (holder.binding.videoView.isPlaying) {
            holder.binding.videoView.stopPlayback()
        }
    }

    fun stopAllVideos() {
        // 这里可以停止所有视频播放
        // 在实际实现中，可能需要保持对所有ViewHolder的引用
        Log.d(TAG, "Stopping all videos")
    }
}