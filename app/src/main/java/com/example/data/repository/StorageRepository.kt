package com.example.data.repository

import android.net.Uri
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class StorageRepository {
    private val storage = FirebaseHelper.storage

    suspend fun uploadAvatar(uid: String, fileUri: Uri): String {
        val ref = storage.reference.child("avatars/${uid}_${System.currentTimeMillis()}.jpg")
        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadAvatarBytes(uid: String, bytes: ByteArray): String {
        val ref = storage.reference.child("avatars/${uid}_${System.currentTimeMillis()}.jpg")
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadChatImage(conversationId: String, fileUri: Uri): String {
        val ref = storage.reference.child("media/$conversationId/${UUID.randomUUID()}.jpg")
        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadVoiceMessage(conversationId: String, audioFile: File): String {
        val ref = storage.reference.child("voice/$conversationId/${UUID.randomUUID()}.m4a")
        ref.putFile(Uri.fromFile(audioFile)).await()
        return ref.downloadUrl.await().toString()
    }
}
