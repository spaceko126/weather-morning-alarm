package com.weathermorning.alarm.data.local

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

class BackendProxyClientIdStore(context: Context) {
    private val prefs = context.getSharedPreferences("backend_proxy_client", Context.MODE_PRIVATE)

    fun getOrCreate(): String {
        val existing = prefs.getString("client_id", null)
        if (!existing.isNullOrBlank()) return existing

        val created = "install_${UUID.randomUUID().toString().replace("-", "")}"
        prefs.edit {
            putString("client_id", created)
        }
        return created
    }
}
