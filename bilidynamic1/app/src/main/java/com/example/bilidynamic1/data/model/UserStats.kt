package com.example.bilidynamic1.data.model

data class UserStats(
    val following: Int = 0,   // 关注数
    val favorites: Int = 0,   // 收藏数 (所有收藏夹内视频总和)
    val watchLater: Int = 0   // 稍后再看数
)