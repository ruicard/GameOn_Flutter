package com.example.rankup.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.rankup.SportModel
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
    availableSports: List<SportModel>,
    isRefreshing: Boolean,
    onPlanMatchClick: () -> Unit,
    onSignInClick: () -> Unit,
    onMatchClick: (PlannedMatch) -> Unit,
    onRefresh: () -> Unit,
    onUpdateStatus: (PlannedMatch, InvitationStatus) -> Unit,
    modifier: Modifier = Modifier,
    allUsers: List<UserProfile> = emptyList(), // Added missing parameter from MainActivity usage
    onUpdateMatch: (PlannedMatch) -> Unit = {} // Added missing parameter from MainActivity usage
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
                    if (pendingInvitations.isNotEmpty()) {
                        item {
                            InvitationsPager(
                                invitations = pendingInvitations,
                                allUsers = allUsers,
                                onClick = onMatchClick,
                                onUpdateStatus = onUpdateStatus
                            )
                        }
                    } else if (sortedUpcoming.isNotEmpty()) {
                        item {
                            UpcomingMatchCard(sortedUpcoming.first(), onClick = { onMatchClick(sortedUpcoming.first()) })
                        }
                    }

                    if (sortedUpcoming.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text("Next matches", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                    items(sortedUpcoming) { match ->
                                        NextMatchCard(match, allTeams, availableSports, onClick = { onMatchClick(match) })
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
    invitedByName: String,
    onClick: () -> Unit,
    onUpdateStatus: (InvitationStatus) -> Unit
) {
    var formattedDate = "--.--.----"
    try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = sdf.parse(match.dateTime)
        if (date != null) {
            formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {}

    var selectedStatus by remember(match.id) { mutableStateOf<InvitationStatus?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE91E63))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Invite pending - ${match.modality}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Tentative
                    val tentativeSelected = selectedStatus == InvitationStatus.TENTATIVE
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (tentativeSelected) Color(0xFFFFA000) else Color.Transparent)
                            .border(1.5.dp, Color(0xFFFFA000), CircleShape)
                            .clickable { selectedStatus = InvitationStatus.TENTATIVE },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (tentativeSelected) Color.White else Color(0xFFFFA000)
                            )
                        )
                    }
                    // Decline
                    val declineSelected = selectedStatus == InvitationStatus.DECLINED
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (declineSelected) Color(0xFFE91E63) else Color.Transparent)
                            .border(1.5.dp, Color(0xFFE91E63), CircleShape)
                            .clickable { selectedStatus = InvitationStatus.DECLINED },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Decline",
                            tint = if (declineSelected) Color.White else Color(0xFFE91E63),
                            modifier = Modifier.size(20.dp))
                    }
                    // Accept
                    val acceptSelected = selectedStatus == InvitationStatus.ACCEPTED
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (acceptSelected) Color(0xFF009688) else Color.Transparent)
                            .border(1.5.dp, Color(0xFF009688), CircleShape)
                            .clickable { selectedStatus = InvitationStatus.ACCEPTED },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Accept",
                            tint = if (acceptSelected) Color.White else Color(0xFF009688),
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Invited by $invitedByName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(formattedDate, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(match.location, style = MaterialTheme.typography.bodyMedium)
                }
                if (selectedStatus != null) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3F51B5)
                        ),
                        modifier = Modifier.clickable { onUpdateStatus(selectedStatus!!) }
                    )
                }
            }
        }
    }
}

@Composable
fun InvitationsPager(
    invitations: List<PlannedMatch>,
    allUsers: List<UserProfile>,
    onClick: (PlannedMatch) -> Unit,
    onUpdateStatus: (PlannedMatch, InvitationStatus) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { invitations.size })
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val match = invitations[page]
            val invitedByName = allUsers.find { it.id == match.createdByUserId }?.let { user ->
                user.name?.takeIf { it.isNotBlank() }
                    ?: user.username.takeIf { it.isNotBlank() }
                    ?: user.email
            } ?: "Unknown player"
            InvitationPendingCard(
                match = match,
                invitedByName = invitedByName,
                onClick = { onClick(match) },
                onUpdateStatus = { status -> onUpdateStatus(match, status) }
            )
        }
        if (invitations.size > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(invitations.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) Color(0xFF3F51B5)
                                else Color(0xFFBDBDBD)
                            )
                    )
                }
            }
        }
    }
}


