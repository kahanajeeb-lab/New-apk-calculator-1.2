package com.example.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseHelper.auth

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogleCredential(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: throw Exception("Failed to sign in with Google")
    }

    suspend fun signInWithEmail(email: String, pass: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
        return result.user ?: throw Exception("Failed to sign in")
    }

    suspend fun registerWithEmail(email: String, pass: String, displayName: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
        val user = result.user ?: throw Exception("Failed to register")
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdates).await()
        return user
    }

    suspend fun signInQuickAccount(name: String): FirebaseUser {
        val result = auth.signInAnonymously().await()
        val user = result.user ?: throw Exception("Failed to create session")
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name.ifBlank { "User ${System.currentTimeMillis() % 1000}" })
            .build()
        user.updateProfile(profileUpdates).await()
        return user
    }

    fun signOut() {
        auth.signOut()
    }
}
