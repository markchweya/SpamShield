package com.spamshield

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object Api {
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient()

    // Emulator: http://10.0.2.2:8080
    // Real phone: use your laptop IP on same WiFi e.g. http://192.168.1.10:8080
    var baseUrl: String = "http://10.0.2.2:8080"

    fun getBootstrap(): String? {
        val req = Request.Builder().url("$baseUrl/v1/bootstrap").get().build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            return res.body?.string()
        }
    }

    fun postEvent(json: String): Boolean {
        val body = json.toRequestBody(JSON)
        val req = Request.Builder().url("$baseUrl/v1/events").post(body).build()
        client.newCall(req).execute().use { res -> return res.isSuccessful }
    }

    fun getRisk(senderHash: String): String? {
        val req = Request.Builder().url("$baseUrl/v1/risk/$senderHash").get().build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            return res.body?.string()
        }
    }

    fun postFeedback(json: String): Boolean {
        val body = json.toRequestBody(JSON)
        val req = Request.Builder().url("$baseUrl/v1/feedback").post(body).build()
        client.newCall(req).execute().use { res -> return res.isSuccessful }
    }
}
