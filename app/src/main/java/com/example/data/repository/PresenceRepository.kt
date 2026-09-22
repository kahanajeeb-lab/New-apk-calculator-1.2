package com.example.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PresenceRepository {
    private val database = FirebaseHelper.database
    private val firestore = FirebaseHelper.firestore

    fun setupPresence(uid: String) {
        if (uid.isEmpty()) return

        val connectedRef = database.getReference(".info/connected")
        val userPresenceRef = database.getReference("presence").child(uid)

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    val onlineData = mapOf(
                        "status" to "online",
                        "timestamp" to ServerValue.TIMESTAMP
                    )
                    userPresenceRef.setValue(onlineData)

                    val offlineData = mapOf(
                        "status" to "offline",
                        "lastSeen" to ServerValue.TIMESTAMP
                    )
                    userPresenceRef.onDisconnect().setValue(offlineData)

                    // Also update Firestore
                    firestore.collection("users").document(uid).update(
                        mapOf(
                            "presence" to "online",
                            "lastSeen" to System.currentTimeMillis()
                        )
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun setAppForeground(uid: String, isForeground: Boolean) {
        if (uid.isEmpty()) return
        val userPresenceRef = database.getReference("presence").child(uid)
        if (isForeground) {
            val onlineData = mapOf(
                "status" to "online",
                "timestamp" to ServerValue.TIMESTAMP
            )
            userPresenceRef.setValue(onlineData)
            firestore.collection("users").document(uid).update(
                mapOf(
                    "presence" to "online",
                    "lastSeen" to System.currentTimeMillis()
                )
            )
        } else {
            val offlineData = mapOf(
                "status" to "offline",
                "lastSeen" to ServerValue.TIMESTAMP
            )
            userPresenceRef.setValue(offlineData)
            firestore.collection("users").document(uid).update(
                mapOf(
                    "presence" to "offline",
                    "lastSeen" to System.currentTimeMillis()
                )
            )
        }
    }

    data class PresenceState(
        val isOnline: Boolean = false,
        val lastSeen: Long = 0L
    )

    fun observeUserPresence(uid: String): Flow<PresenceState> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(PresenceState(isOnline = false, lastSeen = 0L))
            close()
            return@callbackFlow
        }

        val presenceRef = database.getReference("presence").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java) ?: "offline"
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java)
                    ?: snapshot.child("timestamp").getValue(Long::class.java)
                    ?: 0L
                trySend(PresenceState(isOnline = status == "online", lastSeen = lastSeen))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(PresenceState(isOnline = false, lastSeen = 0L))
            }
        }

        presenceRef.addValueEventListener(listener)
        awaitClose { presenceRef.removeEventListener(listener) }
    }
}
