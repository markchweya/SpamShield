package com.spamshield

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Hashing {
    fun normalizeKenya(msisdn: String): String {
        val trimmed = msisdn.replace(" ", "").replace("-", "")
        return when {
            trimmed.startsWith("+") -> trimmed
            trimmed.startsWith("0") -> "+254" + trimmed.drop(1)
            trimmed.startsWith("254") -> "+$trimmed"
            else -> trimmed
        }
    }

    fun hmacSha256Base64Url(keyBytes: ByteArray, msg: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val out = mac.doFinal(msg.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP or Base64.URL_SAFE)
    }
}
