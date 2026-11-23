package com.angelcry.preferredwifi

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Represents a known Wi-Fi network in the application's local database.
 *  This is the single source of truth for network preferences within the app.
 *  The system's saved-network list is only used as an import/source of truth,
 *  not as the working list. */
@Entity(tableName = "wifi_networks")
data class WifiNetwork(
    /** Unique identifier for the logical network (SSID). Primary key. */
    @PrimaryKey(val = true) val ssid: String,

    /** Type of security, if reliably determined from scan results (WPA2, WPA3, OPEN, etc.). */
    @ColumnInfo(name = "security_type") val securityType: String?,

    /** User's preferred flag - chosen by the user as their preferred network. */
    @ColumnInfo(name = "preferred") val preferred: Boolean = false,

    /** Flag that this network is in Android's system saved networks list.
     *  Updated only during import from system, not automatically changed. */
    @ColumnInfo(name = "saved") val saved: Boolean = false,

    /** Source of this record: SYSTEM_IMPORT, SCAN_DISCOVERY, or BOTH. */
    @ColumnInfo(name = "source") val source: String,

    /** Timestamp of first detection/recording. */
    @ColumnInfo(name = "first_seen") val firstSeen: Long,

    /** Timestamp of last update/modification. */
    @ColumnInfo(name = "last_updated") val lastUpdated: Long,

    /** Timestamp of last appearance in Wi-Fi scan results (null if not seen recently). */
    @ColumnInfo(name = "last_seen_in_scan") val lastSeenInScan: Long? = null,

    /** JSON-encoded list of observed BSSIDs for reference (not used as logical ID). */
    @ColumnInfo(name = "bssids") val bssids: String? = null
)