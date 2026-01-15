package com.spamshield

import android.content.Context
import java.util.UUID

object DeviceId {
    private const val PREF = "spamshield_prefs"
    private const val KEY_DEV = "device_uuid"

    fun deviceHash(context: Context, pepper: ByteArray): String {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        var id = sp.getString(KEY_DEV, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            sp.edit().putString(KEY_DEV, id).apply()
        }
        return Hashing.hmacSha256Base64Url(pepper, id)
    }
}
