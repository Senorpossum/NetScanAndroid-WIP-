package com.example.networkscanner.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "network_snapshots")
data class NetworkSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val ssid: String,
    val bssid: String,
    val gatewayIp: String,
    val subnetMask: String,
    val deviceCount: Int
)

@Entity(tableName = "survey_points")
data class SurveyPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val floorplanId: Long,
    val x: Float,
    val y: Float,
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val linkSpeed: Int
)

@Entity(
    tableName = "discovered_hosts",
    foreignKeys = [
        ForeignKey(
            entity = NetworkSnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["snapshotId"])]
)
data class DiscoveredHostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snapshotId: Long,
    val ipAddress: String,
    val macAddress: String?,
    val vendorOui: String?,
    val hostname: String?,
    val openPorts: List<Int>,
    val isNewDevice: Boolean,
    val bannerString: String? = null,
    val mdnsHostname: String? = null,
    val ssdpManufacturer: String? = null,
    val ssdpModelName: String? = null,
    val ssdpModelNumber: String? = null,
    val ssdpSerialNumber: String? = null
)

class Converters {
    @TypeConverter
    fun fromPortList(value: List<Int>?): String {
        return value?.joinToString(separator = ",") ?: ""
    }

    @TypeConverter
    fun toPortList(value: String?): List<Int> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { it.toIntOrNull() }
    }
}

data class SnapshotWithHosts(
    @Embedded val snapshot: NetworkSnapshotEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "snapshotId"
    )
    val hosts: List<DiscoveredHostEntity>
)

@Dao
interface NetworkScannerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: NetworkSnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHosts(hosts: List<DiscoveredHostEntity>): List<Long>

    @Transaction
    @Query("SELECT * FROM network_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshotsWithHostsFlow(): Flow<List<SnapshotWithHosts>>
    
    @Transaction
    @Query("SELECT * FROM network_snapshots WHERE id = :snapshotId")
    suspend fun getSnapshotById(snapshotId: Long): SnapshotWithHosts?
}

@Database(entities = [NetworkSnapshotEntity::class, SurveyPointEntity::class, DiscoveredHostEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun networkScannerDao(): NetworkScannerDao
}
