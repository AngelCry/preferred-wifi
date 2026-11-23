package com.angelcry.preferredwifi

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Insert
import androidx.room.Query

/** DAO for WifiNetwork entity with atomic upsert/deduplication.
 *  Provides a common function for both system import and scan discovery
 *  to insert/update records without creating duplicates. */
@Dao
interface WifiNetworkDao {

    /** Upsert (insert or update) a WifiNetwork record.
     *  Uses ON_CONFLICT_REPLACE to atomically update the entire record
     *  if a network with the same SSID (primary key) already exists.
     *  This is the common mechanism for both SYSTEM_IMPORT and SCAN_DISCOVERY sources. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(network: WifiNetwork)

    /** Get all networks from the local database, ordered by preferred status then SSID. */
    @Query("SELECT * FROM wifi_networks ORDER BY preferred DESC, ssid ASC")
    fun getAllNetworks(): List<WifiNetwork>

    /** Get only preferred networks. */
    @Query("SELECT * FROM wifi_networks WHERE preferred = 1")
    suspend fun getPreferredNetworks(): List<WifiNetwork>

    /** Get only non-preferred networks. */
    @Query("SELECT * FROM wifi_networks WHERE preferred = 0")
    suspend fun getNonPreferredNetworks(): List<WifiNetwork>

    /** Delete a network by SSID. */
    @Query("DELETE FROM wifi_networks WHERE ssid = :ssid")
    suspend fun deleteBySsid(ssid: String)

    /** Clear all networks. */
    @Query("DELETE FROM wifi_networks")
    suspend fun clearAll()
}