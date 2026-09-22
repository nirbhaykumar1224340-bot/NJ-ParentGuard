package com.nj.parentguard.location
import androidx.room.*
@Dao interface LocationPointDao {
    @Insert suspend fun insert(point:LocationPoint)
    @Query("SELECT * FROM location_points WHERE synced=0 ORDER BY capturedAtEpochMs ASC")
    suspend fun pendingSync():List<LocationPoint>
    @Query("UPDATE location_points SET synced=1 WHERE id IN (:ids)")
    suspend fun markSynced(ids:List<Long>)
}
