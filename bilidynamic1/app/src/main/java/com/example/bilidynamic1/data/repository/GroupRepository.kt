package com.example.bilidynamic1.data.repository

import android.content.Context
import com.example.bilidynamic1.data.database.AppDatabase
import com.example.bilidynamic1.data.model.Group
import kotlinx.coroutines.flow.Flow

class GroupRepository(context: Context) {
    private val groupDao = AppDatabase.getInstance(context).groupDao()
    // 在 GroupRepository 类中增加
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun setGlobalFilterMode(isWhitelist: Boolean) {
        prefs.edit().putBoolean("global_filter_is_whitelist", isWhitelist).apply()
    }

    fun isGlobalWhitelist(): Boolean {
        // 默认设为白名单模式
        return prefs.getBoolean("global_filter_is_whitelist", true)
    }

    // 暴露给 ViewModel 观察
    fun getAllGroups(): Flow<List<Group>> = groupDao.getAllGroupsFlow()

    suspend fun createGroup(name: String, type: Group.FilterType = Group.FilterType.WHITELIST) {
        val colors = listOf(0xFF66CCFF, 0xFFFFAFC9, 0xFF80FFA5, 0xFFFFD8A8)
        val group = Group(name = name, type = type, color = colors.random().toInt())
        groupDao.insert(group)
    }

    suspend fun updateGroup(group: Group) = groupDao.update(group)

    suspend fun deleteGroup(group: Group) = groupDao.delete(group)

    suspend fun getGroupById(id: Long) = groupDao.getGroupById(id)

    // 向分组添加/移除 UP 主的便捷方法
    suspend fun toggleUpInGroup(groupId: Long, upId: Long, shouldAdd: Boolean) {
        val group = groupDao.getGroupById(groupId) ?: return
        val newUpIds = group.upIds.toMutableSet().apply {
            if (shouldAdd) add(upId) else remove(upId)
        }
        groupDao.update(group.copy(upIds = newUpIds))
    }
}