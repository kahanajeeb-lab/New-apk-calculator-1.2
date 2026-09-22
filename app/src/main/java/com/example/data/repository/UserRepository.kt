package com.example.data.repository

import com.example.data.model.FriendRequest
import com.example.data.model.UserProfile
import com.example.data.model.UserSummary
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class UserRepository {
    private val firestore = FirebaseHelper.firestore

    fun formatHumanId(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length == 6) {
            "${digits.substring(0, 3)} ${digits.substring(3, 6)}"
        } else {
            raw.trim()
        }
    }

    private suspend fun generateUniqueHumanId(): String {
        var attempts = 0
        while (attempts < 10) {
            val num = Random.nextInt(100000, 999999)
            val str = num.toString()
            val formatted = "${str.substring(0, 3)} ${str.substring(3, 6)}"

            val existing = firestore.collection("users")
                .whereEqualTo("humanId", formatted)
                .limit(1)
                .get()
                .await()

            if (existing.isEmpty) {
                return formatted
            }
            attempts++
        }
        val fallback = Random.nextInt(100000, 999999).toString()
        return "${fallback.substring(0, 3)} ${fallback.substring(3, 6)}"
    }

    suspend fun getOrCreateProfile(
        uid: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ): UserProfile {
        val userDocRef = firestore.collection("users").document(uid)
        val snapshot = userDocRef.get().await()

        if (snapshot.exists()) {
            val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(uid = uid)
            // Update last seen
            userDocRef.update("lastSeen", System.currentTimeMillis())
            return profile
        } else {
            val generatedHumanId = generateUniqueHumanId()
            val name = displayName?.ifBlank { null } ?: "User ${generatedHumanId.takeLast(3)}"
            val profile = UserProfile(
                uid = uid,
                displayName = name,
                email = email ?: "",
                photoUrl = photoUrl ?: "",
                humanId = generatedHumanId,
                createdAt = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                presence = "online"
            )
            userDocRef.set(profile.toMap(), SetOptions.merge()).await()
            return profile
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val profile = snapshot?.toObject(UserProfile::class.java)
                trySend(profile)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateProfile(
        uid: String,
        displayName: String,
        bio: String,
        photoUrl: String?
    ) {
        val updates = mutableMapOf<String, Any>(
            "displayName" to displayName,
            "bio" to bio,
            "lastSeen" to System.currentTimeMillis()
        )
        if (!photoUrl.isNullOrEmpty()) {
            updates["photoUrl"] = photoUrl
        }
        firestore.collection("users").document(uid).update(updates).await()
    }

    suspend fun searchByHumanId(rawInput: String): UserProfile? {
        val formatted = formatHumanId(rawInput)
        val digitsOnly = rawInput.filter { it.isDigit() }
        val altFormatted = if (digitsOnly.length == 6) "${digitsOnly.substring(0, 3)} ${digitsOnly.substring(3, 6)}" else rawInput

        val query = firestore.collection("users")
            .whereEqualTo("humanId", altFormatted)
            .limit(1)
            .get()
            .await()

        if (!query.isEmpty) {
            return query.documents.first().toObject(UserProfile::class.java)
        }

        // Try direct exact query
        val direct = firestore.collection("users")
            .whereEqualTo("humanId", formatted)
            .limit(1)
            .get()
            .await()

        return direct.documents.firstOrNull()?.toObject(UserProfile::class.java)
    }

    suspend fun sendFriendRequest(fromUser: UserProfile, targetHumanId: String): Result<String> {
        val targetUser = searchByHumanId(targetHumanId)
            ?: return Result.failure(Exception("No user found with Human ID: $targetHumanId"))

        if (targetUser.uid == fromUser.uid) {
            return Result.failure(Exception("You cannot send a friend request to yourself."))
        }

        // Check if already friends
        val friendCheck = firestore.collection("users").document(fromUser.uid)
            .collection("friends").document(targetUser.uid).get().await()
        if (friendCheck.exists()) {
            return Result.failure(Exception("You are already friends with ${targetUser.displayName}."))
        }

        // Check for existing pending request
        val existing = firestore.collection("friend_requests")
            .whereEqualTo("fromUid", fromUser.uid)
            .whereEqualTo("toUid", targetUser.uid)
            .whereEqualTo("status", "pending")
            .get()
            .await()

        if (!existing.isEmpty) {
            return Result.failure(Exception("Friend request already sent to ${targetUser.displayName}."))
        }

        val requestDoc = firestore.collection("friend_requests").document()
        val request = FriendRequest(
            id = requestDoc.id,
            fromUid = fromUser.uid,
            fromName = fromUser.displayName,
            fromHumanId = fromUser.humanId,
            fromPhotoUrl = fromUser.photoUrl,
            toUid = targetUser.uid,
            toHumanId = targetUser.humanId,
            status = "pending",
            timestamp = System.currentTimeMillis()
        )
        requestDoc.set(request).await()
        return Result.success("Friend request sent to ${targetUser.displayName}!")
    }

    fun observeIncomingFriendRequests(uid: String): Flow<List<FriendRequest>> = callbackFlow {
        val listener = firestore.collection("friend_requests")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { it.toObject(FriendRequest::class.java) } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    suspend fun acceptFriendRequest(request: FriendRequest) {
        firestore.runBatch { batch ->
            val requestRef = firestore.collection("friend_requests").document(request.id)
            batch.update(requestRef, "status", "accepted")

            // Add to recipient's friends
            val myFriendRef = firestore.collection("users").document(request.toUid)
                .collection("friends").document(request.fromUid)
            batch.set(
                myFriendRef, mapOf(
                    "uid" to request.fromUid,
                    "displayName" to request.fromName,
                    "humanId" to request.fromHumanId,
                    "photoUrl" to request.fromPhotoUrl,
                    "addedAt" to System.currentTimeMillis()
                )
            )

            // Add to sender's friends
            val senderFriendRef = firestore.collection("users").document(request.fromUid)
                .collection("friends").document(request.toUid)
            batch.set(
                senderFriendRef, mapOf(
                    "uid" to request.toUid,
                    "displayName" to request.toHumanId,
                    "humanId" to request.toHumanId,
                    "addedAt" to System.currentTimeMillis()
                )
            )
        }.await()
    }

    suspend fun declineFriendRequest(requestId: String) {
        firestore.collection("friend_requests").document(requestId)
            .update("status", "declined").await()
    }

    fun observeFriends(uid: String): Flow<List<UserSummary>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val friends = snapshot?.documents?.mapNotNull { doc ->
                    val fUid = doc.getString("uid") ?: doc.id
                    val name = doc.getString("displayName") ?: "Contact"
                    val humanId = doc.getString("humanId") ?: ""
                    val photoUrl = doc.getString("photoUrl") ?: ""
                    UserSummary(uid = fUid, displayName = name, humanId = humanId, photoUrl = photoUrl)
                } ?: emptyList()
                trySend(friends)
            }
        awaitClose { listener.remove() }
    }

    suspend fun removeFriend(myUid: String, friendUid: String) {
        firestore.collection("users").document(myUid)
            .collection("friends").document(friendUid).delete().await()
        firestore.collection("users").document(friendUid)
            .collection("friends").document(myUid).delete().await()
    }
}
