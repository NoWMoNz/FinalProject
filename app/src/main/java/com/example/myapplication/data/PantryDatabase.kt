package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [PantryItemEntity::class, SavedRecipeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PantryDatabase : RoomDatabase() {

    abstract fun pantryDao(): PantryDao
    abstract fun savedRecipeDao(): SavedRecipeDao

    companion object {
        @Volatile
        private var INSTANCE: PantryDatabase? = null

        fun getDatabase(context: Context): PantryDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = try {
                    context.applicationContext
                } catch (_: Throwable) {
                    context
                } ?: context

                val instance = try {
                    Room.databaseBuilder(
                        appContext,
                        PantryDatabase::class.java,
                        "pantry_wise_db"
                    ).addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = database.pantryDao()
                                    dao.insertPantryItem(PantryItemEntity(name = "Chicken Breast", daysUntilExpiration = 1, category = "Meat"))
                                    dao.insertPantryItem(PantryItemEntity(name = "Tomatoes", daysUntilExpiration = 2, category = "Produce"))
                                    dao.insertPantryItem(PantryItemEntity(name = "Spinach", daysUntilExpiration = 2, category = "Produce"))
                                    dao.insertPantryItem(PantryItemEntity(name = "Garlic", daysUntilExpiration = 5, category = "Pantry"))
                                    dao.insertPantryItem(PantryItemEntity(name = "Brown Rice", daysUntilExpiration = 12, category = "Pantry"))
                                    dao.insertPantryItem(PantryItemEntity(name = "Avocado", daysUntilExpiration = 3, category = "Produce"))
                                }
                            }
                        }
                    }).build()
                } catch (_: Throwable) {
                    Room.inMemoryDatabaseBuilder(
                        context,
                        PantryDatabase::class.java
                    ).build()
                }
                INSTANCE = instance
                instance
            }
        }
    }
}
