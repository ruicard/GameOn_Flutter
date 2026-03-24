package com.example.rankup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsRugby
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rankup.PlannedTeam
import com.example.rankup.ui.theme.RankUpTheme
import java.util.*

/** Maps a sport/modality name to a Material Icon. */
fun sportIcon(sport: String): ImageVector = when (sport.lowercase(Locale.getDefault()).trim()) {
    "football", "soccer", "futsal" -> Icons.Default.SportsSoccer
    "padel", "tennis", "squash", "badminton" -> Icons.Default.SportsTennis
    "basketball" -> Icons.Default.SportsBasketball
    "volleyball", "beach volleyball" -> Icons.Default.SportsVolleyball
    "handball" -> Icons.Default.SportsHandball
    "hockey", "field hockey", "ice hockey" -> Icons.Default.SportsHockey
    "golf" -> Icons.Default.SportsGolf
    "rugby" -> Icons.Default.SportsRugby
    "martial arts", "mma", "karate", "judo" -> Icons.Default.SportsMartialArts
    else -> Icons.Default.EmojiEvents
}

@Composable
fun TeamsScreen(
    teams: List<PlannedTeam>,
    onTeamClick: (PlannedTeam) -> Unit,
    onCreateTeamClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (teams.isEmpty()) {
        // Empty State: Matches the "Create your first team" image
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Team/Stadium Entrance Image
            AsyncImage(
                model = "https://images.unsplash.com/photo-1518091043644-c1d4457512c6?q=80&w=1000&auto=format&fit=crop",
                contentDescription = "Team Entrance",
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Create your first team",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onCreateTeamClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
            ) {
                Text("Create Team", color = Color.White, fontSize = 16.sp)
            }
        }
    } else {
        // Group by sport, sorted alphabetically
        val groupedTeams = teams
            .groupBy { it.sport.ifBlank { "Other" } }
            .toSortedMap(compareBy { it.lowercase(Locale.getDefault()) })

        // List State
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "My Teams",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            groupedTeams.forEach { (modality, teamsInGroup) ->
                // Modality separator
                item(key = "header_$modality") {
                    ModalitySeparator(modality = modality)
                }
                // Teams in this modality
                items(teamsInGroup, key = { it.id }) { team ->
                    TeamListItem(team = team, onClick = { onTeamClick(team) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateTeamClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Team", color = Color.White, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ModalitySeparator(modality: String) {
    val icon = sportIcon(modality)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = modality,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = modality,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun TeamListItem(team: PlannedTeam, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (team.profilePictureUrl != null) {
                    AsyncImage(
                        model = team.profilePictureUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${team.members.size} member${if (team.members.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TeamsScreenEmptyPreview() {
    RankUpTheme {
        TeamsScreen(teams = emptyList(), onTeamClick = {}, onCreateTeamClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun TeamsScreenListPreview() {
    RankUpTheme {
        TeamsScreen(
            teams = listOf(
                PlannedTeam(name = "Team HackYou", sport = "Football", members = listOf("1", "2"), profilePictureUrl = "https://cdn-icons-png.flaticon.com/512/166/166344.png"),
                PlannedTeam(name = "Dream Team", sport = "Padel", members = listOf("1", "2", "3")),
                PlannedTeam(name = "Rockets", sport = "Basketball", members = listOf("1", "2", "3", "4", "5")),
                PlannedTeam(name = "Futsal Kings", sport = "Futsal", members = listOf("1", "2"))
            ),
            onTeamClick = {},
            onCreateTeamClick = {}
        )
    }
}
