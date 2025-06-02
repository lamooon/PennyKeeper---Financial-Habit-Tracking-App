package com.example.pennykeeper.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pennykeeper.data.dao.SettingsDao
import com.example.pennykeeper.data.model.Budget

@Database(
    entities = [Budget::class],
    version = 1,
    exportSchema = false
)
abstract class SettingsDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: SettingsDatabase? = null

        fun getDatabase(context: Context): SettingsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SettingsDatabase::class.java,
                    "settings_database"
                )
                    .fallbackToDestructiveMigration() // Optional: Handle migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
