package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.UserProfile
import com.example.data.model.UserSummary
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.FirebaseHelper
import com.example.data.repository.PreferencesManager
import com.example.data.repository.PresenceRepository
import com.example.data.repository.StorageRepository
import com.example.data.repository.UserRepository
import com.example.service.CallUiState
import com.example.service.WebRtcCallManager
import com.example.ui.screens.admin.AdminPanelScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.calculator.CalculatorScreen
import com.example.ui.screens.call.CallScreen
import com.example.ui.screens.chats.ChatListScreen
import com.example.ui.screens.contacts.ContactsScreen
import com.example.ui.screens.conversation.ConversationScreen
import com.example.ui.screens.lock.AppLockScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.ThemePickerScreen
import com.example.ui.screens.settings.VideoEffectsScreen
import com.example.ui.screens.settings.VoiceEffectsScreen
import com.example.ui.theme.AetherTheme
import com.example.ui.theme.DarkBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    private val preferencesManager by lazy { PreferencesManager(this) }
    private val authRepository by lazy { AuthRepository() }
    private val userRepository by lazy { UserRepository() }
    private val chatRepository by lazy { ChatRepository() }
    private val callRepository by lazy { CallRepository() }
    private val storageRepository by lazy { StorageRepository() }
    private val presenceRepository by lazy { PresenceRepository() }
    private val callManager by lazy { WebRtcCallManager(this, callRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (preferencesManager.protectRecentApps) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        setContent {
            var currentTheme by remember { mutableStateOf(preferencesManager.theme) }

            AetherTheme(selectedTheme = currentTheme) {
                MainAppHost(
                    preferencesManager = preferencesManager,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    chatRepository = chatRepository,
                    callRepository = callRepository,
                    storageRepository = storageRepository,
                    presenceRepository = presenceRepository,
                    callManager = callManager,
                    onThemeChange = { newTheme -> currentTheme = newTheme }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callManager.endCurrentCall()
    }
}

sealed class Screen {
    data object MainTabs : Screen()
    data class Conversation(val conversationId: String, val otherUser: UserSummary) : Screen()
    data class ActiveCall(
        val callerName: String,
        val callerPhoto: String?,
        val callerHumanId: String,
        val isVideo: Boolean,
        val isIncoming: Boolean
    ) : Screen()
    data object Profile : Screen()
    data object Admin : Screen()
    data object ThemePicker : Screen()
    data object VoiceEffects : Screen()
    data object VideoEffects : Screen()
    data object QuickCalculator : Screen()
}

@Composable
fun MainAppHost(
    preferencesManager: PreferencesManager,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    callRepository: CallRepository,
    storageRepository: StorageRepository,
    presenceRepository: PresenceRepository,
    callManager: WebRtcCallManager,
    onThemeChange: (String) -> Unit
) {
    val authUser by authRepository.observeAuthState().collectAsState(initial = authRepository.currentUser)

    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainTabs) }
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var isAppUnlocked by remember { mutableStateOf(!preferencesManager.appLockEnabled) }

    // Request Runtime permissions for Camera, Microphone, and Notifications
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    if (authUser == null) {
        AuthScreen(
            authRepository = authRepository,
            userRepository = userRepository,
            onAuthSuccess = {
                // Refreshed via observeAuthState
            }
        )
        return
    }

    // Observe user profile in Firestore
    val userProfile by userRepository.observeUserProfile(authUser!!.uid)
        .collectAsState(initial = null)

    LaunchedEffect(authUser?.uid) {
        authUser?.uid?.let { uid ->
            presenceRepository.setupPresence(uid)
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        FirebaseHelper.firestore.collection("users").document(uid)
                            .update("fcmToken", task.result)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // App Lock enforcement
    if (preferencesManager.appLockEnabled && !isAppUnlocked) {
        AppLockScreen(
            preferencesManager = preferencesManager,
            onUnlocked = { isAppUnlocked = true }
        )
        return
    }

    if (userProfile == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val activeProfile = userProfile!!

    // Global Incoming Call Listener
    val incomingCall by callRepository.observeIncomingCalls(activeProfile.uid)
        .collectAsState(initial = null)

    LaunchedEffect(incomingCall) {
        val call = incomingCall
        if (call != null && currentScreen !is Screen.ActiveCall) {
            callManager.triggerSingleVibrationPulse()
            currentScreen = Screen.ActiveCall(
                callerName = call.callerName,
                callerPhoto = call.callerPhotoUrl,
                callerHumanId = call.callerHumanId,
                isVideo = call.callType == "video",
                isIncoming = true
            )
            callManager.acceptIncomingCall(call.callId, call.callType == "video")
        }
    }

    when (val screen = currentScreen) {
        is Screen.Conversation -> {
            ConversationScreen(
                conversationId = screen.conversationId,
                otherUser = screen.otherUser,
                currentUser = activeProfile,
                chatRepository = chatRepository,
                storageRepository = storageRepository,
                presenceRepository = presenceRepository,
                preferencesManager = preferencesManager,
                onNavigateBack = { currentScreen = Screen.MainTabs },
                onStartCall = { target, isVideo ->
                    val callId = callRepository.startCall(activeProfile, target, if (isVideo) "video" else "voice")
                    callManager.startOutgoingCall(callId, isVideo)
                    currentScreen = Screen.ActiveCall(
                        callerName = target.displayName,
                        callerPhoto = target.photoUrl,
                        callerHumanId = target.humanId,
                        isVideo = isVideo,
                        isIncoming = false
                    )
                },
                onQuickSwitchCalculator = { currentScreen = Screen.QuickCalculator }
            )
        }

        is Screen.ActiveCall -> {
            CallScreen(
                callManager = callManager,
                callerName = screen.callerName,
                callerPhoto = screen.callerPhoto,
                callerHumanId = screen.callerHumanId,
                isVideo = screen.isVideo,
                isIncoming = screen.isIncoming,
                preferencesManager = preferencesManager,
                onCallEnded = {
                    callManager.resetState()
                    currentScreen = Screen.MainTabs
                }
            )
        }

        is Screen.Profile -> {
            ProfileScreen(
                currentUser = activeProfile,
                userRepository = userRepository,
                storageRepository = storageRepository,
                onNavigateBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.Admin -> {
            AdminPanelScreen(
                currentUser = activeProfile,
                adminRepository = AdminRepository(),
                onNavigateBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.ThemePicker -> {
            ThemePickerScreen(
                preferencesManager = preferencesManager,
                onThemeChanged = onThemeChange,
                onNavigateBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.VoiceEffects -> {
            VoiceEffectsScreen(
                preferencesManager = preferencesManager,
                onNavigateBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.VideoEffects -> {
            VideoEffectsScreen(
                preferencesManager = preferencesManager,
                onNavigateBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.QuickCalculator -> {
            CalculatorScreen(
                onNavigateBack = { currentScreen = Screen.MainTabs }
            )
        }

        is Screen.MainTabs -> {
            Scaffold(
                containerColor = DarkBg,
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF140F24),
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedBottomTab == 0,
                            onClick = { selectedBottomTab = 0 },
                            icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                            label = { Text("Chats") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        NavigationBarItem(
                            selected = selectedBottomTab == 1,
                            onClick = { selectedBottomTab = 1 },
                            icon = { Icon(Icons.Default.People, contentDescription = "Contacts") },
                            label = { Text("Contacts") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        NavigationBarItem(
                            selected = selectedBottomTab == 2,
                            onClick = { selectedBottomTab = 2 },
                            icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculator") },
                            label = { Text("Calculator") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        NavigationBarItem(
                            selected = selectedBottomTab == 3,
                            onClick = { selectedBottomTab = 3 },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedBottomTab) {
                        0 -> {
                            ChatListScreen(
                                currentUser = activeProfile,
                                chatRepository = chatRepository,
                                onConversationClick = { convId ->
                                    val otherSummary = UserSummary(
                                        uid = "other",
                                        displayName = "Conversation",
                                        humanId = ""
                                    )
                                    currentScreen = Screen.Conversation(convId, otherSummary)
                                },
                                onQuickSwitchCalculator = { currentScreen = Screen.QuickCalculator },
                                onAdminClick = { currentScreen = Screen.Admin },
                                onProfileClick = { currentScreen = Screen.Profile },
                                onNewChatClick = { selectedBottomTab = 1 }
                            )
                        }

                        1 -> {
                            ContactsScreen(
                                currentUser = activeProfile,
                                userRepository = userRepository,
                                chatRepository = chatRepository,
                                onStartChat = { convId ->
                                    val summary = UserSummary(uid = "friend", displayName = "Friend", humanId = "")
                                    currentScreen = Screen.Conversation(convId, summary)
                                },
                                onStartCall = { target, isVideo ->
                                    val callId = callRepository.startCall(activeProfile, target, if (isVideo) "video" else "voice")
                                    callManager.startOutgoingCall(callId, isVideo)
                                    currentScreen = Screen.ActiveCall(
                                        callerName = target.displayName,
                                        callerPhoto = target.photoUrl,
                                        callerHumanId = target.humanId,
                                        isVideo = isVideo,
                                        isIncoming = false
                                    )
                                }
                            )
                        }

                        2 -> {
                            CalculatorScreen(
                                onNavigateBack = null
                            )
                        }

                        3 -> {
                            SettingsScreen(
                                currentUser = activeProfile,
                                preferencesManager = preferencesManager,
                                onProfileClick = { currentScreen = Screen.Profile },
                                onThemesClick = { currentScreen = Screen.ThemePicker },
                                onVoiceEffectsClick = { currentScreen = Screen.VoiceEffects },
                                onVideoEffectsClick = { currentScreen = Screen.VideoEffects },
                                onAdminClick = { currentScreen = Screen.Admin },
                                onQuickSwitchCalculator = { currentScreen = Screen.QuickCalculator },
                                onSignOut = {
                                    authRepository.signOut()
                                    isAppUnlocked = !preferencesManager.appLockEnabled
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
