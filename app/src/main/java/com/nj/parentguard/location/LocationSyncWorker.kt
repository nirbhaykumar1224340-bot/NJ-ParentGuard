package com.nj.parentguard.location

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LocationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.retry()
        val db = FirebaseFirestore.getInstance()
        val localDb = androidx.room.Room.databaseBuilder(
            applicationContext,
            ParentGuardDatabase::class.java,
            "parentguard.db"
        ).build()

        val pending = localDb.locationPointDao().pendingSync()
        if (pending.isEmpty()) return Result.success()

        return try {
            val batch = db.batch()
            val collection = db.collection("users").document(uid).collection("locations")
            pending.forEach { point ->
                batch.set(collection.document(point.id.toString()), mapOf(
                    "latitude" to point.latitude,
                    "longitude" to point.longitude,
                    "accuracyMeters" to point.accuracyMeters,
                    "capturedAtEpochMs" to point.capturedAtEpochMs
                ))
            }
            batch.commit().await()
            localDb.locationPointDao().markSynced(pending.map { it.id })
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            localDb.close()
        }
    }
}
