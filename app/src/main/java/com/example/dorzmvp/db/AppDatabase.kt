package com.example.dorzmvp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main Room database class for the application.
 *
 * This class defines the database configuration and serves as the main access point
 * to the persisted data. It uses a singleton pattern to ensure only one instance
 * of the database is ever created.
 *
 * @property entities The list of data classes that will be tables in the database.
 * @property version The current schema version. Must be incremented on schema changes.
 */
@Database(entities = [SavedAddress::class, RideHistory::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    /** Provides access to the Data Access Object for Saved Addresses. */
    abstract fun savedAddressDao(): SavedAddressDao

    /** Provides access to the Data Access Object for Ride History. */
    abstract fun rideHistoryDao(): RideHistoryDao

    /**
     * Companion object to provide a singleton instance of the database.
     */
    companion object {
        /**
         * The @Volatile annotation ensures that writes to this field are immediately
         * visible to other threads, preventing race conditions.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton database instance, creating it if it doesn't already exist.
         * This synchronized method is thread-safe.
         *
         * @param context The application context.
         * @return The singleton [AppDatabase] instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            // Return the existing instance, or create a new one inside a synchronized block.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dorz_mvp_database"
                )
                    // Allows Room to destructively recreate database tables from scratch
                    // on a version number upgrade. Good for development, but data will be lost.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance // Assign the newly created instance
                instance // Return the instance
            }
        }
    }
}
