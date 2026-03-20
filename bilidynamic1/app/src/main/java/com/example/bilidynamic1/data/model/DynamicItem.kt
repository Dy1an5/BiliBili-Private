package com.example.bilidynamic1.data.model

data class DynamicItem(
    val uid: Long,
    val uname: String,
    val avatar: String,
    val pubTime: Long,
    val title: String,
    val cover: String,
    val bvid: String,        // 视频BV号
    val aid: Long,           // 视频AV号
    val cid: Long            // 分P ID
)