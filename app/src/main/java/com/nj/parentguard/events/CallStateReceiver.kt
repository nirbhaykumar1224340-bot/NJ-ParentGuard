package com.nj.parentguard.events

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val event = when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> "incoming_call"
            TelephonyManager.EXTRA_STATE_OFFHOOK -> "call_active"
            TelephonyManager.EXTRA_STATE_IDLE -> "call_ended"
            else -> return
        }
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
        TelephonyEventStore.save(context, event, if (number.isBlank()) "number unavailable" else number)
    }
}
