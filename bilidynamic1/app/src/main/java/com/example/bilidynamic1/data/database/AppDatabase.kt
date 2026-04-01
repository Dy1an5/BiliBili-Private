package com.example.bilidynamic1.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.bilidynamic1.data.model.Group

@Database(entities = [Group::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dynamic_groups_db"
                )
                    .fallbackToDestructiveMigration() // 开发阶段如果改了字段，这行会重置数据库避免崩溃
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}