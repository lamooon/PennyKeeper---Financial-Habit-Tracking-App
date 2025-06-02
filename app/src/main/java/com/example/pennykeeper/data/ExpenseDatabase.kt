package com.example.pennykeeper.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pennykeeper.data.dao.CategoryDao
import com.example.pennykeeper.data.dao.ExpenseDao
import com.example.pennykeeper.data.model.CategoryEntity
import com.example.pennykeeper.data.model.Converters
import com.example.pennykeeper.data.model.Expense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This DB file will destroy (via .fallBackToDestructiveMigration) and rebuild the DB file.
 * It will insert prefilled categories (which users cannot delete or update)
 */

@Database(
    entities = [Expense::class, CategoryEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    // Prefill the example categories
                                    val categoryDao = database.categoryDao()
                                    categoryDao.insertCategory(CategoryEntity(name = "Groceries", isDefault = true))
                                    categoryDao.insertCategory(CategoryEntity(name = "Subscriptions", isDefault = true))
                                    categoryDao.insertCategory(CategoryEntity(name = "Taxes", isDefault = true))
                                    categoryDao.insertCategory(CategoryEntity(name = "Entertainment", isDefault = true))
                                    categoryDao.insertCategory(CategoryEntity(name = "Utilities", isDefault = true))
                                    categoryDao.insertCategory(CategoryEntity(name = "Other", isDefault = true))
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}