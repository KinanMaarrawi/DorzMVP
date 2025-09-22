package com.example.dorzmvp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedAddress::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savedAddressDao(): SavedAddressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dorz_mvp_database"
                )
                // Optional: Add migrations if you change the schema in the future
                // .addMigrations(MIGRATION_1_2 /*, MIGRATION_2_3, ...*/)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
