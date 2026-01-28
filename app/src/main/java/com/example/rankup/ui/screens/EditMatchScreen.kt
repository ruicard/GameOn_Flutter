package com.example.rankup.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rankup.PlannedMatch
import com.example.rankup.ui.theme.RankUpTheme
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMatchScreen(
    match: PlannedMatch,
    onSave: (PlannedMatch) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDateTime by remember { mutableStateOf(match.dateTime) }
    var whereText by remember { mutableStateOf(match.location) }
    var selectedTeam by remember { mutableStateOf(match.myTeam) }
    var opponentText by remember { mutableStateOf(match.opponent) }
    
    var teamExpanded by remember { mutableStateOf(false) }
    var opponentExpanded by remember { mutableStateOf(false) }
    
    val teams = listOf("Team HackYou", "Team Yellow", "Team Blue")
    val opponents = listOf("Team Badass", "Team Crackers", "Team 123")
    val filteredOpponents = opponents.filter { it.contains(opponentText, ignoreCase = true) }

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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Plan Match",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your first match starts here. Fill in the fields below.",
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

        // "Select your team" Dropdown
        ExposedDropdownMenuBox(
            expanded = teamExpanded,
            onExpandedChange = { teamExpanded = !teamExpanded }
        ) {
            OutlinedTextField(
                value = selectedTeam,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                label = { Text("Select your team") }
            )

            ExposedDropdownMenu(
                expanded = teamExpanded,
                onDismissRequest = { teamExpanded = false }
            ) {
                teams.forEach { team ->
                    DropdownMenuItem(
                        text = { Text(team) },
                        onClick = {
                            selectedTeam = team
                            teamExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "Select your opponent" Search Bar
        ExposedDropdownMenuBox(
            expanded = opponentExpanded && filteredOpponents.isNotEmpty(),
            onExpandedChange = { opponentExpanded = !opponentExpanded }
        ) {
            OutlinedTextField(
                value = opponentText,
                onValueChange = { 
                    opponentText = it
                    opponentExpanded = true
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                label = { Text("Select your opponent") },
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            ExposedDropdownMenu(
                expanded = opponentExpanded && filteredOpponents.isNotEmpty(),
                onDismissRequest = { opponentExpanded = false }
            ) {
                filteredOpponents.forEach { opponent ->
                    DropdownMenuItem(
                        text = { Text(opponent) },
                        onClick = {
                            opponentText = opponent
                            opponentExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val allFieldsFilled = selectedDateTime.isNotEmpty() && whereText.isNotEmpty() && selectedTeam.isNotEmpty() && opponentText.isNotEmpty()

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
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
            ) {
                Text("Cancel", color = Color.Black, fontSize = 16.sp)
            }
            Button(
                onClick = { 
                    onSave(match.copy(
                        dateTime = selectedDateTime,
                        location = whereText,
                        myTeam = selectedTeam,
                        opponent = opponentText
                    ))
                },
                enabled = allFieldsFilled,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
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
            onSave = {},
            onBack = {}
        )
    }
}
