package com.spamshield

import android.content.Context
import android.util.Base64
import org.json.JSONObject

object PepperStore {
    private const val PREF = "spamshield_prefs"
    private const val KEY_PEP = "pepper_b64"

    fun getOrFetchPepper(context: Context): ByteArray? {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val existing = sp.getString(KEY_PEP, null)
        if (existing != null) return Base64.decode(existing, Base64.DEFAULT)

        val boot = Api.getBootstrap() ?: return null
        val pepperB64 = JSONObject(boot).getString("pepper_b64")
        sp.edit().putString(KEY_PEP, pepperB64).apply()
        return Base64.decode(pepperB64, Base64.DEFAULT)
    }
}
