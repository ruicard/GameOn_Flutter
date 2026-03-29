package com.example.rankup.ui.screens

import android.Manifest
import android.location.Geocoder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rankup.PlannedMatch
import com.example.rankup.PlannedTeam
import com.example.rankup.UserProfile
import com.example.rankup.data.AgeGroup
import com.example.rankup.data.Gender
import com.example.rankup.ui.theme.RankUpTheme
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class MatchOutcome { WIN, LOSS, DRAW, UNKNOWN }

private fun resolveOutcome(
    match: PlannedMatch,
    userId: String,
    allTeams: List<PlannedTeam>
): MatchOutcome {
    val a = match.scoreMyTeam ?: return MatchOutcome.UNKNOWN
    val b = match.scoreOpponent ?: return MatchOutcome.UNKNOWN
    if (!match.resultsConfirmed) return MatchOutcome.UNKNOWN

    val saver = match.resultsSavedByUserId
    // Determine if user is on the same side as the result saver
    val userOnSaverSide = when (match.matchType) {
        "Player" -> {
            val saverInA = match.teamAPlayers.contains(saver)
            val saverInB = match.teamBPlayers.contains(saver)
            val userInA = match.teamAPlayers.contains(userId)
            val userInB = match.teamBPlayers.contains(userId)
            (saverInA && userInA) || (saverInB && userInB)
        }
        "Team" -> {
            val myTeamMembers = allTeams.find { it.name == match.myTeam }?.members ?: emptyList()
            val opponentMembers = allTeams.find { it.name == match.opponent }?.members ?: emptyList()
            val saverInMy = myTeamMembers.contains(saver)
            val saverInOpp = opponentMembers.contains(saver)
            val userInMy = myTeamMembers.contains(userId)
            val userInOpp = opponentMembers.contains(userId)
            (saverInMy && userInMy) || (saverInOpp && userInOpp)
        }
        else -> saver == userId || match.createdByUserId == userId
    }
    val userScore = if (userOnSaverSide) a else b
    val oppScore  = if (userOnSaverSide) b else a
    return when {
        userScore > oppScore -> MatchOutcome.WIN
        userScore < oppScore -> MatchOutcome.LOSS
        else                 -> MatchOutcome.DRAW
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    userProfile: UserProfile?,
    onUpdateClick: (String, Gender, AgeGroup, String) -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    plannedMatches: List<PlannedMatch> = emptyList(),
    allTeams: List<PlannedTeam> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (userProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onSignInClick) { Text("Sign in with Google") }
        }
        return
    }

    val context = LocalContext.current

    var username by remember { mutableStateOf(userProfile.username) }
    var city     by remember { mutableStateOf(userProfile.city) }
    var gender by remember {
        mutableStateOf(try { Gender.valueOf(userProfile.gender) } catch (_: Exception) { Gender.UNKNOWN })
    }
    var ageGroup by remember {
        mutableStateOf(try { AgeGroup.valueOf(userProfile.ageGroup) } catch (_: Exception) { AgeGroup.SENIOR_18_45 })
    }
    var genderExpanded by remember { mutableStateOf(false) }
    var ageExpanded    by remember { mutableStateOf(false) }

    // Location — same pattern as CreateTeamScreen
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(it.latitude, it.longitude, 1) { addresses ->
                                if (addresses.isNotEmpty()) city = addresses[0].locality ?: ""
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                            if (!addresses.isNullOrEmpty()) city = addresses[0].locality ?: ""
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("AccountScreen", "Location permission denied: ${e.message}")
            }
        }
    }

    // Past matches the current user was involved in, newest first
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val displaySdf = remember { SimpleDateFormat("d MMM ''yy", Locale.getDefault()) }
    val now = remember { Calendar.getInstance().time }
    val pastMatches = remember(plannedMatches, userProfile.id) {
        plannedMatches
            .filter { match ->
                val involved = match.createdByUserId == userProfile.id ||
                               match.invitedPlayers.contains(userProfile.id)
                val isPast = try { sdf.parse(match.dateTime)?.before(now) == true } catch (_: Exception) { false }
                involved && isPast
            }
            .sortedByDescending { try { sdf.parse(it.dateTime)?.time ?: 0L } catch (_: Exception) { 0L } }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Profile picture
        if (userProfile.profilePictureUrl != null) {
            AsyncImage(
                model = userProfile.profilePictureUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(80.dp).clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(80.dp),
                tint = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = userProfile.name ?: userProfile.username,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        Text(
            text = userProfile.email,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 13.sp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Rank Points Card ──────────────────────────────────
        val displayPoints = if (userProfile.rankPoints <= 0) 100 else userProfile.rankPoints
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Rank",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Rank Points",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                        Text(
                            text = "$displayPoints pts",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                // Mini legend
                Column(horizontalAlignment = Alignment.End) {
                    Text("Win +3 · Draw +2", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f))
                    Text("Loss −1 · Miss −3", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Username Field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gender Dropdown
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = gender.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                Gender.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.name) }, onClick = { gender = option; genderExpanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Age Group Dropdown
        ExposedDropdownMenuBox(
            expanded = ageExpanded,
            onExpandedChange = { ageExpanded = !ageExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = ageGroup.name.replace("_", " "),
                onValueChange = {},
                readOnly = true,
                label = { Text("Age Group") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ageExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(expanded = ageExpanded, onDismissRequest = { ageExpanded = false }) {
                AgeGroup.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name.replace("_", " ")) },
                        onClick = { ageGroup = option; ageExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // City Field — tapping the location icon requests permission then auto-fills city
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City") },
            trailingIcon = {
                IconButton(onClick = {
                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Get current city")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onUpdateClick(username, gender, ageGroup, city) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("Update Profile")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSignOutClick) {
            Text("Sign Out", color = Color.Red)
        }

        // ── Match History ─────────────────────────────────────
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Match History",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (pastMatches.isNotEmpty()) {
                Text(
                    text = "${pastMatches.size} match${if (pastMatches.size != 1) "es" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (pastMatches.isEmpty()) {
            Text(
                text = "No matches played yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pastMatches.forEach { match ->
                    MatchHistoryRow(
                        match = match,
                        userId = userProfile.id,
                        allTeams = allTeams,
                        displayDate = try { displaySdf.format(sdf.parse(match.dateTime)!!) } catch (_: Exception) { match.dateTime }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MatchHistoryRow(
    match: PlannedMatch,
    userId: String,
    allTeams: List<PlannedTeam>,
    displayDate: String
) {
    val outcome = resolveOutcome(match, userId, allTeams)
    val (badgeColor, badgeLabel) = when (outcome) {
        MatchOutcome.WIN     -> Color(0xFF2E7D32) to "W"
        MatchOutcome.LOSS    -> Color(0xFFC62828) to "L"
        MatchOutcome.DRAW    -> Color(0xFF757575) to "D"
        MatchOutcome.UNKNOWN -> Color(0xFFBDBDBD) to "—"
    }

    val opponentLabel = when (match.matchType) {
        "Team"   -> "${match.myTeam} vs ${match.opponent}"
        else     -> "Player Match"
    }
    val scoreLabel = if (match.scoreMyTeam != null && match.scoreOpponent != null)
        "${match.scoreMyTeam} – ${match.scoreOpponent}" else ""

    val sportIcon = sportIcon(match.modality)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sport icon
            Icon(
                imageVector = sportIcon,
                contentDescription = match.modality,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.modality,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = opponentLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Score + badge
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                if (scoreLabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = scoreLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {
    RankUpTheme {
        AccountScreen(
            userProfile = UserProfile(
                id = "u1",
                name = "Rui Cardoso",
                email = "rui@example.com",
                profilePictureUrl = null,
                username = "ruicardoso",
                gender = Gender.MALE.name,
                ageGroup = AgeGroup.SENIOR_18_45.name,
                city = "Porto"
            ),
            onUpdateClick = { _, _, _, _ -> },
            onSignInClick = {},
            onSignOutClick = {},
            plannedMatches = listOf(
                PlannedMatch(
                    modality = "Football", matchType = "Team",
                    dateTime = "10/01/2026 18:00",
                    myTeam = "Team HackYou", opponent = "Dream Team",
                    scoreMyTeam = 3, scoreOpponent = 1,
                    resultsSavedByUserId = "u1", resultsConfirmed = true,
                    createdByUserId = "u1", teamAPlayers = listOf("u1")
                ),
                PlannedMatch(
                    modality = "Padel", matchType = "Player",
                    dateTime = "20/02/2026 10:00",
                    scoreMyTeam = 1, scoreOpponent = 2,
                    resultsSavedByUserId = "u2", resultsConfirmed = true,
                    createdByUserId = "u2", teamAPlayers = listOf("u2"), teamBPlayers = listOf("u1"),
                    invitedPlayers = listOf("u1")
                )
            )
        )
    }
}
