package com.example.rankup.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rankup.PlannedMatch
import com.example.rankup.PlannedTeam
import com.example.rankup.SportModel
import com.example.rankup.UserProfile
import com.example.rankup.ui.theme.RankUpTheme
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMatchScreen(
    match: PlannedMatch,
    availableSports: List<SportModel>,
    userTeams: List<PlannedTeam>,
    allTeams: List<PlannedTeam>,
    allUsers: List<UserProfile>,
    onSave: (PlannedMatch) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDateTime by remember { mutableStateOf(match.dateTime) }
    var whereText by remember { mutableStateOf(match.location) }
    
    // Team mode states
    var selectedTeam by remember { mutableStateOf(match.myTeam) }
    var teamExpanded by remember { mutableStateOf(false) }
    var opponentText by remember { mutableStateOf(match.opponent) }
    var opponentExpanded by remember { mutableStateOf(false) }
    val filteredOpponents = allTeams.filter { 
        it.name.contains(opponentText, ignoreCase = true) && it.name != selectedTeam 
    }

    // Player mode states
    var playerSearchText by remember { mutableStateOf("") }
    var playerSearchExpanded by remember { mutableStateOf(false) }
    val invitedPlayers = remember { mutableStateListOf<UserProfile>() }
    
    // Initialize invited players from IDs
    LaunchedEffect(match.invitedPlayers, allUsers) {
        if (invitedPlayers.isEmpty() && match.matchType == "Player") {
            val players = allUsers.filter { match.invitedPlayers.contains(it.id) }
            invitedPlayers.addAll(players)
        }
    }

    val selectedSportModel = availableSports.find { it.name == match.modality }
    val maxPlayersMatch = (selectedSportModel?.maxPlayersTeam ?: 5) * 2
    val minPlayersNeeded = selectedSportModel?.minPlayersMatch ?: 2

    val filteredPlayers = allUsers.filter { user ->
        user.name?.contains(playerSearchText, ignoreCase = true) == true && 
        invitedPlayers.none { p -> p.id == user.id }
    }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    Column(
        modifier = modifier
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
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Edit Match",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Update the match details below.",
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // "When" Field
        OutlinedTextField(
            value = selectedDateTime,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            label = { Text("When") },
            trailingIcon = {
                IconButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d ->
                        TimePickerDialog(context, { _, h, min ->
                            selectedDateTime = "$d/${m + 1}/$y $h:${String.format("%02d", min)}"
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                }) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // "Where" Field
        OutlinedTextField(
            value = whereText,
            onValueChange = { whereText = it },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            label = { Text("Where") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (match.matchType == "Team") {
            // "Select your team" - Only teams where the user is a member
            ExposedDropdownMenuBox(
                expanded = teamExpanded,
                onExpandedChange = { teamExpanded = !teamExpanded }
            ) {
                OutlinedTextField(
                    value = selectedTeam.ifEmpty { "Select your team" },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    label = { Text("Select your team") }
                )
                ExposedDropdownMenu(expanded = teamExpanded, onDismissRequest = { teamExpanded = false }) {
                    userTeams.forEach { team -> 
                        DropdownMenuItem(text = { Text(team.name) }, onClick = { selectedTeam = team.name; teamExpanded = false }) 
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Select your opponent" - Search through all teams in Firestore excluding your selected team
            ExposedDropdownMenuBox(
                expanded = opponentExpanded && filteredOpponents.isNotEmpty(),
                onExpandedChange = { opponentExpanded = !opponentExpanded }
            ) {
                OutlinedTextField(
                    value = opponentText,
                    onValueChange = { opponentText = it; opponentExpanded = true },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    label = { Text("Select your opponent") },
                    trailingIcon = { Icon(Icons.Default.Search, null) }
                )
                ExposedDropdownMenu(expanded = opponentExpanded && filteredOpponents.isNotEmpty(), onDismissRequest = { opponentExpanded = false }) {
                    filteredOpponents.forEach { team -> 
                        DropdownMenuItem(text = { Text(team.name) }, onClick = { opponentText = team.name; opponentExpanded = false }) 
                    }
                }
            }
        } else {
            // "Player" mode logic
            val searchEnabled = invitedPlayers.size < maxPlayersMatch
            ExposedDropdownMenuBox(
                expanded = playerSearchExpanded && filteredPlayers.isNotEmpty() && searchEnabled,
                onExpandedChange = { if (searchEnabled) playerSearchExpanded = !playerSearchExpanded }
            ) {
                OutlinedTextField(
                    value = playerSearchText,
                    onValueChange = { playerSearchText = it; playerSearchExpanded = true },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    label = { Text(if (searchEnabled) "Invite players" else "Max members reached") },
                    trailingIcon = { Icon(Icons.Default.Search, null) },
                    enabled = searchEnabled
                )
                ExposedDropdownMenu(expanded = playerSearchExpanded && filteredPlayers.isNotEmpty(), onDismissRequest = { playerSearchExpanded = false }) {
                    filteredPlayers.forEach { user -> 
                        DropdownMenuItem(
                            text = { Text(user.name ?: user.email) }, 
                            onClick = { 
                                invitedPlayers.add(user)
                                playerSearchText = ""
                                playerSearchExpanded = false 
                            }
                        ) 
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Invites", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(invitedPlayers) { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = player.name ?: player.email, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { invitedPlayers.remove(player) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray)
                        }
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val allFilled = selectedDateTime.isNotEmpty() && whereText.isNotEmpty() && (
            (match.matchType == "Team" && selectedTeam.isNotEmpty() && opponentText.isNotEmpty()) ||
            (match.matchType == "Player" && invitedPlayers.size >= minPlayersNeeded)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Text("Cancel", fontSize = 16.sp)
            }
            Button(
                onClick = { 
                    onSave(match.copy(
                        dateTime = selectedDateTime,
                        location = whereText,
                        myTeam = if (match.matchType == "Team") selectedTeam else "",
                        opponent = if (match.matchType == "Team") opponentText else "",
                        invitedPlayers = if (match.matchType == "Player") invitedPlayers.map { it.id } else match.invitedPlayers
                    ))
                },
                enabled = allFilled,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF333333),
                    disabledContainerColor = Color.LightGray
                )
            ) {
                Text("Save", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditMatchScreenPreview() {
    RankUpTheme {
        EditMatchScreen(
            match = PlannedMatch(
                modality = "Football",
                dateTime = "12.09.2025 18:00",
                location = "Campo Futebol Bosch",
                myTeam = "Team Yellow",
                opponent = "Team 123"
            ),
            availableSports = emptyList(),
            userTeams = emptyList(),
            allTeams = emptyList(),
            allUsers = emptyList(),
            onSave = {},
            onBack = {}
        )
    }
}
