package com.nj.parentguard.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object LocationTracking {
    @SuppressLint("MissingPermission")
    fun captureOnce(context: Context) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { location: Location? ->
            if (location == null) return@addOnSuccessListener
            val db = androidx.room.Room.databaseBuilder(
                context.applicationContext,
                ParentGuardDatabase::class.java,
                "parentguard.db"
            ).build()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                db.locationPointDao().insert(
                    LocationPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = location.accuracy,
                        capturedAtEpochMs = System.currentTimeMillis()
                    )
                )
                db.close()
            }
        }
    }

    fun scheduleSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<LocationSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "parentguard-location-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
