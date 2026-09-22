package com.nj.parentguard.events

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { message ->
            TelephonyEventStore.save(
                context,
                "incoming_sms",
                "sender=" + (message.displayOriginatingAddress ?: "unknown") +
                    ", message=" + (message.displayMessageBody ?: "")
            )
        }
    }
}
