package com.example.bilidynamic1.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.model.Group

class PopupGroupAdapter(
    private val items: List<Group?>, // null 代表“全部”
    private val currentSelected: Group?,
    private val onSelect: (Group?) -> Unit,
    private val onDelete: (Group) -> Unit,
) : RecyclerView.Adapter<PopupGroupAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val colorDot: View = v.findViewById(R.id.viewColor)
        val name: TextView = v.findViewById(R.id.tvGroupName)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_popup_group, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val group = items[position]

        // 1. 设置 UI 基本内容
        if (group == null) {
            holder.name.text = "全部分动态"
            holder.colorDot.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)
            holder.btnDelete.visibility = View.GONE
        } else {
            holder.name.text = group.name
            holder.colorDot.backgroundTintList = ColorStateList.valueOf(group.color)
            holder.btnDelete.visibility = View.VISIBLE
        }

        // 2. 选中高亮状态
        // 在 onBindViewHolder 内部修改选中状态逻辑
        val isSelected = (group?.id == currentSelected?.id)

        if (isSelected) {
            // 💡 应用刚才创建的圆角背景
            holder.itemView.setBackgroundResource(R.drawable.bg_item_selected)
            holder.name.setTextColor(Color.parseColor("#5856D6")) // 选中文字变蓝
            holder.name.setTypeface(null, android.graphics.Typeface.BOLD) // 选中加粗
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.name.setTextColor(Color.parseColor("#1A1A1A"))
            holder.name.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        // 3. 事件绑定
        holder.itemView.setOnClickListener { onSelect(group) }

        group?.let { g ->
            holder.btnDelete.setOnClickListener { onDelete(g) }
        }
    }

    override fun getItemCount(): Int = items.size
}