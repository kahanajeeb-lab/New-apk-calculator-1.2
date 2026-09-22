package com.example.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseHelper {
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val currentApps = FirebaseApp.getApps(context)
        if (currentApps.isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyBTQKtAJDWLk-iwfOuSJ6ST4qEy0N4qlKg")
                .setApplicationId("1:42e126a1872d056df121c4c")
                .setProjectId("chat-7305d")
                .setDatabaseUrl("https://chat-7305d-default-rtdb.firebaseio.com")
                .setStorageBucket("chat-7305d.firebasestorage.app")
                .setGcmSenderId("68083133837")
                .build()
            FirebaseApp.initializeApp(context, options)
        }

        // Enable Firestore offline persistence
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
        } catch (_: Exception) {}

        // Enable Realtime Database persistence
        try {
            FirebaseDatabase.getInstance("https://chat-7305d-default-rtdb.firebaseio.com")
                .setPersistenceEnabled(true)
        } catch (_: Exception) {}

        initialized = true
    }

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    val storage: FirebaseStorage
        get() = FirebaseStorage.getInstance("gs://chat-7305d.firebasestorage.app")

    val database: FirebaseDatabase
        get() = FirebaseDatabase.getInstance("https://chat-7305d-default-rtdb.firebaseio.com")
}
