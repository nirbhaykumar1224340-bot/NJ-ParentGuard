package com.nj.parentguard.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

class ParentTelephonySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val db = FirebaseFirestore.getInstance()

        return try {
            val parent = db.collection("users").document(uid).get().await()
            if (parent.getString("role") != "parent") return Result.success()

            val children = db.collection("families").document(uid)
                .collection("children").get().await()

            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            for (child in children.documents) {
                val childUid = child.getString("childUid") ?: child.id
                val events = db.collection("users").document(childUid)
                    .collection("telephonyEvents")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(20)
                    .get().await()

                if (!prefs.getBoolean("initialized_${childUid}", false)) {
                    events.documents.forEach { editor.putBoolean("seen_${childUid}_${it.id}", true) }
                    editor.putBoolean("initialized_${childUid}", true)
                    continue
                }

                events.documents.reversed().forEach { event ->
                    val key = "seen_${childUid}_${event.id}"
                    if (prefs.getBoolean(key, false)) return@forEach

                    val type = event.getString("type").orEmpty()
                    val name = event.getString("contactName").orEmpty()
                    val number = event.getString("number").orEmpty().ifBlank { "number unavailable" }
                    val message = event.getString("message").orEmpty()
                    val label = when (type) {
                        "incoming_sms" -> "New SMS"
                        "incoming_call" -> "Incoming call"
                        "call_active" -> "Call active"
                        "call_ended" -> "Call ended"
                        else -> "Telephony event"
                    }
                    val who = if (name.isBlank()) number else "$" + "name — $" + "number"
                    val body = if (message.isBlank()) who else "$" + "who: $" + "message"

                    notifyParent(applicationContext, label, body, event.id.hashCode())
                    editor.putBoolean(key, true)
                }
            }

            editor.apply()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun notifyParent(context: Context, title: String, body: String, id: Int) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "ParentGuard events",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        private const val PREFS = "parentguard_telephony_notifications"
        private const val CHANNEL_ID = "parentguard-telephony"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ParentTelephonySyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "parentguard-parent-telephony-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
