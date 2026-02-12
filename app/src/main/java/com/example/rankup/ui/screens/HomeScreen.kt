package com.example.rankup.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rankup.InvitationStatus
import com.example.rankup.PlannedMatch
import com.example.rankup.PlannedTeam
import com.example.rankup.UserProfile
import com.example.rankup.ui.theme.RankUpTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    plannedMatches: List<PlannedMatch>,
    allTeams: List<PlannedTeam>,
    isRefreshing: Boolean,
    onPlanMatchClick: () -> Unit,
    onSignInClick: () -> Unit,
    onMatchClick: (PlannedMatch) -> Unit,
    onRefresh: () -> Unit,
    onUpdateStatus: (PlannedMatch, InvitationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = Calendar.getInstance().time
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    val (pastMatches, upcomingMatches) = plannedMatches.partition {
        try {
            val matchDate = sdf.parse(it.dateTime)
            matchDate?.before(now) ?: false
        } catch (e: Exception) {
            false
        }
    }

    val sortedUpcoming = upcomingMatches.sortedBy { 
        try { sdf.parse(it.dateTime)?.time ?: Long.MAX_VALUE } catch (e: Exception) { Long.MAX_VALUE }
    }
    
    val sortedPast = pastMatches.sortedByDescending { 
        try { sdf.parse(it.dateTime)?.time ?: 0L } catch (e: Exception) { 0L }
    }

    val pendingInvitations = sortedUpcoming.filter { match ->
        userProfile != null && match.playerInvitations[userProfile.id] == InvitationStatus.NO_ANSWER.name
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (userProfile == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
                    Text("Hello there,", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                    Text("Sign in to continue", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                }
                Surface(modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(1.5f), shape = RoundedCornerShape(32.dp), color = Color(0xFFE0E0E0)) {}
                Spacer(modifier = Modifier.height(100.dp))
                OutlinedButton(
                    onClick = onSignInClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "G ", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text(text = "Sign in with Google", color = Color.Black, fontSize = 18.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                if (sortedUpcoming.isEmpty() && pendingInvitations.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillParentMaxHeight(0.7f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?q=80&w=1000&auto=format&fit=crop",
                                contentDescription = "Stadium",
                                modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(32.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                            Text("Plan your first match", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Catchy sentence here, bla bla bla bla\nbla bla bla bla bla bla bla bla bla bla", style = MaterialTheme.typography.bodyLarge, color = Color.DarkGray, textAlign = TextAlign.Center, lineHeight = 24.sp)
                            Spacer(modifier = Modifier.height(48.dp))
                            Button(onClick = onPlanMatchClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))) {
                                Text("Plan Match", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                } else {
                    if (sortedUpcoming.isNotEmpty()) {
                        item {
                            UpcomingMatchCard(sortedUpcoming.first(), onClick = { onMatchClick(sortedUpcoming.first()) })
                        }
                    }

                    if (pendingInvitations.isNotEmpty()) {
                        items(pendingInvitations) { match ->
                            InvitationPendingCard(
                                match = match,
                                onClick = { onMatchClick(match) },
                                onUpdateStatus = { status -> onUpdateStatus(match, status) }
                            )
                        }
                    }

                    if (sortedUpcoming.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text("Next matches", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                    items(sortedUpcoming) { match ->
                                        NextMatchCard(match, allTeams, onClick = { onMatchClick(match) })
                                    }
                                }
                            }
                        }
                    }
                }

                if (sortedPast.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Previous matches", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                items(sortedPast) { match -> 
                                    PreviousMatchCard(match, allTeams, onClick = { onMatchClick(match) }) 
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Plan a match", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(12.dp))
                    InviteFriendsCard(onPlanMatchClick)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun UpcomingMatchCard(match: PlannedMatch, onClick: () -> Unit) {
    val calendar = Calendar.getInstance()
    var dayOfWeek = "---"
    var dayOfMonth = "--"
    try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = sdf.parse(match.dateTime)
        if (date != null) {
            calendar.time = date
            dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(date).uppercase()
            dayOfMonth = SimpleDateFormat("dd", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {}
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), 
        shape = RoundedCornerShape(24.dp), 
        colors = CardDefaults.cardColors(containerColor = Color.White), 
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayOfWeek, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(dayOfMonth, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Upcoming match • ${match.modality}", 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(match.location, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    }
}

@Composable
fun InvitationPendingCard(
    match: PlannedMatch,
    onClick: () -> Unit,
    onUpdateStatus: (InvitationStatus) -> Unit
) {
    val calendar = Calendar.getInstance()
    var dayOfWeek = "---"
    var dayOfMonth = "--"
    var monthAbbr = "---"
    var timeStr = "--:--"
    try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = sdf.parse(match.dateTime)
        if (date != null) {
            calendar.time = date
            dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(date).uppercase()
            dayOfMonth = SimpleDateFormat("dd", Locale.getDefault()).format(date)
            monthAbbr = SimpleDateFormat("MMM", Locale.getDefault()).format(date).uppercase()
            timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {}

    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE91E63))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayOfWeek, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(dayOfMonth, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Invitation pending",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${match.modality} - $dayOfMonth $monthAbbr - $timeStr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Text(match.location, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            
            if (isEditing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Tentative",
                        tint = Color(0xFFFFA000),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onUpdateStatus(InvitationStatus.TENTATIVE) }
                    )
                    Icon(
                        imageVector = Icons.Default.HighlightOff,
                        contentDescription = "Declined",
                        tint = Color(0xFFF44336),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onUpdateStatus(InvitationStatus.DECLINED) }
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = "Accepted",
                        tint = Color(0xFF00897B),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onUpdateStatus(InvitationStatus.ACCEPTED) }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFBDBDBD))
                        .clickable { isEditing = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Pending",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NextMatchCard(match: PlannedMatch, allTeams: List<PlannedTeam>, onClick: () -> Unit) {
    var formattedDate = match.dateTime
    try {
        val sdfInput = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = sdfInput.parse(match.dateTime)
        if (date != null) formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) {}

    if (match.matchType == "Team") {
        val myTeamObj = allTeams.find { it.name == match.myTeam }
        val opponentObj = allTeams.find { it.name == match.opponent }

        Card(
            modifier = Modifier.width(220.dp).height(180.dp).clickable(onClick = onClick), 
            shape = RoundedCornerShape(24.dp), 
            colors = CardDefaults.cardColors(containerColor = Color.White), 
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(match.modality, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(formattedDate, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TeamAvatar(match.myTeam, Color(0xFFFFE082), modifier = Modifier.weight(1f), imageUrl = myTeamObj?.profilePictureUrl)
                    TeamAvatar(match.opponent, Color(0xFFA5D6A7), modifier = Modifier.weight(1f), imageUrl = opponentObj?.profilePictureUrl)
                }
            }
        }
    } else {
        // Player Type Match Card - Matches Team box size
        Card(
            modifier = Modifier.width(220.dp).height(180.dp).clickable(onClick = onClick), 
            shape = RoundedCornerShape(24.dp), 
            colors = CardDefaults.cardColors(containerColor = Color.White), 
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(match.modality, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(formattedDate, fontSize = 12.sp, color = Color.Gray)
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                val invitations = match.playerInvitations
                val accepted = invitations.values.count { it == InvitationStatus.ACCEPTED.name }
                val declined = invitations.values.count { it == InvitationStatus.DECLINED.name }
                val noAnswer = invitations.values.count { it == InvitationStatus.NO_ANSWER.name }
                val tentative = invitations.values.count { it == InvitationStatus.TENTATIVE.name }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatusIndicator(count = accepted, label = "accepted", color = Color(0xFF00897B), icon = Icons.Default.Check)
                        StatusIndicator(count = declined, label = "declined", color = Color(0xFFF44336), icon = Icons.Default.Close)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatusIndicator(count = noAnswer, label = "no answer", color = Color.Gray, icon = Icons.Default.MoreHoriz)
                        StatusIndicator(count = tentative, label = "tentative", color = Color(0xFFFFA000), icon = Icons.Default.HelpOutline)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(count: Int, label: String, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE0E0E0)))
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(text = "$count $label", fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PreviousMatchCard(match: PlannedMatch, allTeams: List<PlannedTeam>, onClick: () -> Unit) {
    var formattedDate = match.dateTime
    try {
        val sdfInput = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = sdfInput.parse(match.dateTime)
        if (date != null) formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) {}
    
    val missingResults = match.scoreMyTeam == null || match.scoreOpponent == null
    val myTeamObj = allTeams.find { it.name == match.myTeam }
    val opponentObj = allTeams.find { it.name == match.opponent }

    Column(modifier = Modifier.width(220.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp).clickable(onClick = onClick), 
            shape = RoundedCornerShape(24.dp), 
            colors = CardDefaults.cardColors(containerColor = Color.White), 
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = if (missingResults) BorderStroke(1.dp, Color(0xFFE91E63)) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(match.modality, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(formattedDate, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    TeamAvatar(match.myTeam, Color(0xFFBDBDBD), modifier = Modifier.weight(1f), showMore = false, score = match.scoreMyTeam, imageUrl = myTeamObj?.profilePictureUrl)
                    Text("vs", modifier = Modifier.padding(horizontal = 4.dp), color = Color.Gray, fontSize = 12.sp)
                    TeamAvatar(match.opponent, Color(0xFFBDBDBD), modifier = Modifier.weight(1f), showMore = false, score = match.scoreOpponent, imageUrl = opponentObj?.profilePictureUrl)
                }
            }
        }
        if (missingResults) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                Icon(
                    imageVector = Icons.Default.Error, 
                    contentDescription = null, 
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Missing match results", 
                    color = Color.Black, 
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun TeamAvatar(name: String, color: Color, modifier: Modifier = Modifier, showMore: Boolean = true, score: Int? = null, imageUrl: String? = null) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (score != null) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (imageUrl != null) 0.4f else 0f)), contentAlignment = Alignment.Center) {
                        Text(text = score.toString(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = if (imageUrl != null) Color.White else Color.Black)
                    }
                }
            }
            if (showMore) {
                Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.BottomEnd) {
                    Surface(modifier = Modifier.size(24.dp).clip(CircleShape), color = Color.Gray.copy(alpha = 0.8f)) {
                        Icon(Icons.Default.MoreHoriz, null, tint = Color.White, modifier = Modifier.padding(2.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun InviteFriendsCard(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SentimentSatisfiedAlt, null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Invite your friends for a match", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}
