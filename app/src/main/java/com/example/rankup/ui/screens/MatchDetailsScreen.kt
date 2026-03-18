package com.example.rankup.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.rankup.InvitationStatus
import com.example.rankup.PlannedMatch
import com.example.rankup.PlannedTeam
import com.example.rankup.UserProfile
import com.example.rankup.ui.theme.RankUpTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailsScreen(
    match: PlannedMatch,
    allUsers: List<UserProfile>,
    allTeams: List<PlannedTeam>,
    currentUser: UserProfile,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSaveResults: (Int?, Int?) -> Unit,
    onRandomizeTeams: (List<String>, List<String>) -> Unit,
    onCancelMatch: () -> Unit,
    onUpdateStatus: (String, InvitationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val now = Calendar.getInstance().time
    val matchDate = try { sdf.parse(match.dateTime) } catch (e: Exception) { null }
    val isPast = matchDate?.before(now) ?: false
    val missingResults = match.scoreMyTeam == null || match.scoreOpponent == null

    var scoreMyTeamText by remember { mutableStateOf(match.scoreMyTeam?.toString() ?: "") }
    var scoreOpponentText by remember { mutableStateOf(match.scoreOpponent?.toString() ?: "") }

    var selectedTeamName by remember { mutableStateOf(match.myTeam) }

    // Drag and Drop State
    var draggingPlayerId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // Full-section bounds (header + all player rows) used for drop-target detection
    var teamABounds by remember { mutableStateOf<Rect?>(null) }
    var teamBBounds by remember { mutableStateOf<Rect?>(null) }
    var unallocatedBounds by remember { mutableStateOf<Rect?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Match Details",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )
                    Text(
                        text = match.modality,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }

            if (isPast && missingResults) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE91E63))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Missing match results",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Match details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF3F51B5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = match.dateTime, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = match.location, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Compute unallocated player IDs here (outer scope) so the Save Results
            // button can reference it regardless of which branch renders below.
            val unallocatedPlayerIds = if (match.matchType == "Player") {
                match.playerInvitations.keys.filter {
                    !match.teamAPlayers.contains(it) && !match.teamBPlayers.contains(it)
                }
            } else {
                emptyList() // Team-type matches have no manual allocation
            }

            if (match.matchType == "Team") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { selectedTeamName = match.myTeam },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamName == match.myTeam) Color(0xFF212121) else Color(0xFFE0E0E0),
                            contentColor = if (selectedTeamName == match.myTeam) Color.White else Color.Black
                        )
                    ) {
                        Text(text = match.myTeam, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = { selectedTeamName = match.opponent },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeamName == match.opponent) Color(0xFF212121) else Color(0xFFE0E0E0),
                            contentColor = if (selectedTeamName == match.opponent) Color.White else Color.Black
                        )
                    ) {
                        Text(text = match.opponent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Invites",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
                    )
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val currentTeam = allTeams.find { it.name == selectedTeamName }
                val teamMembers = currentTeam?.members ?: emptyList()
                val playersToShow = allUsers.filter { teamMembers.contains(it.id) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(playersToShow) { player ->
                        val originalStatus = match.playerInvitations[player.id] ?: InvitationStatus.NO_ANSWER.name
                        val effectiveStatus = if (isPast && (originalStatus == InvitationStatus.NO_ANSWER.name || originalStatus == InvitationStatus.TENTATIVE.name)) {
                            InvitationStatus.DECLINED.name
                        } else {
                            originalStatus
                        }
                        PlayerInvitationItem(
                            player = player,
                            status = effectiveStatus,
                            currentUserId = currentUser.id,
                            onUpdateStatus = { newStatus -> onUpdateStatus(player.id, newStatus) },
                            isPast = isPast
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }

            } else {
                // Player type with Drag and Drop support
                val teamAPlayers = allUsers.filter { match.teamAPlayers.contains(it.id) }
                val teamBPlayers = allUsers.filter { match.teamBPlayers.contains(it.id) }
                // Use playerInvitations keys as the authoritative invited-player list;
                // it is always kept in sync by normalizeInvitationState on every save.
                val invitedPlayerIds = match.playerInvitations.keys
                // unallocatedPlayerIds is computed in the outer scope above
                val unallocatedPlayers = allUsers.filter {
                    it.id in invitedPlayerIds &&
                    !match.teamAPlayers.contains(it.id) &&
                    !match.teamBPlayers.contains(it.id)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Teams Structure",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
                    )
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Team A ────────────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { teamABounds = it.boundsInWindow() }
                        ) {
                            SectionHeader(
                                title = "Team A",
                                isDropTarget = draggingPlayerId != null && teamABounds?.contains(dragOffset) == true
                            )
                            teamAPlayers.forEach { player ->
                                DraggablePlayerWrapper(
                                    player = player,
                                    isDragging = draggingPlayerId == player.id,
                                    onDragStart = { draggingPlayerId = player.id; dragOffset = it },
                                    onDrag = { dragOffset += it },
                                    onDragEnd = {
                                        val currentId = draggingPlayerId
                                        if (currentId != null) {
                                            val newTeamA = match.teamAPlayers.toMutableList()
                                            val newTeamB = match.teamBPlayers.toMutableList()
                                            newTeamA.remove(currentId)
                                            newTeamB.remove(currentId)
                                            if (teamABounds?.contains(dragOffset) == true) newTeamA.add(currentId)
                                            else if (teamBBounds?.contains(dragOffset) == true) newTeamB.add(currentId)
                                            onRandomizeTeams(newTeamA, newTeamB)
                                        }
                                        draggingPlayerId = null
                                    }
                                ) {
                                    val originalStatus = match.playerInvitations[player.id] ?: InvitationStatus.NO_ANSWER.name
                                    val effectiveStatus = if (isPast && (originalStatus == InvitationStatus.NO_ANSWER.name || originalStatus == InvitationStatus.TENTATIVE.name)) {
                                        InvitationStatus.DECLINED.name
                                    } else {
                                        originalStatus
                                    }
                                    PlayerInvitationItem(
                                        player = player,
                                        status = effectiveStatus,
                                        currentUserId = currentUser.id,
                                        onUpdateStatus = { newStatus -> onUpdateStatus(player.id, newStatus) },
                                        showDragHandle = true,
                                        isPast = isPast
                                    )
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    // ── Team B ────────────────────────────────────────────────
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { teamBBounds = it.boundsInWindow() }
                        ) {
                            SectionHeader(
                                title = "Team B",
                                isDropTarget = draggingPlayerId != null && teamBBounds?.contains(dragOffset) == true
                            )
                            teamBPlayers.forEach { player ->
                                DraggablePlayerWrapper(
                                    player = player,
                                    isDragging = draggingPlayerId == player.id,
                                    onDragStart = { draggingPlayerId = player.id; dragOffset = it },
                                    onDrag = { dragOffset += it },
                                    onDragEnd = {
                                        val currentId = draggingPlayerId
                                        if (currentId != null) {
                                            val newTeamA = match.teamAPlayers.toMutableList()
                                            val newTeamB = match.teamBPlayers.toMutableList()
                                            newTeamA.remove(currentId)
                                            newTeamB.remove(currentId)
                                            if (teamABounds?.contains(dragOffset) == true) newTeamA.add(currentId)
                                            else if (teamBBounds?.contains(dragOffset) == true) newTeamB.add(currentId)
                                            onRandomizeTeams(newTeamA, newTeamB)
                                        }
                                        draggingPlayerId = null
                                    }
                                ) {
                                    val originalStatus = match.playerInvitations[player.id] ?: InvitationStatus.NO_ANSWER.name
                                    val effectiveStatus = if (isPast && (originalStatus == InvitationStatus.NO_ANSWER.name || originalStatus == InvitationStatus.TENTATIVE.name)) {
                                        InvitationStatus.DECLINED.name
                                    } else {
                                        originalStatus
                                    }
                                    PlayerInvitationItem(
                                        player = player,
                                        status = effectiveStatus,
                                        currentUserId = currentUser.id,
                                        onUpdateStatus = { newStatus -> onUpdateStatus(player.id, newStatus) },
                                        showDragHandle = true,
                                        isPast = isPast
                                    )
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    // ── Unallocated / Invites ─────────────────────────────────
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { unallocatedBounds = it.boundsInWindow() }
                        ) {
                            SectionHeader(
                                title = "Unallocated / Invites",
                                isDropTarget = draggingPlayerId != null && unallocatedBounds?.contains(dragOffset) == true
                            )
                            if (unallocatedPlayers.isEmpty()) {
                                Text(
                                    "No unallocated players",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            } else {
                                unallocatedPlayers.forEach { player ->
                                    DraggablePlayerWrapper(
                                        player = player,
                                        isDragging = draggingPlayerId == player.id,
                                        onDragStart = { draggingPlayerId = player.id; dragOffset = it },
                                        onDrag = { dragOffset += it },
                                        onDragEnd = {
                                            val currentId = draggingPlayerId
                                            if (currentId != null) {
                                                val newTeamA = match.teamAPlayers.toMutableList()
                                                val newTeamB = match.teamBPlayers.toMutableList()
                                                newTeamA.remove(currentId)
                                                newTeamB.remove(currentId)
                                                if (teamABounds?.contains(dragOffset) == true) newTeamA.add(currentId)
                                                else if (teamBBounds?.contains(dragOffset) == true) newTeamB.add(currentId)
                                                onRandomizeTeams(newTeamA, newTeamB)
                                            }
                                            draggingPlayerId = null
                                        }
                                    ) {
                                        val originalStatus = match.playerInvitations[player.id] ?: InvitationStatus.NO_ANSWER.name
                                        val effectiveStatus = if (isPast && (originalStatus == InvitationStatus.NO_ANSWER.name || originalStatus == InvitationStatus.TENTATIVE.name)) {
                                            InvitationStatus.DECLINED.name
                                        } else {
                                            originalStatus
                                        }
                                        PlayerInvitationItem(
                                            player = player,
                                            status = effectiveStatus,
                                            currentUserId = currentUser.id,
                                            onUpdateStatus = { newStatus -> onUpdateStatus(player.id, newStatus) },
                                            showDragHandle = true,
                                            isPast = isPast
                                        )
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!isPast && match.invitedPlayers.size >= 2) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    val shuffled = match.invitedPlayers.shuffled()
                                    val mid = shuffled.size / 2
                                    onRandomizeTeams(shuffled.subList(0, mid), shuffled.subList(mid, shuffled.size))
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                            ) {
                                Text("Randomize Teams", color = Color.White)
                            }
                        }
                    }
                }
            }

            if (isPast) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Text(
                        text = "Match Results",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    /*Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add results:",
                        style = MaterialTheme.typography.bodyLarge
                    )*/
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedTextField(
                                value = scoreMyTeamText,
                                onValueChange = { if (it.all { char -> char.isDigit() }) scoreMyTeamText = it },
                                modifier = Modifier.width(80.dp),
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("X", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (match.matchType == "Team") match.myTeam else "Team A",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedTextField(
                                value = scoreOpponentText,
                                onValueChange = { if (it.all { char -> char.isDigit() }) scoreOpponentText = it },
                                modifier = Modifier.width(80.dp),
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("X", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (match.matchType == "Team") match.opponent else "Team B",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    val hasUnallocated = unallocatedPlayerIds.isNotEmpty()
                    val scoresEntered = scoreMyTeamText.isNotEmpty() && scoreOpponentText.isNotEmpty()
                    val saveEnabled = !hasUnallocated && scoresEntered
                    val saveButtonText = if (hasUnallocated) "Missing team Players allocation" else "Save Results"

                    Button(
                        onClick = {
                            onSaveResults(scoreMyTeamText.toIntOrNull(), scoreOpponentText.toIntOrNull())
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        enabled = saveEnabled
                    ) {
                        Text(saveButtonText, color = Color.White)
                    }
                }
            }

            if (match.matchType == "Team" && !isPast) {
                Spacer(modifier = Modifier.weight(1f))
            }

            TextButton(
                onClick = onCancelMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("Cancel Match", fontSize = 16.sp)
            }
        }

        // Drag Overlay
        if (draggingPlayerId != null) {
            val player = allUsers.find { it.id == draggingPlayerId }
            if (player != null) {
                Surface(
                    modifier = Modifier
                        .offset { IntOffset(dragOffset.x.roundToInt() - 100, dragOffset.y.roundToInt() - 25) }
                        .size(200.dp, 50.dp)
                        .zIndex(1000f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.LightGray.copy(alpha = 0.9f),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(player.name ?: player.email, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    isDropTarget: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDropTarget) Color(0xFFE3F2FD) else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDropTarget) Color(0xFF1976D2) else Color.Gray
        )
    }
}

@Composable
private fun DraggablePlayerWrapper(
    player: UserProfile,
    isDragging: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    content: @Composable () -> Unit
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { itemPosition = it.positionInWindow() }
            .alpha(if (isDragging) 0.3f else 1.0f)
            .pointerInput(player.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> onDragStart(itemPosition + offset) },
                    onDrag = { change, dragAmount -> 
                        change.consume()
                        onDrag(dragAmount) 
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            }
    ) {
        content()
    }
}

@Composable
private fun PlayerInvitationItem(
    player: UserProfile,
    status: String,
    currentUserId: String,
    onUpdateStatus: (InvitationStatus) -> Unit,
    showDragHandle: Boolean = false,
    isPast: Boolean = false
) {
    var isEditing by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDragHandle) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        val isMe = player.id == currentUserId
        val displayName = if (isMe) "${player.name ?: player.email} (you)" else player.name ?: player.email
        
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
            modifier = Modifier.weight(1f)
        )

        if (isEditing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tentative Icon - Only show if not past
                if (!isPast) {
                    Icon(
                        imageVector = if (status == InvitationStatus.TENTATIVE.name) Icons.Default.Help else Icons.Default.HelpOutline,
                        contentDescription = "Tentative",
                        tint = Color(0xFFFFA000),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { 
                                onUpdateStatus(InvitationStatus.TENTATIVE)
                                isEditing = false
                            }
                    )
                }
                
                // Declined Icon
                Icon(
                    imageVector = if (status == InvitationStatus.DECLINED.name) Icons.Default.Cancel else Icons.Default.HighlightOff,
                    contentDescription = "Declined",
                    tint = Color(0xFFF44336),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { 
                            onUpdateStatus(InvitationStatus.DECLINED)
                            isEditing = false
                        }
                )
                // Accepted Icon
                Icon(
                    imageVector = if (status == InvitationStatus.ACCEPTED.name) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                    contentDescription = "Accepted",
                    tint = Color(0xFF00897B),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { 
                            onUpdateStatus(InvitationStatus.ACCEPTED)
                            isEditing = false
                        }
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isMe && !isPast) {
                    Text(
                        text = "Edit Status",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF3F51B5),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable { isEditing = true }
                            .padding(horizontal = 8.dp)
                    )
                }

                if (status == InvitationStatus.NO_ANSWER.name) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFBDBDBD))
                                .clickable { isEditing = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "No Answer",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    val (icon, color) = when(status) {
                        InvitationStatus.ACCEPTED.name -> Icons.Default.CheckCircle to Color(0xFF00897B)
                        InvitationStatus.DECLINED.name -> Icons.Default.Cancel to Color(0xFFF44336)
                        InvitationStatus.TENTATIVE.name -> Icons.Default.Help to Color(0xFFFFA000)
                        else -> Icons.Default.MoreHoriz to Color.Gray
                    }
                    
                    Icon(
                        imageVector = icon,
                        contentDescription = status,
                        tint = color,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { isEditing = true }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MatchDetailsScreenPreview() {
    RankUpTheme {
        MatchDetailsScreen(
            match = PlannedMatch(
                modality = "Football",
                dateTime = "12.09.2025",
                location = "Campo Futebol Bosch, Aveiro",
                myTeam = "Team Yellow",
                opponent = "Team Green",
                matchType = "Team"
            ),
            allUsers = emptyList(),
            allTeams = emptyList(),
            currentUser = UserProfile(id = "me", name = "Me"),
            onBack = {},
            onEdit = {},
            onSaveResults = { _, _ -> },
            onRandomizeTeams = { _, _ -> },
            onCancelMatch = {},
            onUpdateStatus = { _, _ -> }
        )
    }
}
