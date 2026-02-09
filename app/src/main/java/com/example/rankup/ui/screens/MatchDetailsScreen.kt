package com.example.rankup.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rankup.PlannedMatch
import com.example.rankup.UserProfile
import com.example.rankup.ui.theme.RankUpTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailsScreen(
    match: PlannedMatch,
    allUsers: List<UserProfile>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSaveResults: (Int?, Int?) -> Unit,
    onCancelMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val now = Calendar.getInstance().time
    val matchDate = try { sdf.parse(match.dateTime) } catch (e: Exception) { null }
    val isPast = matchDate?.before(now) ?: false
    val missingResults = match.scoreMyTeam == null || match.scoreOpponent == null

    var scoreMyTeamText by remember { mutableStateOf(match.scoreMyTeam?.toString() ?: "") }
    var scoreOpponentText by remember { mutableStateOf(match.scoreOpponent?.toString() ?: "") }

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
            Column(modifier = Modifier.padding(24.dp)) {
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
                            tint = Color(0xFF3F51B5)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = match.dateTime, style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = match.location, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (match.matchType == "Team") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { /* View team details */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121))
                ) {
                    Text(text = match.myTeam, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = { /* View opponent details */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                ) {
                    Text(text = match.opponent, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            // Player type: show list of players
            Text(
                text = "Invited Players",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val invitedPlayers = allUsers.filter { match.invitedPlayers.contains(it.id) }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(invitedPlayers) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = player.name ?: player.email,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }

        if (isPast) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(
                    text = "Match Results",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add results:",
                    style = MaterialTheme.typography.bodyLarge
                )
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
                            text = if (match.matchType == "Team") match.myTeam else "Side A",
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
                            text = if (match.matchType == "Team") match.opponent else "Side B",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        onSaveResults(scoreMyTeamText.toIntOrNull(), scoreOpponentText.toIntOrNull())
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    enabled = scoreMyTeamText.isNotEmpty() && scoreOpponentText.isNotEmpty()
                ) {
                    Text("Save Results", color = Color.White)
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
            onBack = {},
            onEdit = {},
            onSaveResults = { _, _ -> },
            onCancelMatch = {}
        )
    }
}
