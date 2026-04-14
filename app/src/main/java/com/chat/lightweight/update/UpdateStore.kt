package com.chat.lightweight.update

import android.content.Context

interface UpdateFailureStore {
    fun getFailureRecord(channel: String): UpdateFailureRecord?
    fun shouldSkipVersion(channel: String, versionCode: Int, nowMs: Long): Boolean
    fun recordFailure(channel: String, versionCode: Int, nowMs: Long, message: String)
    fun clearFailure(channel: String)
}

class UpdateStore(
    context: Context
) : UpdateFailureStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getFailureRecord(channel: String): UpdateFailureRecord? {
        val versionCode = prefs.getInt(versionKey(channel), -1)
        val failedAtMs = prefs.getLong(failedAtKey(channel), -1L)
        if (versionCode <= 0 || failedAtMs <= 0L) {
            return null
        }
        val message = prefs.getString(messageKey(channel), null)
        return UpdateFailureRecord(
            versionCode = versionCode,
            failedAtMs = failedAtMs,
            message = message
        )
    }

    override fun shouldSkipVersion(channel: String, versionCode: Int, nowMs: Long): Boolean {
        val record = getFailureRecord(channel) ?: return false
        if (record.versionCode != versionCode) {
            return false
        }
        return nowMs - record.failedAtMs < UpdateConfig.failureCooldownMs
    }

    override fun recordFailure(channel: String, versionCode: Int, nowMs: Long, message: String) {
        prefs.edit()
            .putInt(versionKey(channel), versionCode)
            .putLong(failedAtKey(channel), nowMs)
            .putString(messageKey(channel), message)
            .apply()
    }

    override fun clearFailure(channel: String) {
        prefs.edit()
            .remove(versionKey(channel))
            .remove(failedAtKey(channel))
            .remove(messageKey(channel))
            .apply()
    }

    private fun versionKey(channel: String) = "failed_version_$channel"
    private fun failedAtKey(channel: String) = "failed_at_$channel"
    private fun messageKey(channel: String) = "failed_message_$channel"

    companion object {
        private const val PREFS_NAME = "app_update_prefs"
    }
}
