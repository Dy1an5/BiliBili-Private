package com.example.bilidynamic1.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.model.DynamicItem

class WatchLaterAdapter(
    private val onItemClick: (DynamicItem) -> Unit
) : RecyclerView.Adapter<WatchLaterAdapter.VH>() {

    private var items = listOf<DynamicItem>()

    fun submitList(newList: List<DynamicItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_small_video, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // 加载视频封面
        Glide.with(holder.cover).load(item.cover).into(holder.cover)

        // 加载UP主头像
        Glide.with(holder.upAvatar)
            .load(item.avatar) // 这里 item.avatar 必须是 face 链接
            .circleCrop()
            .placeholder(R.mipmap.ic_launcher_round) // 增加一个占位图看是否加载中
            .into(holder.upAvatar)

        holder.title.text = item.title
        holder.upName.text = item.uname

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cover: ImageView = v.findViewById(R.id.imgCover)
        val title: TextView = v.findViewById(R.id.tvTitle)
        val upName: TextView = v.findViewById(R.id.tvUpName)
        val upAvatar: ImageView = v.findViewById(R.id.imgUpAvatar)
    }
}