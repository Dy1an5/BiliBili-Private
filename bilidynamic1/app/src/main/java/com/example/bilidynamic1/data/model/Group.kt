package com.example.bilidynamic1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: FilterType = FilterType.WHITELIST, // 默认白名单
    val upIds: Set<Long> = emptySet(),           // 存储 UP 主的 UID 集合
    val color: Int = 0xFF66CCFF.toInt()          // 圆形图标颜色 (ARGB)
) {
    enum class FilterType {
        WHITELIST, // 白名单模式
        BLACKLIST  // 黑名单模式
    }
}