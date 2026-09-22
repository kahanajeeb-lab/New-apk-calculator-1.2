package com.example.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.ui.components.AvatarImage
import com.example.ui.components.MessageStatusTicks
import com.example.ui.theme.DarkBg
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    currentUser: UserProfile,
    chatRepository: ChatRepository,
    onConversationClick: (conversationId: String) -> Unit,
    onQuickSwitchCalculator: () -> Unit,
    onAdminClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val rawConversations by chatRepository.observeConversations(currentUser.uid)
        .collectAsState(initial = emptyList())

    val filteredConversations = rawConversations.filter { conv ->
        val otherName = conv.getOtherParticipantName(currentUser.uid)
        val otherHumanId = conv.getOtherParticipantHumanId(currentUser.uid)
        val message = conv.lastMessage
        searchQuery.isBlank() ||
                otherName.contains(searchQuery, ignoreCase = true) ||
                otherHumanId.contains(searchQuery, ignoreCase = true) ||
                message.contains(searchQuery, ignoreCase = true)
    }.sortedWith(compareByDescending<Conversation> { it.pinnedBy.contains(currentUser.uid) }
        .thenByDescending { it.lastMessageTimestamp })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // WhatsApp-like Top Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Aether",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Privacy Quick-Switch Button (Calculators screen)
                IconButton(
                    onClick = onQuickSwitchCalculator,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF221A36), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Quick Privacy Switch",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Admin "JB" Identifier entry point
                IconButton(
                    onClick = onAdminClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF1E1730), CircleShape)
                ) {
                    Text(
                        text = "JB",
                        color = Color(0xFFA78BFA),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Profile DP
                Box(modifier = Modifier.clickable { onProfileClick() }) {
                    AvatarImage(
                        photoUrl = currentUser.photoUrl,
                        name = currentUser.displayName,
                        size = 38.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats or messages") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextMuted)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF231C38),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredConversations.isEmpty()) {
                // Empty state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF1B152B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No conversations yet",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the message button to find friends by 6-digit Human ID",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { conv ->
                        ConversationItemRow(
                            conversation = conv,
                            currentUserId = currentUser.uid,
                            onClick = { onConversationClick(conv.id) },
                            onTogglePin = {
                                val isPinned = conv.pinnedBy.contains(currentUser.uid)
                                scope.launch { chatRepository.togglePinConversation(conv.id, currentUser.uid, isPinned) }
                            },
                            onToggleMute = {
                                val isMuted = conv.mutedBy.contains(currentUser.uid)
                                scope.launch { chatRepository.toggleMuteConversation(conv.id, currentUser.uid, isMuted) }
                            },
                            onToggleArchive = {
                                val isArchived = conv.archivedBy.contains(currentUser.uid)
                                scope.launch { chatRepository.toggleArchiveConversation(conv.id, currentUser.uid, isArchived) }
                            },
                            onDelete = {
                                scope.launch { chatRepository.deleteConversation(conv.id) }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to start chat
        FloatingActionButton(
            onClick = onNewChatClick,
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "New Chat", tint = Color.White)
        }
    }
}

@Composable
fun ConversationItemRow(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val otherName = conversation.getOtherParticipantName(currentUserId)
    val otherPhoto = conversation.getOtherParticipantPhoto(currentUserId)
    val isPinned = conversation.pinnedBy.contains(currentUserId)
    val isMuted = conversation.mutedBy.contains(currentUserId)
    val unreadCount = conversation.unreadCounts[currentUserId] ?: 0L
    val isMyLastMessage = conversation.lastMessageSenderId == currentUserId

    val timeFormatted = remember(conversation.lastMessageTimestamp) {
        if (conversation.lastMessageTimestamp == 0L) "" else {
            val date = Date(conversation.lastMessageTimestamp)
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(date)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) Color(0xFF201936) else Color(0xFF161224)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            AvatarImage(photoUrl = otherPhoto, name = otherName, size = 52.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = otherName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 4.dp)
                        )
                    }

                    if (isMuted) {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = "Muted",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 4.dp)
                        )
                    }

                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isMyLastMessage) {
                        MessageStatusTicks(
                            status = conversation.lastMessageStatus,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    Text(
                        text = conversation.lastMessage.ifEmpty { "Tap to chat" },
                        fontSize = 13.sp,
                        color = if (unreadCount > 0) TextPrimary else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (unreadCount > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions",
                        tint = TextMuted
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isPinned) "Unpin Chat" else "Pin Chat") },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                        onClick = { showMenu = false; onTogglePin() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isMuted) "Unmute Chat" else "Mute Chat") },
                        leadingIcon = { Icon(Icons.Default.VolumeOff, contentDescription = null) },
                        onClick = { showMenu = false; onToggleMute() }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive Chat") },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                        onClick = { showMenu = false; onToggleArchive() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Chat", color = ErrorRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}
