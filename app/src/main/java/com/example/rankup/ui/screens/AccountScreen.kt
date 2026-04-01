package com.example.rankup.ui.screens

import android.Manifest
import android.location.Geocoder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

// ── Helpers ──────────────────────────────────────────────────────────────────

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
    val userOnSaverSide = when (match.matchType) {
        "Player" -> {
            val saverInA = match.teamAPlayers.contains(saver)
            val saverInB = match.teamBPlayers.contains(saver)
            val userInA  = match.teamAPlayers.contains(userId)
            val userInB  = match.teamBPlayers.contains(userId)
            (saverInA && userInA) || (saverInB && userInB)
        }
        "Team" -> {
            val myTeamMembers  = allTeams.find { it.name == match.myTeam }?.members  ?: emptyList()
            val oppMembers     = allTeams.find { it.name == match.opponent }?.members ?: emptyList()
            val saverInMy  = myTeamMembers.contains(saver);  val saverInOpp = oppMembers.contains(saver)
            val userInMy   = myTeamMembers.contains(userId); val userInOpp  = oppMembers.contains(userId)
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

private fun AgeGroup.chipLabel(): String = when (this) {
    AgeGroup.JUNIOR_U18     -> "Under 18"
    AgeGroup.SENIOR_18_45   -> "18 – 45"
    AgeGroup.VETERAN_45PLUS -> "45 +"
}

private fun Gender.chipLabel(): String = name
    .lowercase()
    .replaceFirstChar { it.uppercaseChar() }

// ── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountScreen(
    userProfile: UserProfile?,
    onUpdateClick: (String, Gender, AgeGroup, String) -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    plannedMatches: List<PlannedMatch> = emptyList(),
    allTeams: List<PlannedTeam> = emptyList(),
    allUsers: List<UserProfile> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (userProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onSignInClick) { Text("Sign in with Google") }
        }
        return
    }

    val context = LocalContext.current

    // ── Edit-mode state ──────────────────────────────────────
    var isEditing by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(userProfile.username) }
    var city     by remember { mutableStateOf(userProfile.city) }
    var gender   by remember {
        mutableStateOf(try { Gender.valueOf(userProfile.gender) } catch (_: Exception) { Gender.UNKNOWN })
    }
    var ageGroup by remember {
        mutableStateOf(try { AgeGroup.valueOf(userProfile.ageGroup) } catch (_: Exception) { AgeGroup.SENIOR_18_45 })
    }
    var genderExpanded by remember { mutableStateOf(false) }
    var ageExpanded    by remember { mutableStateOf(false) }

    // ── Location picker ──────────────────────────────────────
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

    // ── Derived data ─────────────────────────────────────────
    val sdf        = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val displaySdf = remember { SimpleDateFormat("d MMM ''yy",      Locale.getDefault()) }
    val now        = remember { Calendar.getInstance().time }

    val pastMatches = remember(plannedMatches, userProfile.id) {
        plannedMatches
            .filter { m ->
                val involved = m.createdByUserId == userProfile.id || m.invitedPlayers.contains(userProfile.id)
                val isPast   = try { sdf.parse(m.dateTime)?.before(now) == true } catch (_: Exception) { false }
                involved && isPast
            }
            .sortedByDescending { try { sdf.parse(it.dateTime)?.time ?: 0L } catch (_: Exception) { 0L } }
    }

    val sortedUsers = remember(allUsers) {
        allUsers.sortedByDescending { it.glickoRating }
    }

    val rankPosition = remember(sortedUsers, userProfile.id) {
        val idx = sortedUsers.indexOfFirst { it.id == userProfile.id }
        if (idx == -1) null else idx + 1
    }

    // Players immediately above and below the current user in the ranking
    val playerAbove = remember(sortedUsers, rankPosition) {
        if (rankPosition != null && rankPosition > 1) sortedUsers[rankPosition - 2] else null
    }
    val playerBelow = remember(sortedUsers, rankPosition) {
        if (rankPosition != null && rankPosition < sortedUsers.size) sortedUsers[rankPosition] else null
    }

    val wins   = remember(pastMatches) { pastMatches.count { resolveOutcome(it, userProfile.id, allTeams) == MatchOutcome.WIN  } }
    val draws  = remember(pastMatches) { pastMatches.count { resolveOutcome(it, userProfile.id, allTeams) == MatchOutcome.DRAW } }
    val losses = remember(pastMatches) { pastMatches.count { resolveOutcome(it, userProfile.id, allTeams) == MatchOutcome.LOSS } }

    // ── Layout ───────────────────────────────────────────────
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // ════════════════════════════════════════════════════
        //  SECTION 1 — PROFILE
        // ════════════════════════════════════════════════════

        // Avatar
        if (userProfile.profilePictureUrl != null) {
            AsyncImage(
                model = userProfile.profilePictureUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(88.dp).clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(88.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = userProfile.name ?: userProfile.username,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = userProfile.email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ── Info chips (view mode) ─────────────────────────
        if (!isEditing) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement  = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (gender != Gender.UNKNOWN) {
                    ProfileChip(icon = Icons.Default.Person, label = gender.chipLabel())
                }
                ProfileChip(icon = Icons.Default.DateRange, label = ageGroup.chipLabel())
                if (userProfile.city.isNotBlank()) {
                    ProfileChip(icon = Icons.Default.LocationOn, label = userProfile.city)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Edit Profile + Share buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
                ) {
                    Text(
                        text = "EDIT PROFILE",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                OutlinedButton(
                    onClick = { /* share */ },
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onSignOutClick) {
                Text("Sign Out", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

        } else {
            // ── Edit form ──────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )

                    // Gender dropdown
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = gender.chipLabel(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            Gender.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.chipLabel()) },
                                    onClick = { gender = opt; genderExpanded = false }
                                )
                            }
                        }
                    }

                    // Age Group dropdown
                    ExposedDropdownMenuBox(
                        expanded = ageExpanded,
                        onExpandedChange = { ageExpanded = !ageExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = ageGroup.chipLabel(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Age Group") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ageExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = ageExpanded,
                            onDismissRequest = { ageExpanded = false }
                        ) {
                            AgeGroup.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.chipLabel()) },
                                    onClick = { ageGroup = opt; ageExpanded = false }
                                )
                            }
                        }
                    }

                    // City
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
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )

                    // Save / Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // reset to saved values
                                username = userProfile.username
                                city     = userProfile.city
                                gender   = try { Gender.valueOf(userProfile.gender) } catch (_: Exception) { Gender.UNKNOWN }
                                ageGroup = try { AgeGroup.valueOf(userProfile.ageGroup) } catch (_: Exception) { AgeGroup.SENIOR_18_45 }
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Cancel") }

                        Button(
                            onClick = {
                                onUpdateClick(username, gender, ageGroup, city)
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
                        ) { Text("Save") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(24.dp))

        // ════════════════════════════════════════════════════
        //  SECTION 2 — PERFORMANCE DASHBOARD
        // ════════════════════════════════════════════════════

        AccountSectionHeader(
            icon  = Icons.Default.ShowChart,
            title = "Performance Dashboard"
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Rating + Rank row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── Rating ──
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RATING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.0f".format(userProfile.glickoRating),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(60.dp)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                    )

                    // ── Rank ──
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RANKING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (rankPosition != null) "#$rankPosition" else "—",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "of ${allUsers.size} players",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(12.dp))

                // ── Ranking ladder ────────────────────────────────────
                if (rankPosition != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Player above
                        if (playerAbove != null) {
                            RankingLadderRow(
                                position  = rankPosition - 1,
                                name      = playerAbove.name ?: playerAbove.username,
                                rating    = playerAbove.glickoRating,
                                highlight = false
                            )
                        }
                        // Current user (highlighted)
                        RankingLadderRow(
                            position  = rankPosition,
                            name      = "${userProfile.name ?: userProfile.username} (you)",
                            rating    = userProfile.glickoRating,
                            highlight = true
                        )
                        // Player below
                        if (playerBelow != null) {
                            RankingLadderRow(
                                position  = rankPosition + 1,
                                name      = playerBelow.name ?: playerBelow.username,
                                rating    = playerBelow.glickoRating,
                                highlight = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── W / D / L stats ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MatchStatBadge(
                        count = wins,
                        label = "Wins",
                        color = Color(0xFF2E7D32),
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    MatchStatBadge(
                        count = draws,
                        label = "Draws",
                        color = Color(0xFF757575),
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    MatchStatBadge(
                        count = losses,
                        label = "Losses",
                        color = Color(0xFFC62828),
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(12.dp))

                // ── Glicko-2 meta ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RD ${"%.0f".format(userProfile.glickoRd)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "  ·  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "Vol ${"%.4f".format(userProfile.glickoVol)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(24.dp))

        // ════════════════════════════════════════════════════
        //  SECTION 3 — MATCH HISTORY
        // ════════════════════════════════════════════════════

        AccountSectionHeader(
            icon  = Icons.Default.History,
            title = "Match History",
            badge = if (pastMatches.isNotEmpty())
                "${pastMatches.size} match${if (pastMatches.size != 1) "es" else ""}" else null
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (pastMatches.isEmpty()) {
            Text(
                text = "No matches played yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pastMatches.forEach { match ->
                    MatchHistoryRow(
                        match = match,
                        userId = userProfile.id,
                        allTeams = allTeams,
                        displayDate = try {
                            displaySdf.format(sdf.parse(match.dateTime)!!)
                        } catch (_: Exception) { match.dateTime }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ── Shared small composables ─────────────────────────────────────────────────

@Composable
private fun AccountSectionHeader(
    icon: ImageVector,
    title: String,
    badge: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun ProfileChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RankingLadderRow(
    position: Int,
    name: String,
    rating: Double,
    highlight: Boolean
) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val bgColor     = if (highlight) onContainer.copy(alpha = 0.10f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position badge
        Text(
            text   = "#$position",
            style  = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
            ),
            color  = onContainer.copy(alpha = if (highlight) 0.9f else 0.5f),
            modifier = Modifier.width(36.dp)
        )

        // Name
        Text(
            text     = name,
            style    = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal
            ),
            color    = onContainer.copy(alpha = if (highlight) 1f else 0.70f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Rating points
        Text(
            text  = "%.0f".format(rating),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
            ),
            color = onContainer.copy(alpha = if (highlight) 0.9f else 0.5f)
        )
    }
}

@Composable
private fun MatchStatBadge(count: Int, label: String, color: Color, containerColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = containerColor.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
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
        "Team" -> "${match.myTeam} vs ${match.opponent}"
        else   -> "Player Match"
    }
    val scoreLabel = if (match.scoreMyTeam != null && match.scoreOpponent != null)
        "${match.scoreMyTeam} – ${match.scoreOpponent}" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = sportIcon(match.modality),
                contentDescription = match.modality,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))

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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {
    RankUpTheme {
        AccountScreen(
            userProfile = UserProfile(
                id = "u1", name = "Rui Cardoso", email = "rui@example.com",
                profilePictureUrl = null, username = "ruicardoso",
                gender = Gender.MALE.name, ageGroup = AgeGroup.SENIOR_18_45.name, city = "Porto",
                glickoRating = 1542.0, glickoRd = 245.0, glickoVol = 0.0598
            ),
            onUpdateClick = { _, _, _, _ -> },
            onSignInClick = {},
            onSignOutClick = {},
            allUsers = listOf(
                UserProfile(id = "u0", glickoRating = 1700.0),
                UserProfile(id = "u1", glickoRating = 1542.0),
                UserProfile(id = "u2", glickoRating = 1350.0),
            ),
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
