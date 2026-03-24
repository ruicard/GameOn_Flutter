package com.example.rankup.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
fun PlanMatchScreen(
    availableSports: List<SportModel>,
    userTeams: List<PlannedTeam>,
    allTeams: List<PlannedTeam>,
    allUsers: List<UserProfile>,
    onSave: (PlannedMatch) -> Unit,
    onBack: () -> Unit,
    onCancelToHome: () -> Unit,
    initialMatch: PlannedMatch? = null,
    modifier: Modifier = Modifier
) {
    // Start at step 2 for rematches (modality + type already known)
    var step by remember { mutableIntStateOf(if (initialMatch != null) 2 else 1) }

    // Step 1 states — pre-filled from initialMatch when rematching
    var modalityExpanded by remember { mutableStateOf(false) }
    var selectedModalityName by remember { mutableStateOf(initialMatch?.modality ?: "") }
    val selectedSportModel = availableSports.find { it.name == selectedModalityName }

    var matchTypeExpanded by remember { mutableStateOf(false) }
    var selectedMatchType by remember { mutableStateOf(initialMatch?.matchType ?: "") }
    val matchTypes = listOf("Player", "Team")

    // Step 2 states — date is always empty (user must pick new date)
    var selectedDateTime by remember { mutableStateOf("") }
    var whereText by remember { mutableStateOf(initialMatch?.location ?: "") }

    // Team mode states — pre-filled from initialMatch
    var teamExpanded by remember { mutableStateOf(false) }
    var selectedTeam by remember { mutableStateOf(initialMatch?.myTeam ?: "") }
    var opponentText by remember { mutableStateOf(initialMatch?.opponent ?: "") }
    var opponentExpanded by remember { mutableStateOf(false) }

    // Filter user teams by selected modality
    val filteredUserTeams = userTeams.filter { it.sport == selectedModalityName }

    // Filter opponent teams by selected modality, name, and exclude selected user team
    val filteredOpponents = allTeams.filter { 
        it.sport == selectedModalityName &&
        it.name.contains(opponentText, ignoreCase = true) && 
        it.name != selectedTeam 
    }

    // Player mode states
    var playerSearchText by remember { mutableStateOf("") }
    var playerSearchExpanded by remember { mutableStateOf(false) }
    val invitedPlayers = remember { mutableStateListOf<UserProfile>() }

    // Pre-fill invited players for rematch (Player-type matches)
    LaunchedEffect(initialMatch, allUsers) {
        if (initialMatch != null && initialMatch.matchType == "Player"
            && invitedPlayers.isEmpty() && allUsers.isNotEmpty()
        ) {
            val players = allUsers.filter { initialMatch.playerInvitations.containsKey(it.id) }
            invitedPlayers.addAll(players)
        }
    }

    val filteredPlayers = allUsers.filter { 
        it.name?.contains(playerSearchText, ignoreCase = true) == true && 
        invitedPlayers.none { p -> p.id == it.id }
    }

    // Dynamic rules from database (SportModel)
    val maxPlayersTeam = (selectedSportModel?.maxPlayersTeam)?: 10
    val maxPlayersMatch = maxPlayersTeam * 2
    val minPlayersNeeded = selectedSportModel?.minPlayersMatch ?: 4

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        if (step < 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (step == 2) step = 1 else onBack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialMatch != null) "Rematch" else "Plan Match",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your first match starts here. Fill in the fields below.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        when (step) {
            1 -> {
                ExposedDropdownMenuBox(expanded = modalityExpanded, onExpandedChange = { modalityExpanded = !modalityExpanded }) {
                    OutlinedTextField(
                        value = selectedModalityName.ifEmpty { "Select modality" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modalityExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(expanded = modalityExpanded, onDismissRequest = { modalityExpanded = false }) {
                        availableSports.forEach { sport ->
                            DropdownMenuItem(
                                text = { Text(sport.name) },
                                onClick = {
                                    if (selectedModalityName != sport.name) {
                                        selectedModalityName = sport.name
                                        selectedMatchType = ""
                                        selectedTeam = "" // Clear team selection when modality changes
                                        opponentText = "" // Clear opponent selection when modality changes
                                    }
                                    modalityExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedModalityName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExposedDropdownMenuBox(expanded = matchTypeExpanded, onExpandedChange = { matchTypeExpanded = !matchTypeExpanded }) {
                        OutlinedTextField(
                            value = selectedMatchType.ifEmpty { "Select match type" },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = matchTypeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(expanded = matchTypeExpanded, onDismissRequest = { matchTypeExpanded = false }) {
                            matchTypes.forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = { selectedMatchType = type; matchTypeExpanded = false })
                            }
                        }
                    }
                }

                if (selectedMatchType.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                        Button(onClick = { step = 2 }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("Next") }
                    }
                }
            }
            2 -> {
                OutlinedTextField(
                    value = selectedDateTime.ifEmpty { "When" },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                TimePickerDialog(context, { _, h, min ->
                                    selectedDateTime = "$d/${m + 1}/$y $h:${String.format("%02d", min)}"
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        }) { Icon(Icons.Default.DateRange, null) }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = whereText,
                    onValueChange = { whereText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    placeholder = { Text("Where") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedMatchType == "Team") {
                    ExposedDropdownMenuBox(expanded = teamExpanded, onExpandedChange = { teamExpanded = !teamExpanded }) {
                        OutlinedTextField(
                            value = selectedTeam.ifEmpty { "Select your team" },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(expanded = teamExpanded, onDismissRequest = { teamExpanded = false }) {
                            filteredUserTeams.forEach { team -> 
                                DropdownMenuItem(text = { Text(team.name) }, onClick = { selectedTeam = team.name; teamExpanded = false }) 
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ExposedDropdownMenuBox(expanded = opponentExpanded && filteredOpponents.isNotEmpty(), onExpandedChange = { opponentExpanded = !opponentExpanded }) {
                        OutlinedTextField(
                            value = opponentText,
                            onValueChange = { opponentText = it; opponentExpanded = true },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            placeholder = { Text("Select your opponent") },
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
                        onExpandedChange = { if (invitedPlayers.size < maxPlayersMatch) playerSearchExpanded = !playerSearchExpanded }
                    ) {
                        OutlinedTextField(
                            value = playerSearchText,
                            onValueChange = { playerSearchText = it; playerSearchExpanded = true },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            placeholder = { Text(if (searchEnabled) "Invite players" else "Max players reached") },
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
                    Text(text = invitedPlayers.size.toString() + " Invites" + " (max: " + maxPlayersMatch + ")", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
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
                                Text(text = player.name ?: player.email, style = MaterialTheme.typography.bodyLarge)
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
                    (selectedMatchType == "Team" && selectedTeam.isNotEmpty() && opponentText.isNotEmpty()) ||
                    (selectedMatchType == "Player" && invitedPlayers.size >= minPlayersNeeded)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = onCancelToHome, modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                    Button(onClick = { step = 3 }, enabled = allFilled, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("Save") }
                }
            }
            3 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = 24.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Color(0xFF00897B)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(72.dp))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Match planned. Invites sent!", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(48.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Match details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                IconButton(onClick = { step = 2 }) { Icon(Icons.Default.Edit, null, tint = Color(0xFF3F51B5)) }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row { Icon(Icons.Default.DateRange, null); Spacer(modifier = Modifier.width(12.dp)); Text(selectedDateTime) }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row { Icon(Icons.Default.LocationOn, null); Spacer(modifier = Modifier.width(12.dp)); Text(whereText) }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { 
                            onSave(PlannedMatch(
                                modality = selectedModalityName, 
                                matchType = selectedMatchType, 
                                dateTime = selectedDateTime, 
                                location = whereText, 
                                myTeam = if (selectedMatchType == "Team") selectedTeam else "", 
                                opponent = if (selectedMatchType == "Team") opponentText else "",
                                invitedPlayers = invitedPlayers.map { it.id }
                            )) 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                    ) { Text("Done") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlanMatchStep1Preview() {
    RankUpTheme { PlanMatchScreen(availableSports = emptyList(), userTeams = emptyList(), allTeams = emptyList(), allUsers = emptyList(), onSave = {}, onBack = {}, onCancelToHome = {}) }
}
