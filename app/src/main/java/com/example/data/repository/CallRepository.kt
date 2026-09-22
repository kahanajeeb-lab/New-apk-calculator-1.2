package com.example.data.repository

import com.example.data.model.CallSession
import com.example.data.model.UserProfile
import com.example.data.model.UserSummary
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class CallRepository {
    private val database = FirebaseHelper.database

    fun startCall(
        caller: UserProfile,
        receiver: UserSummary,
        callType: String = "voice"
    ): String {
        val callId = UUID.randomUUID().toString()
        val callRef = database.getReference("calls").child(callId)

        val session = CallSession(
            callId = callId,
            callerId = caller.uid,
            callerName = caller.displayName,
            callerHumanId = caller.humanId,
            callerPhotoUrl = caller.photoUrl,
            receiverId = receiver.uid,
            receiverName = receiver.displayName,
            callType = callType,
            status = "ringing",
            timestamp = System.currentTimeMillis()
        )

        callRef.setValue(session)
        callRef.onDisconnect().updateChildren(mapOf("status" to "ended"))
        return callId
    }

    fun observeIncomingCalls(uid: String): Flow<CallSession?> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val callsRef = database.getReference("calls")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeIncoming: CallSession? = null
                for (child in snapshot.children) {
                    val receiverId = child.child("receiverId").getValue(String::class.java)
                    val status = child.child("status").getValue(String::class.java)
                    if (receiverId == uid && status == "ringing") {
                        activeIncoming = child.getValue(CallSession::class.java)
                        break
                    }
                }
                trySend(activeIncoming)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        callsRef.addValueEventListener(listener)
        awaitClose { callsRef.removeEventListener(listener) }
    }

    fun observeCallSession(callId: String): Flow<CallSession?> = callbackFlow {
        val callRef = database.getReference("calls").child(callId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val session = snapshot.getValue(CallSession::class.java)
                trySend(session)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        callRef.addValueEventListener(listener)
        awaitClose { callRef.removeEventListener(listener) }
    }

    fun acceptCall(callId: String) {
        database.getReference("calls").child(callId).updateChildren(mapOf("status" to "accepted"))
    }

    fun declineCall(callId: String) {
        database.getReference("calls").child(callId).updateChildren(mapOf("status" to "declined"))
    }

    fun endCall(callId: String) {
        database.getReference("calls").child(callId).updateChildren(mapOf("status" to "ended"))
    }

    fun sendSdpOffer(callId: String, sdp: String) {
        database.getReference("calls").child(callId).child("sdpOffer").setValue(sdp)
    }

    fun sendSdpAnswer(callId: String, sdp: String) {
        database.getReference("calls").child(callId).child("sdpAnswer").setValue(sdp)
    }

    fun sendIceCandidate(callId: String, isCaller: Boolean, candidateJson: String) {
        val path = if (isCaller) "callerCandidates" else "receiverCandidates"
        database.getReference("calls").child(callId).child(path).push().setValue(candidateJson)
    }

    fun observeRemoteCandidates(callId: String, isCaller: Boolean): Flow<String> = callbackFlow {
        val path = if (isCaller) "receiverCandidates" else "callerCandidates"
        val candidatesRef = database.getReference("calls").child(callId).child(path)
        val listener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val cand = snapshot.getValue(String::class.java)
                if (cand != null) {
                    trySend(cand)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        candidatesRef.addChildEventListener(listener)
        awaitClose { candidatesRef.removeEventListener(listener) }
    }
}
