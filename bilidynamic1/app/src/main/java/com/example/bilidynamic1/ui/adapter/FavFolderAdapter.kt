package com.example.bilidynamic1.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.model.FavFolder

class FavFolderAdapter(
    private val onClick: (FavFolder) -> Unit
) : RecyclerView.Adapter<FavFolderAdapter.VH>() {

    private var list = listOf<FavFolder>()

    fun submitList(newList: List<FavFolder>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // 确保这里引用的是你那个只有文字的正方形布局
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fav_folder_grid, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]

        // 设置文字内容
        holder.tvName.text = item.title
        holder.tvCount.text = item.mediaCount.toString()

        // 点击事件
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = list.size

    // ⭐️ 重点修复：VH 内部只能包含 XML 中确实存在的 ID
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        // 删掉了 imgCover，只保留这两个
        val tvName: TextView = v.findViewById(R.id.tvFolderName)
        val tvCount: TextView = v.findViewById(R.id.tvCount)
    }
}