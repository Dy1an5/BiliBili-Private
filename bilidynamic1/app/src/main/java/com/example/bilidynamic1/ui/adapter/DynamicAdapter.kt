package com.example.bilidynamic1.ui.adapter

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

class DynamicAdapter(
    private val onItemClick: (DynamicItem) -> Unit,
    private val onItemLongClick: (DynamicItem, android.view.View) -> Unit
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

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(item, it)
            true
        }
    }



    class Diff : DiffUtil.ItemCallback<DynamicItem>() {
        override fun areItemsTheSame(a: DynamicItem, b: DynamicItem) = a.bvid == b.bvid
        override fun areContentsTheSame(a: DynamicItem, b: DynamicItem) = a == b
    }
}