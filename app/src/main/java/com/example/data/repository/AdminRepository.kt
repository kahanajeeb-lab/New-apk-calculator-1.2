package com.example.data.repository

import com.example.data.model.AppConfig
import com.example.data.model.UserProfile
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminRepository {
    private val firestore = FirebaseHelper.firestore

    suspend fun verifyAdminAuthorization(uid: String): Boolean {
        if (uid.isEmpty()) return false
        val userDoc = firestore.collection("users").document(uid).get().await()
        val role = userDoc.getString("role")
        if (role == "admin") return true

        // Also check server-side authorized admins collection
        val adminCheck = firestore.collection("admins").document(uid).get().await()
        return adminCheck.exists()
    }

    suspend fun elevateUserToAdmin(uid: String, authCode: String): Result<Boolean> {
        val serverAuthDoc = firestore.collection("app_config").document("admin_auth").get().await()
        val storedHash = serverAuthDoc.getString("secretCode") ?: "20020022"
        if (authCode.trim() == storedHash) {
            firestore.collection("users").document(uid).update("role", "admin").await()
            firestore.collection("admins").document(uid).set(mapOf("authorizedAt" to System.currentTimeMillis())).await()
            return Result.success(true)
        }
        return Result.failure(Exception("Invalid administrative authorization code."))
    }

    fun observeAppConfig(): Flow<AppConfig> = callbackFlow {
        val listener = firestore.collection("app_config").document("main")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(AppConfig())
                    return@addSnapshotListener
                }
                val maintenance = snapshot.getBoolean("maintenanceMode") ?: false
                val announcement = snapshot.getString("announcement") ?: ""
                val defaultTheme = snapshot.getString("defaultTheme") ?: "default"
                val themes = (snapshot.get("allowedThemes") as? List<String>) ?: listOf(
                    "default", "purple", "ocean", "midnight", "soft", "classic", "gunmetal", "lavender"
                )
                val voicePresets = (snapshot.get("voicePresets") as? List<String>) ?: listOf(
                    "Soft", "Bright", "Clear", "Warm", "Light"
                )
                val videoPresets = (snapshot.get("videoFilterPresets") as? List<String>) ?: listOf(
                    "Original", "Natural", "Clear", "Soft", "Glow", "Fresh", "Warm", "Classic"
                )

                trySend(
                    AppConfig(
                        maintenanceMode = maintenance,
                        announcement = announcement,
                        allowedThemes = themes,
                        defaultTheme = defaultTheme,
                        voicePresets = voicePresets,
                        videoFilterPresets = videoPresets
                    )
                )
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateAppConfig(config: AppConfig) {
        val data = mapOf(
            "maintenanceMode" to config.maintenanceMode,
            "announcement" to config.announcement,
            "allowedThemes" to config.allowedThemes,
            "defaultTheme" to config.defaultTheme,
            "voicePresets" to config.voicePresets,
            "videoFilterPresets" to config.videoFilterPresets,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("app_config").document("main").set(data, SetOptions.merge()).await()
    }

    suspend fun getAllUsers(): List<UserProfile> {
        val snapshot = firestore.collection("users").limit(50).get().await()
        return snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
    }

    suspend fun submitAbuseReport(reporterUid: String, reportedUid: String, reason: String) {
        val doc = firestore.collection("abuse_reports").document()
        doc.set(
            mapOf(
                "id" to doc.id,
                "reporterUid" to reporterUid,
                "reportedUid" to reportedUid,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
        ).await()
    }
}
