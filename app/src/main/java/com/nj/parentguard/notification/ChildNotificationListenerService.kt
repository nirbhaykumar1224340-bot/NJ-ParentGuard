package com.nj.parentguard.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ChildNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        NotificationEventStore.save(sbn.packageName, title, text, sbn.postTime)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("notifications")
            .add(
                mapOf(
                    "packageName" to sbn.packageName,
                    "title" to title,
                    "text" to text,
                    "postedAtEpochMs" to sbn.postTime,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
    }
}

object NotificationEventStore {
    data class Event(
        val packageName: String,
        val title: String,
        val text: String,
        val postedAtEpochMs: Long
    )

    private val events = mutableListOf<Event>()

    fun save(packageName: String, title: String, text: String, postedAtEpochMs: Long) {
        synchronized(events) {
            events += Event(packageName, title, text, postedAtEpochMs)
            if (events.size > 1000) events.removeAt(0)
        }
    }

    fun all(): List<Event> = synchronized(events) { events.toList() }
}
