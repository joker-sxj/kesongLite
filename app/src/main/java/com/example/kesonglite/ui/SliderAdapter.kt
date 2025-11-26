package com.example.kesonglite.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kesonglite.databinding.ItemSliderClipBinding
import com.example.kesonglite.model.Clip

class SliderAdapter(private val clips: List<Clip>) :
    RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

    inner class SliderViewHolder(val binding: ItemSliderClipBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val binding = ItemSliderClipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SliderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val clip = clips[position]
        
        // 假设 ItemSliderClipBinding 包含一个 ImageView: ivClip
        // 假设 ItemSliderClipBinding 包含一个 VideoView: videoView
        
        if (clip.type == 0) { // Image
            holder.binding.ivClip.visibility = android.view.View.VISIBLE
            holder.binding.videoView.visibility = android.view.View.GONE
            Glide.with(holder.itemView.context).load(clip.url).into(holder.binding.ivClip)
        } else { // Video
            holder.binding.ivClip.visibility = android.view.View.GONE
            holder.binding.videoView.visibility = android.view.View.VISIBLE
            
            // 视频播放逻辑简化处理，仅设置路径
            holder.binding.videoView.setVideoPath(clip.url)
            holder.binding.videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.start()
            }
            // 实际项目中需要更复杂的生命周期管理和播控
        }
    }

    override fun getItemCount() = clips.size
}
