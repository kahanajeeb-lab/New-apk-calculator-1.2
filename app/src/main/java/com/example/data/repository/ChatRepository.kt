package com.example.data.repository

import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.data.model.UserSummary
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val firestore = FirebaseHelper.firestore
    private val database = FirebaseHelper.database

    suspend fun getOrCreateConversation(
        currentUser: UserProfile,
        otherUser: UserSummary
    ): String {
        // Look for existing conversation between these two
        val existing = firestore.collection("conversations")
            .whereArrayContains("participantIds", currentUser.uid)
            .get()
            .await()

        for (doc in existing.documents) {
            val pIds = doc.get("participantIds") as? List<*> ?: emptyList<Any>()
            if (pIds.contains(otherUser.uid)) {
                return doc.id
            }
        }

        // Create new conversation
        val newDoc = firestore.collection("conversations").document()
        val conversation = Conversation(
            id = newDoc.id,
            participantIds = listOf(currentUser.uid, otherUser.uid),
            participantNames = mapOf(
                currentUser.uid to currentUser.displayName,
                otherUser.uid to otherUser.displayName
            ),
            participantPhotos = mapOf(
                currentUser.uid to currentUser.photoUrl,
                otherUser.uid to otherUser.photoUrl
            ),
            participantHumanIds = mapOf(
                currentUser.uid to currentUser.humanId,
                otherUser.uid to otherUser.humanId
            ),
            lastMessage = "Started a conversation",
            lastMessageType = "text",
            lastMessageTimestamp = System.currentTimeMillis(),
            lastMessageSenderId = currentUser.uid,
            lastMessageStatus = "sent",
            unreadCounts = mapOf(currentUser.uid to 0L, otherUser.uid to 0L)
        )

        newDoc.set(conversation).await()
        return newDoc.id
    }

    fun observeConversations(uid: String): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val conversations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val participantIds = doc.get("participantIds") as? List<String> ?: emptyList()
                        val participantNames = (doc.get("participantNames") as? Map<String, String>) ?: emptyMap()
                        val participantPhotos = (doc.get("participantPhotos") as? Map<String, String>) ?: emptyMap()
                        val participantHumanIds = (doc.get("participantHumanIds") as? Map<String, String>) ?: emptyMap()
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val lastMessageType = doc.getString("lastMessageType") ?: "text"
                        val lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L
                        val lastMessageSenderId = doc.getString("lastMessageSenderId") ?: ""
                        val lastMessageStatus = doc.getString("lastMessageStatus") ?: "sent"
                        val unreadCounts = (doc.get("unreadCounts") as? Map<String, Long>) ?: emptyMap()
                        val pinnedBy = (doc.get("pinnedBy") as? List<String>) ?: emptyList()
                        val archivedBy = (doc.get("archivedBy") as? List<String>) ?: emptyList()
                        val mutedBy = (doc.get("mutedBy") as? List<String>) ?: emptyList()
                        val wallpaper = doc.getString("wallpaper") ?: "default"

                        Conversation(
                            id = id,
                            participantIds = participantIds,
                            participantNames = participantNames,
                            participantPhotos = participantPhotos,
                            participantHumanIds = participantHumanIds,
                            lastMessage = lastMessage,
                            lastMessageType = lastMessageType,
                            lastMessageTimestamp = lastMessageTimestamp,
                            lastMessageSenderId = lastMessageSenderId,
                            lastMessageStatus = lastMessageStatus,
                            unreadCounts = unreadCounts,
                            pinnedBy = pinnedBy,
                            archivedBy = archivedBy,
                            mutedBy = mutedBy,
                            wallpaper = wallpaper
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(conversations)
            }
        awaitClose { listener.remove() }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val reactions = (doc.get("reactions") as? Map<String, String>) ?: emptyMap()
                        ChatMessage(
                            id = doc.id,
                            conversationId = conversationId,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderHumanId = doc.getString("senderHumanId") ?: "",
                            content = doc.getString("content") ?: "",
                            type = doc.getString("type") ?: "text",
                            mediaUrl = doc.getString("mediaUrl") ?: "",
                            mediaDurationMs = doc.getLong("mediaDurationMs") ?: 0L,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            status = doc.getString("status") ?: "sent",
                            replyToId = doc.getString("replyToId"),
                            replyToText = doc.getString("replyToText"),
                            reactions = reactions
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        conversationId: String,
        sender: UserProfile,
        content: String,
        type: String = "text",
        mediaUrl: String = "",
        mediaDurationMs: Long = 0L,
        replyToId: String? = null,
        replyToText: String? = null
    ) {
        val messageDoc = firestore.collection("conversations").document(conversationId)
            .collection("messages").document()

        val chatMessage = ChatMessage(
            id = messageDoc.id,
            conversationId = conversationId,
            senderId = sender.uid,
            senderName = sender.displayName,
            senderHumanId = sender.humanId,
            content = content,
            type = type,
            mediaUrl = mediaUrl,
            mediaDurationMs = mediaDurationMs,
            timestamp = System.currentTimeMillis(),
            status = "sent",
            replyToId = replyToId,
            replyToText = replyToText
        )

        messageDoc.set(chatMessage.toMap()).await()

        val preview = when (type) {
            "image" -> "📷 Photo"
            "voice" -> "🎤 Voice message"
            "video" -> "🎥 Video"
            "file" -> "📄 Document"
            "location" -> "📍 Location"
            else -> content
        }

        // Get conversation to find receiver id
        val convDoc = firestore.collection("conversations").document(conversationId).get().await()
        val participantIds = convDoc.get("participantIds") as? List<*> ?: emptyList<Any>()
        val otherUserId = participantIds.firstOrNull { it != sender.uid } as? String

        val updates = mutableMapOf<String, Any>(
            "lastMessage" to preview,
            "lastMessageType" to type,
            "lastMessageTimestamp" to System.currentTimeMillis(),
            "lastMessageSenderId" to sender.uid,
            "lastMessageStatus" to "sent"
        )

        if (otherUserId != null) {
            updates["unreadCounts.$otherUserId"] = FieldValue.increment(1)
        }

        firestore.collection("conversations").document(conversationId).update(updates).await()
    }

    suspend fun markMessagesAsRead(conversationId: String, currentUserId: String, readReceiptsEnabled: Boolean) {
        try {
            // Reset unread count for current user
            firestore.collection("conversations").document(conversationId)
                .update("unreadCounts.$currentUserId", 0)

            if (readReceiptsEnabled) {
                // Find unread messages from other user
                val unread = firestore.collection("conversations").document(conversationId)
                    .collection("messages")
                    .whereNotEqualTo("senderId", currentUserId)
                    .get()
                    .await()

                val batch = firestore.batch()
                var count = 0
                for (doc in unread.documents) {
                    if (doc.getString("status") != "read") {
                        batch.update(doc.reference, "status", "read")
                        count++
                    }
                }
                if (count > 0) {
                    batch.commit().await()
                }
            }
        } catch (_: Exception) {}
    }

    fun setTyping(conversationId: String, uid: String, isTyping: Boolean) {
        if (conversationId.isEmpty() || uid.isEmpty()) return
        val typingRef = database.getReference("typing").child(conversationId).child(uid)
        if (isTyping) {
            typingRef.setValue(true)
            typingRef.onDisconnect().removeValue()
        } else {
            typingRef.removeValue()
        }
    }

    fun observeOtherUserTyping(conversationId: String, otherUid: String): Flow<Boolean> = callbackFlow {
        if (conversationId.isEmpty() || otherUid.isEmpty()) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val typingRef = database.getReference("typing").child(conversationId).child(otherUid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isTyping = snapshot.getValue(Boolean::class.java) ?: false
                trySend(isTyping)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(false)
            }
        }
        typingRef.addValueEventListener(listener)
        awaitClose { typingRef.removeEventListener(listener) }
    }

    suspend fun togglePinConversation(conversationId: String, uid: String, isPinned: Boolean) {
        val op = if (isPinned) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        firestore.collection("conversations").document(conversationId).update("pinnedBy", op).await()
    }

    suspend fun toggleMuteConversation(conversationId: String, uid: String, isMuted: Boolean) {
        val op = if (isMuted) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        firestore.collection("conversations").document(conversationId).update("mutedBy", op).await()
    }

    suspend fun toggleArchiveConversation(conversationId: String, uid: String, isArchived: Boolean) {
        val op = if (isArchived) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        firestore.collection("conversations").document(conversationId).update("archivedBy", op).await()
    }

    suspend fun deleteConversation(conversationId: String) {
        firestore.collection("conversations").document(conversationId).delete().await()
    }

    suspend fun addReaction(conversationId: String, messageId: String, uid: String, emoji: String) {
        firestore.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
            .update("reactions.$uid", emoji).await()
    }

    suspend fun deleteMessage(conversationId: String, messageId: String, senderId: String, currentUserId: String) {
        if (senderId == currentUserId) {
            firestore.collection("conversations").document(conversationId)
                .collection("messages").document(messageId).delete().await()
        }
    }
}
