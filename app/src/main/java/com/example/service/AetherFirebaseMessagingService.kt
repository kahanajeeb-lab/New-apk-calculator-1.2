package com.example.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.repository.FirebaseHelper
import com.example.data.repository.PreferencesManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AetherFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_MESSAGES = "aether_messages_channel"
        const val CHANNEL_CALLS = "aether_calls_channel"
        const val CHANNEL_FRIENDS = "aether_friends_channel"

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val messagesChannel = NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Aether incoming chat messages"
                    enableVibration(true)
                }

                val callsChannel = NotificationChannel(
                    CHANNEL_CALLS,
                    "Voice & Video Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Aether incoming WebRTC call alerts"
                    enableVibration(false) // We handle exact single-pulse vibration explicitly
                }

                val friendsChannel = NotificationChannel(
                    CHANNEL_FRIENDS,
                    "Friend Requests",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Aether incoming friend requests"
                }

                notificationManager.createNotificationChannels(
                    listOf(messagesChannel, callsChannel, friendsChannel)
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseHelper.auth.currentUser?.uid ?: return
        FirebaseHelper.firestore.collection("users").document(uid)
            .update("fcmToken", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val prefs = PreferencesManager(this)
        val type = message.data["type"] ?: "message"

        when (type) {
            "call" -> handleIncomingCallNotification(message, prefs)
            "friend_request" -> handleFriendRequestNotification(message)
            else -> handleChatMessageNotification(message, prefs)
        }
    }

    private fun handleChatMessageNotification(message: RemoteMessage, prefs: PreferencesManager) {
        val senderName = message.data["senderName"] ?: "Aether Contact"
        val rawContent = message.data["content"] ?: "Sent a message"

        val title = if (prefs.notificationPreview) senderName else "Aether"
        val content = if (prefs.notificationPreview) rawContent else "New message received"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("conversationId", message.data["conversationId"])
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    @SuppressLint("MissingPermission")
    private fun handleIncomingCallNotification(message: RemoteMessage, prefs: PreferencesManager) {
        val callerName = message.data["callerName"] ?: "Unknown Caller"
        val callType = message.data["callType"] ?: "voice"
        val callId = message.data["callId"] ?: ""

        val displayCaller = if (prefs.notificationPreview) callerName else "Incoming Call"

        // Trigger EXACTLY ONE vibration pulse per requirement #24
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(400)
                }
            }
        } catch (_: Exception) {}

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("incomingCallId", callId)
            putExtra("incomingCallerName", displayCaller)
            putExtra("incomingCallType", callType)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_CALLS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming $callType call")
            .setContentText(displayCaller)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notification)
    }

    private fun handleFriendRequestNotification(message: RemoteMessage) {
        val senderName = message.data["senderName"] ?: "Someone"
        val humanId = message.data["humanId"] ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("openTab", "contacts")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_FRIENDS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New Friend Request")
            .setContentText("$senderName ($humanId) sent you a friend request.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1002, notification)
    }
}
