package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)

    var theme: String
        get() = prefs.getString("selected_theme", "default") ?: "default"
        set(value) = prefs.edit().putString("selected_theme", value).apply()

    var readReceiptsEnabled: Boolean
        get() = prefs.getBoolean("read_receipts_enabled", true)
        set(value) = prefs.edit().putBoolean("read_receipts_enabled", value).apply()

    var notificationPreview: Boolean
        get() = prefs.getBoolean("notification_preview", true)
        set(value) = prefs.edit().putBoolean("notification_preview", value).apply()

    var openCalculatorOnResume: Boolean
        get() = prefs.getBoolean("open_calculator_on_resume", false)
        set(value) = prefs.edit().putBoolean("open_calculator_on_resume", value).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean("app_lock_enabled", false)
        set(value) = prefs.edit().putBoolean("app_lock_enabled", value).apply()

    var appLockPin: String
        get() = prefs.getString("app_lock_pin", "") ?: ""
        set(value) = prefs.edit().putString("app_lock_pin", value).apply()

    var protectRecentApps: Boolean
        get() = prefs.getBoolean("protect_recent_apps", false)
        set(value) = prefs.edit().putBoolean("protect_recent_apps", value).apply()

    var voiceChangerPreset: String
        get() = prefs.getString("voice_changer_preset", "Clear") ?: "Clear"
        set(value) = prefs.edit().putString("voice_changer_preset", value).apply()

    var videoBeautyPreset: String
        get() = prefs.getString("video_beauty_preset", "Natural") ?: "Natural"
        set(value) = prefs.edit().putString("video_beauty_preset", value).apply()

    var videoBeautyIntensity: Float
        get() = prefs.getFloat("video_beauty_intensity", 0.7f)
        set(value) = prefs.edit().putFloat("video_beauty_intensity", value).apply()

    fun getChatWallpaper(conversationId: String): String {
        return prefs.getString("wallpaper_$conversationId", "default") ?: "default"
    }

    fun setChatWallpaper(conversationId: String, wallpaper: String) {
        prefs.edit().putString("wallpaper_$conversationId", wallpaper).apply()
    }
}