@Composable
fun NextMatchCard(match: PlannedMatch, allTeams: List<PlannedTeam>, availableSports: List<SportModel>, onClick: () -> Unit) {
    var formattedDate = match.dateTime
    try {
        val sdfInput = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = sdfInput.parse(match.dateTime)
        if (date != null) formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) {}

    if (match.matchType == "Team") {
        val myTeamObj = allTeams.find { it.name == match.myTeam }
        val opponentObj = allTeams.find { it.name == match.opponent }
        val sportObj = availableSports.find { it.name == match.modality }
        val minPlayersPerMatch = sportObj?.minPlayersMatch ?: 4
        val requiredPerTeam = minPlayersPerMatch / 2

        val myTeamAccepted = myTeamObj?.members?.count { match.playerInvitations[it] == InvitationStatus.ACCEPTED.name } ?: 0
        val myTeamDeclined = myTeamObj?.members?.count { match.playerInvitations[it] == InvitationStatus.DECLINED.name } ?: 0
        val myTeamTentative = myTeamObj?.members?.count { match.playerInvitations[it] == InvitationStatus.TENTATIVE.name } ?: 0
        val myTeamTotal = myTeamObj?.members?.size ?: 0

        val opponentAccepted = opponentObj?.members?.count { match.playerInvitations[it] == InvitationStatus.ACCEPTED.name } ?: 0
        val opponentDeclined = opponentObj?.members?.count { match.playerInvitations[it] == InvitationStatus.DECLINED.name } ?: 0
        val opponentTentative = opponentObj?.members?.count { match.playerInvitations[it] == InvitationStatus.TENTATIVE.name } ?: 0
        val opponentTotal = opponentObj?.members?.size ?: 0

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
                    //Text(minPlayersPerMatch.toString(), fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TeamAvatar(
                        name = match.myTeam,
                        color = Color(0xFFFFE082),
                        modifier = Modifier.weight(1f),
                        imageUrl = myTeamObj?.profilePictureUrl,
                        isAccepted = myTeamAccepted >= requiredPerTeam,
                        isDeclined = myTeamDeclined > (myTeamTotal - requiredPerTeam),
                        isTentative = myTeamTentative > (myTeamTotal - requiredPerTeam)
                    )
                    TeamAvatar(
                        name = match.opponent,
                        color = Color(0xFFA5D6A7),
                        modifier = Modifier.weight(1f),
                        imageUrl = opponentObj?.profilePictureUrl,
                        isAccepted = opponentAccepted >= requiredPerTeam,
                        isDeclined = opponentDeclined > (opponentTotal - requiredPerTeam),
                        isTentative = opponentTentative > (opponentTotal - requiredPerTeam)
                    )
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
                    Text(match.modality, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(formattedDate, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(20.dp))
                
                if (match.matchType == "Player") {
                    // Roll back to two circles and team names below
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(Color(0xFFBDBDBD)), contentAlignment = Alignment.Center) {
                                Text(text = (match.scoreMyTeam ?: 0).toString(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = if (match.myTeam.isNotEmpty()) match.myTeam else "Team A", fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(Color(0xFFBDBDBD)), contentAlignment = Alignment.Center) {
                                Text(text = (match.scoreOpponent ?: 0).toString(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = if (match.opponent.isNotEmpty()) match.opponent else "Team B", fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        TeamAvatar(match.myTeam, Color(0xFFBDBDBD), modifier = Modifier.weight(1f), showMore = false, score = match.scoreMyTeam, imageUrl = myTeamObj?.profilePictureUrl)
                        Text("vs", modifier = Modifier.padding(horizontal = 4.dp), color = Color.Gray, fontSize = 12.sp)
                        TeamAvatar(match.opponent, Color(0xFFBDBDBD), modifier = Modifier.weight(1f), showMore = false, score = match.scoreOpponent, imageUrl = opponentObj?.profilePictureUrl)
                    }
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
fun TeamAvatar(name: String, color: Color, modifier: Modifier = Modifier, showMore: Boolean = true, score: Int? = null, imageUrl: String? = null, isAccepted: Boolean = false, isDeclined: Boolean = false, isTentative: Boolean = false) {
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
            if (isAccepted) {
                Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.BottomEnd) {
                    Surface(modifier = Modifier.size(24.dp).clip(CircleShape), color = Color(0xFF00897B)) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                    }
                }
            } else if (isDeclined) {
                Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.BottomEnd) {
                    Surface(modifier = Modifier.size(24.dp).clip(CircleShape), color = Color(0xFFF44336)) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                    }
                }
            } else if (isTentative) {
                Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.BottomEnd) {
                    Surface(modifier = Modifier.size(24.dp).clip(CircleShape), color = Color(0xFFFFA000)) {
                        Icon(Icons.Default.HelpOutline, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                    }
                }
            } else if (showMore) {
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
