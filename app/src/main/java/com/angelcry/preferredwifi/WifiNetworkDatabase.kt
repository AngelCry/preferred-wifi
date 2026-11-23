package com.angelcry.preferredwifi

import android.content.ApplicationContext
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import javax.inject.Inject
import javax.annotation.PreDestroy
import javax.sql.DataSource

/** Room database for the application's local Wi-Fi network storage.
 *  Contains a single table for WifiNetwork entities.
 *  All working operations (list display, preference selection, auto-switch)
 *  should use this local database, not the system's saved-network list directly. */
@Database(entities = [WifiNetwork::class], version = 1, exportSchema = false)
abstract class WifiNetworkDatabase : RoomDatabase() {

    /** DAO for accessing and manipulating Wi-Fi network records. */
    abstract fun wifiNetworkDao(): WifiNetworkDao

    /** Singleton instance of the database. */
    @Inject
    lateinit var instance: WifiNetworkDatabase

    /** Builds the database if it doesn't exist.
     *  Should be called once during application startup.
     *  @param context Application context
     *  @return The database instance */
    companion object {
        @Volatile
        private var INSTANCE: WifiNetworkDatabase? = null

        fun getDatabase(context: ApplicationContext): WifiNetworkDatabase {
            // Double-check locking for thread safety
            return INSTANCE?.let { it }
                ?: synchronized(WifiNetworkDatabase::class) {
                    INSTANCE?.let { it }
                        ?: Room.databaseBuilder(
                            context,
                            WifiNetworkDatabase::class,
                            "preferred_wifi_db"
                        ).build().also { INSTANCE = it }
                }
        }

        /** Clears the singleton instance for testing. */
        fun clearInstance() {
            INSTANCE = null
        }
    }

    @PreDestroy
    fun onDestroy() {
        INSTANCE = null
    }