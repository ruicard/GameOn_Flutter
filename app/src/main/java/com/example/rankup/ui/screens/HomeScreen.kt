package com.example.rankup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rankup.PlannedMatch
import com.example.rankup.UserProfile
import com.example.rankup.ui.theme.RankUpTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    plannedMatches: List<PlannedMatch>,
    onPlanMatchClick: () -> Unit,
    onSignInClick: () -> Unit,
    onMatchClick: (PlannedMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val sortedMatches = plannedMatches.sortedBy { 
        try { sdf.parse(it.dateTime)?.time ?: Long.MAX_VALUE } catch (e: Exception) { Long.MAX_VALUE }
    }

    if (userProfile == null) {
        Column(
            modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
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
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "G ", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(text = "Sign in with Google", color = Color.Black, fontSize = 18.sp)
                }
            }
        }
    } else if (sortedMatches.isNotEmpty()) {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { 
                Spacer(modifier = Modifier.height(8.dp))
                UpcomingMatchCard(sortedMatches.first(), onClick = { onMatchClick(sortedMatches.first()) }) 
            }
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Next matches", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        items(sortedMatches) { match -> 
                            NextMatchCard(match, onClick = { onMatchClick(match) }) 
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
    } else {
        Column(
            modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
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
                Text("Upcoming match", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(match.location, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    }
}

@Composable
fun NextMatchCard(match: PlannedMatch, onClick: () -> Unit) {
    var formattedDate = match.dateTime
    try {
        val sdfInput = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = sdfInput.parse(match.dateTime)
        if (date != null) formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) {}
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
                TeamAvatar(match.myTeam, Color(0xFFFFE082), modifier = Modifier.weight(1f))
                TeamAvatar(match.opponent, Color(0xFFA5D6A7), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TeamAvatar(name: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(color))
            Surface(modifier = Modifier.size(24.dp).clip(CircleShape), color = Color.Gray.copy(alpha = 0.8f)) {
                Icon(Icons.Default.MoreHoriz, null, tint = Color.White, modifier = Modifier.padding(2.dp))
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

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    RankUpTheme {
        HomeScreen(
            userProfile = UserProfile(id = UUID.randomUUID().toString(), name = "Rui Cardoso", email = "rui@example.com", profilePictureUrl = null),
            plannedMatches = listOf(PlannedMatch(modality = "Football", dateTime = "12/09/2025 18:00", location = "Campo Futebol Bosch", myTeam = "Team HackYou", opponent = "Team Badass")),
            onPlanMatchClick = {}, onSignInClick = {}, onMatchClick = {}
        )
    }
}
