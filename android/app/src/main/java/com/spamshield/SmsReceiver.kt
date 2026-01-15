package com.spamshield

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.provider.Telephony
import org.json.JSONObject
import java.time.Instant
import kotlin.concurrent.thread

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (msgs.isEmpty()) return

        val senderRaw = msgs[0].originatingAddress ?: return
        val sender = Hashing.normalizeKenya(senderRaw)

        val tsIso = Instant.now().toString()
        val inContacts = isInContacts(context, sender)

        thread {
            val pepperBytes = PepperStore.getOrFetchPepper(context) ?: return@thread
            val senderHash = Hashing.hmacSha256Base64Url(pepperBytes, sender)
            val deviceHash = DeviceId.deviceHash(context, pepperBytes)

            val ev = JSONObject()
            ev.put("sender_hash", senderHash)
            ev.put("device_hash", deviceHash)
            ev.put("ts_iso", tsIso)
            ev.put("is_in_contacts", inContacts)
            ev.put("action", "none")

            Api.postEvent(ev.toString())

            val riskJson = Api.getRisk(senderHash) ?: return@thread
            val risk = JSONObject(riskJson)
            val level = risk.optString("level", "normal")
            val score = risk.optInt("score", 0)

            if (level == "high" || level == "medium") {
                val reasons = risk.optJSONArray("reasons")
                val reasonText =
                    if (reasons != null && reasons.length() > 0) reasons.getString(0)
                    else "Suspicious sender behavior detected"
                RiskNotifier.notify(context, sender, score, reasonText)
            }
        }
    }

    private fun isInContacts(context: Context, phoneE164: String): Boolean {
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(phoneE164)
        )
        context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null).use { c ->
            return c != null && c.moveToFirst()
        }
    }
}
