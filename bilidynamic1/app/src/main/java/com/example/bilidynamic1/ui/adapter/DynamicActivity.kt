package com.example.bilydynamic1.ui.adapter

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.model.DynamicItem

// 1️⃣ 构造函数增加 onItemClick 回调
class DynamicAdapter(
    private val onItemClick: (DynamicItem) -> Unit
) : ListAdapter<DynamicItem, DynamicAdapter.VH>(Diff()) {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.imgAvatar)
        val cover: ImageView = view.findViewById(R.id.imgCover)
        val title: TextView = view.findViewById(R.id.txtTitle)
        val uname: TextView = view.findViewById(R.id.txtUname)
        val time: TextView = view.findViewById(R.id.txtTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dynamic, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.title.text = item.title
        holder.uname.text = item.uname
        holder.time.text = DateFormat.format(
            "yyyy-MM-dd HH:mm",
            item.pubTime * 1000
        )

        Glide.with(holder.avatar)
            .load(item.avatar)
            .circleCrop()
            .into(holder.avatar)

        Glide.with(holder.cover)
            .load(item.cover)
            .into(holder.cover)

        // 2️⃣ 设置点击事件，将当前 item 回调出去
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    class Diff : DiffUtil.ItemCallback<DynamicItem>() {
        // 注意：这里建议用 unique ID (如 bvid 或 dynamic_id) 比较，不要只用 uid (用户ID)
        // 假设 DynamicItem 的 uid 在这里是指动态的唯一ID
        override fun areItemsTheSame(a: DynamicItem, b: DynamicItem) = a.bvid == b.bvid
        override fun areContentsTheSame(a: DynamicItem, b: DynamicItem) = a == b
    }
}