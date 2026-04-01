package com.example.bilidynamic1.data.database

import androidx.room.*
import com.example.bilidynamic1.data.model.Group
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    // 使用 Flow 监听所有分组变化，用于底部的滑动列表
    @Query("SELECT * FROM `groups` ORDER BY id ASC")
    fun getAllGroupsFlow(): Flow<List<Group>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: Group): Long

    @Update
    suspend fun update(group: Group)

    @Delete
    suspend fun delete(group: Group)

    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): Group?
}