package com.nj.parentguard.events

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object TelephonyEventStore {
    fun save(context: Context, type: String, detail: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("telephonyEvents")
            .add(
                mapOf(
                    "type" to type,
                    "detail" to detail,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
    }
}
