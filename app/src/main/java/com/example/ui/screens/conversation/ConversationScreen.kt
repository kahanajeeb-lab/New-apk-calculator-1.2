package com.example.ui.screens.conversation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ChatMessage
import com.example.data.model.UserProfile
import com.example.data.model.UserSummary
import com.example.data.repository.ChatRepository
import com.example.data.repository.PreferencesManager
import com.example.data.repository.PresenceRepository
import com.example.data.repository.StorageRepository
import com.example.service.AudioPlayerHelper
import com.example.service.AudioRecorderHelper
import com.example.ui.components.AudioWaveformBubble
import com.example.ui.components.AvatarImage
import com.example.ui.components.ChatWallpaperContainer
import com.example.ui.components.MessageStatusTicks
import com.example.ui.theme.DarkBg
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConversationScreen(
    conversationId: String,
    otherUser: UserSummary,
    currentUser: UserProfile,
    chatRepository: ChatRepository,
    storageRepository: StorageRepository,
    presenceRepository: PresenceRepository,
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit,
    onStartCall: (targetUser: UserSummary, isVideo: Boolean) -> Unit,
    onQuickSwitchCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<ChatMessage?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var isSendingMedia by remember { mutableStateOf(false) }

    // Audio recording & playback helpers
    val audioRecorder = remember { AudioRecorderHelper(context) }
    val audioPlayer = remember { AudioPlayerHelper() }

    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableLongStateOf(0L) }
    var currentPlayingAudioUrl by remember { mutableStateOf<String?>(null) }
    var audioProgress by remember { mutableFloatStateOf(0f) }

    var showWallpaperPicker by remember { mutableStateOf(false) }
    var currentWallpaper by remember {
        mutableStateOf(preferencesManager.getChatWallpaper(conversationId))
    }

    val messages by chatRepository.observeMessages(conversationId)
        .collectAsState(initial = emptyList())
    val otherUserPresence by presenceRepository.observeUserPresence(otherUser.uid)
        .collectAsState(initial = PresenceRepository.PresenceState(isOnline = false, lastSeen = 0L))
    val isOtherTyping by chatRepository.observeOtherUserTyping(conversationId, otherUser.uid)
        .collectAsState(initial = false)

    // Mark messages as read
    LaunchedEffect(messages.size) {
        chatRepository.markMessagesAsRead(
            conversationId,
            currentUser.uid,
            preferencesManager.readReceiptsEnabled
        )
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Typing debounce
    LaunchedEffect(inputText) {
        if (inputText.isNotEmpty()) {
            chatRepository.setTyping(conversationId, currentUser.uid, true)
            delay(2500)
            chatRepository.setTyping(conversationId, currentUser.uid, false)
        } else {
            chatRepository.setTyping(conversationId, currentUser.uid, false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            chatRepository.setTyping(conversationId, currentUser.uid, false)
            audioPlayer.stop()
            audioRecorder.cancelRecording()
        }
    }

    // Media picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isSendingMedia = true
                try {
                    val downloadUrl = storageRepository.uploadChatImage(conversationId, uri)
                    chatRepository.sendMessage(
                        conversationId = conversationId,
                        sender = currentUser,
                        content = "📷 Photo",
                        type = "image",
                        mediaUrl = downloadUrl,
                        replyToId = replyingToMessage?.id,
                        replyToText = replyingToMessage?.content
                    )
                    replyingToMessage = null
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSendingMedia = false
                }
            }
        }
    }

    fun handleSendTextMessage() {
        if (inputText.isBlank()) return
        val textToSend = inputText.trim()
        inputText = ""

        val replyId = replyingToMessage?.id
        val replyText = replyingToMessage?.content
        replyingToMessage = null

        scope.launch {
            chatRepository.sendMessage(
                conversationId = conversationId,
                sender = currentUser,
                content = textToSend,
                type = "text",
                replyToId = replyId,
                replyToText = replyText
            )
        }
    }

    fun startVoiceRecording() {
        if (audioRecorder.startRecording()) {
            isRecordingAudio = true
            recordingDurationMs = 0L
            scope.launch {
                while (isActive && isRecordingAudio) {
                    delay(200)
                    recordingDurationMs += 200L
                }
            }
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    fun finishVoiceRecordingAndSend() {
        val (file, duration) = audioRecorder.stopRecording()
        isRecordingAudio = false
        if (file != null && duration > 800) {
            scope.launch {
                isSendingMedia = true
                try {
                    val url = storageRepository.uploadVoiceMessage(conversationId, file)
                    chatRepository.sendMessage(
                        conversationId = conversationId,
                        sender = currentUser,
                        content = "🎤 Voice message",
                        type = "voice",
                        mediaUrl = url,
                        mediaDurationMs = duration
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to send voice message", Toast.LENGTH_SHORT).show()
                } finally {
                    isSendingMedia = false
                }
            }
        }
    }

    ChatWallpaperContainer(wallpaper = currentWallpaper) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top App Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161224))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                AvatarImage(
                    photoUrl = otherUser.photoUrl,
                    name = otherUser.displayName,
                    size = 40.dp,
                    showOnlineBadge = true,
                    isOnline = otherUserPresence.isOnline
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = otherUser.displayName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isOtherTyping -> "typing..."
                            otherUserPresence.isOnline -> "Online"
                            otherUserPresence.lastSeen > 0L -> {
                                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                "Last seen ${sdf.format(Date(otherUserPresence.lastSeen))}"
                            }
                            else -> "Offline"
                        },
                        fontSize = 11.sp,
                        color = if (isOtherTyping || otherUserPresence.isOnline) OnlineGreen else TextMuted
                    )
                }

                // Voice Call
                IconButton(onClick = { onStartCall(otherUser, false) }) {
                    Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.primary)
                }

                // Video Call
                IconButton(onClick = { onStartCall(otherUser, true) }) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                }

                // Privacy Quick-Switch to Calculator
                IconButton(onClick = onQuickSwitchCalculator) {
                    Icon(Icons.Default.Calculate, contentDescription = "Quick Switch", tint = Color(0xFFA78BFA))
                }

                var showMoreMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change Wallpaper") },
                            onClick = {
                                showMoreMenu = false
                                showWallpaperPicker = true
                            }
                        )
                    }
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == currentUser.uid

                    ChatMessageBubble(
                        message = msg,
                        isMe = isMe,
                        isPlayingAudio = currentPlayingAudioUrl == msg.mediaUrl,
                        audioProgress = if (currentPlayingAudioUrl == msg.mediaUrl) audioProgress else 0f,
                        onAudioPlayToggle = {
                            if (currentPlayingAudioUrl == msg.mediaUrl) {
                                audioPlayer.stop()
                                currentPlayingAudioUrl = null
                            } else {
                                currentPlayingAudioUrl = msg.mediaUrl
                                audioPlayer.play(
                                    url = msg.mediaUrl,
                                    onProgress = { p, _, _ -> audioProgress = p },
                                    onComplete = { currentPlayingAudioUrl = null },
                                    onError = { currentPlayingAudioUrl = null }
                                )
                            }
                        },
                        onImageClick = { previewImageUrl = msg.mediaUrl },
                        onLongClick = { selectedMessageForMenu = msg }
                    )
                }
            }

            // Quoted Reply Preview Bar
            if (replyingToMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1F1830))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = replyingToMessage!!.senderName,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = replyingToMessage!!.content,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { replyingToMessage = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel reply", tint = TextMuted)
                    }
                }
            }

            // Media sending indicator
            if (isSendingMedia) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading media...", fontSize = 12.sp, color = TextMuted)
                }
            }

            // Bottom Input Composer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF130F21))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (isRecordingAudio) {
                    // Live audio recording interface
                    IconButton(
                        onClick = { audioRecorder.cancelRecording(); isRecordingAudio = false }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = ErrorRed)
                    }

                    val sec = (recordingDurationMs / 1000) % 60
                    val min = (recordingDurationMs / 60000)
                    Text(
                        text = "Recording: ${String.format("%02d:%02d", min, sec)}",
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { finishVoiceRecordingAndSend() },
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Send Voice", tint = Color.White)
                    }
                } else {
                    // Regular composer
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = TextSecondary)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF231C38),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color(0xFF1B152B),
                            unfocusedContainerColor = Color(0xFF1B152B)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = { handleSendTextMessage() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { startVoiceRecording() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF221A36), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Voice",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Message Action Bottom Dialog (Reactions, Reply, Copy, Delete)
    if (selectedMessageForMenu != null) {
        val targetMsg = selectedMessageForMenu!!
        Dialog(onDismissRequest = { selectedMessageForMenu = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1429)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Emoji reactions row
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        listOf("❤️", "👍", "😂", "😮", "😢", "🙏").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 26.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        scope.launch {
                                            chatRepository.addReaction(
                                                conversationId,
                                                targetMsg.id,
                                                currentUser.uid,
                                                emoji
                                            )
                                        }
                                        selectedMessageForMenu = null
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Reply
                    Text(
                        text = "Reply",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                replyingToMessage = targetMsg
                                selectedMessageForMenu = null
                            }
                            .padding(vertical = 10.dp)
                    )

                    // Copy
                    Text(
                        text = "Copy text",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(targetMsg.content))
                                selectedMessageForMenu = null
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 10.dp)
                    )

                    // Delete (if me)
                    if (targetMsg.senderId == currentUser.uid) {
                        Text(
                            text = "Delete message",
                            color = ErrorRed,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        chatRepository.deleteMessage(
                                            conversationId,
                                            targetMsg.id,
                                            targetMsg.senderId,
                                            currentUser.uid
                                        )
                                    }
                                    selectedMessageForMenu = null
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Image Viewer
    if (previewImageUrl != null) {
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { previewImageUrl = null }
            ) {
                AsyncImage(
                    model = previewImageUrl,
                    contentDescription = "Full Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Wallpaper picker dialog
    if (showWallpaperPicker) {
        Dialog(onDismissRequest = { showWallpaperPicker = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1730)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Chat Wallpaper", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf("default", "midnight", "ocean", "soft", "emerald").forEach { wp ->
                        Text(
                            text = wp.replaceFirstChar { it.uppercase() },
                            color = if (currentWallpaper == wp) MaterialTheme.colorScheme.primary else TextPrimary,
                            fontWeight = if (currentWallpaper == wp) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentWallpaper = wp
                                    preferencesManager.setChatWallpaper(conversationId, wp)
                                    showWallpaperPicker = false
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    isPlayingAudio: Boolean,
    audioProgress: Float,
    onAudioPlayToggle: () -> Unit,
    onImageClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else Color(0xFF221A36)
    val align = if (isMe) Alignment.End else Alignment.Start

    val timeFormatted = remember(message.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Column(
        horizontalAlignment = align,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .padding(10.dp)
        ) {
            Column {
                // Quoted reply banner
                if (!message.replyToText.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.replyToText!!,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Content by Type
                when (message.type) {
                    "image" -> {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onImageClick() }
                        )
                    }
                    "voice" -> {
                        AudioWaveformBubble(
                            isPlaying = isPlayingAudio,
                            progress = audioProgress,
                            durationMs = message.mediaDurationMs,
                            onPlayPauseToggle = onAudioPlayToggle
                        )
                    }
                    else -> {
                        Text(
                            text = message.content,
                            color = Color.White,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Time and status ticks
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusTicks(status = message.status)
                    }
                }
            }
        }

        // Reactions badges under bubble
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 2.dp, start = if (isMe) 0.dp else 6.dp, end = if (isMe) 6.dp else 0.dp)
                    .background(Color(0xFF191329), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                message.reactions.values.distinct().take(4).forEach { emoji ->
                    Text(text = emoji, fontSize = 12.sp)
                }
            }
        }
    }
}
