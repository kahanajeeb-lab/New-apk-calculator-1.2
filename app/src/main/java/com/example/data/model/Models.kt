package com.example.data.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val humanId: String = "",
    val createdAt: Long = 0L,
    val lastSeen: Long = 0L,
    val presence: String = "offline",
    val role: String = "user",
    val bio: String = "Hey there! I am using Aether.",
    val readReceiptsEnabled: Boolean = true,
    val notificationPreview: Boolean = true,
    val openCalculatorOnResume: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val protectRecentApps: Boolean = false,
    val theme: String = "default"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "email" to email,
        "photoUrl" to photoUrl,
        "humanId" to humanId,
        "createdAt" to createdAt,
        "lastSeen" to lastSeen,
        "presence" to presence,
        "role" to role,
        "bio" to bio,
        "readReceiptsEnabled" to readReceiptsEnabled,
        "notificationPreview" to notificationPreview,
        "openCalculatorOnResume" to openCalculatorOnResume,
        "appLockEnabled" to appLockEnabled,
        "appLockPin" to appLockPin,
        "protectRecentApps" to protectRecentApps,
        "theme" to theme
    )
}

data class UserSummary(
    val uid: String = "",
    val displayName: String = "",
    val humanId: String = "",
    val photoUrl: String = "",
    val presence: String = "offline",
    val lastSeen: Long = 0L
)

data class ChatMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderHumanId: String = "",
    val content: String = "",
    val type: String = "text", // text, image, voice, video, file, location
    val mediaUrl: String = "",
    val mediaDurationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent", // sending, sent, delivered, read
    val replyToId: String? = null,
    val replyToText: String? = null,
    val reactions: Map<String, String> = emptyMap()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "conversationId" to conversationId,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderHumanId" to senderHumanId,
        "content" to content,
        "type" to type,
        "mediaUrl" to mediaUrl,
        "mediaDurationMs" to mediaDurationMs,
        "timestamp" to timestamp,
        "status" to status,
        "replyToId" to replyToId,
        "replyToText" to replyToText,
        "reactions" to reactions
    )
}

data class Conversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val participantHumanIds: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageType: String = "text",
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderId: String = "",
    val lastMessageStatus: String = "sent",
    val unreadCounts: Map<String, Long> = emptyMap(),
    val pinnedBy: List<String> = emptyList(),
    val archivedBy: List<String> = emptyList(),
    val mutedBy: List<String> = emptyList(),
    val wallpaper: String = "default"
) {
    fun getOtherParticipantId(currentUserId: String): String {
        return participantIds.firstOrNull { it != currentUserId } ?: ""
    }

    fun getOtherParticipantName(currentUserId: String): String {
        val otherId = getOtherParticipantId(currentUserId)
        return participantNames[otherId] ?: "Unknown User"
    }

    fun getOtherParticipantPhoto(currentUserId: String): String {
        val otherId = getOtherParticipantId(currentUserId)
        return participantPhotos[otherId] ?: ""
    }

    fun getOtherParticipantHumanId(currentUserId: String): String {
        val otherId = getOtherParticipantId(currentUserId)
        return participantHumanIds[otherId] ?: ""
    }
}

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromHumanId: String = "",
    val fromPhotoUrl: String = "",
    val toUid: String = "",
    val toHumanId: String = "",
    val status: String = "pending", // pending, accepted, declined, cancelled
    val timestamp: Long = System.currentTimeMillis()
)

data class CallSession(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerHumanId: String = "",
    val callerPhotoUrl: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val callType: String = "voice", // voice, video
    val status: String = "ringing", // ringing, accepted, declined, ended, busy, timeout
    val sdpOffer: String? = null,
    val sdpAnswer: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppConfig(
    val maintenanceMode: Boolean = false,
    val announcement: String = "",
    val allowedThemes: List<String> = listOf("default", "purple", "ocean", "midnight", "soft", "classic", "gunmetal", "lavender"),
    val defaultTheme: String = "default",
    val voicePresets: List<String> = listOf("Soft", "Bright", "Clear", "Warm", "Light"),
    val videoFilterPresets: List<String> = listOf("Original", "Natural", "Clear", "Soft", "Glow", "Fresh", "Warm", "Classic")
)
