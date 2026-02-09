package com.example.bilidynamic1.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.model.UpItem

class FollowingAdapter : RecyclerView.Adapter<FollowingAdapter.VH>() {

    private var list = listOf<UpItem>()

    fun submitList(newList: List<UpItem>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_following_up, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        holder.tvUname.text = item.uname

        Glide.with(holder.imgAvatar.context)
            .load(item.face)
            .circleCrop()
            .placeholder(R.mipmap.ic_launcher_round)
            .into(holder.imgAvatar)
    }

    override fun getItemCount() = list.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imgAvatar: ImageView = v.findViewById(R.id.imgAvatar)
        val tvUname: TextView = v.findViewById(R.id.tvUname)
    }
}