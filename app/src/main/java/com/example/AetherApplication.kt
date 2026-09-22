package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.data.repository.FirebaseHelper
import com.example.data.repository.PresenceRepository
import com.example.service.AetherFirebaseMessagingService

class AetherApplication : Application(), Application.ActivityLifecycleCallbacks {
    private val presenceRepository by lazy { PresenceRepository() }
    private var activityReferences = 0
    private var isActivityChangingConfigurations = false

    override fun onCreate() {
        super.onCreate()
        FirebaseHelper.init(this)
        AetherFirebaseMessagingService.createNotificationChannels(this)
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // App enters foreground
            val uid = FirebaseHelper.auth.currentUser?.uid
            if (!uid.isNullOrEmpty()) {
                presenceRepository.setAppForeground(uid, true)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App enters background
            val uid = FirebaseHelper.auth.currentUser?.uid
            if (!uid.isNullOrEmpty()) {
                presenceRepository.setAppForeground(uid, false)
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
