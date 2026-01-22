package com.example.rankup.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
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
import com.example.rankup.UserViewModel
import com.example.rankup.ui.theme.RankUpTheme
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanMatchScreen(
    onSave: (PlannedMatch) -> Unit,
    onBack: () -> Unit, 
    onCancelToHome: () -> Unit, 
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1 states
    var modalityExpanded by remember { mutableStateOf(false) }
    var selectedModality by remember { mutableStateOf("") }
    val modalities = listOf("Futsal", "Padel")

    var matchTypeExpanded by remember { mutableStateOf(false) }
    var selectedMatchType by remember { mutableStateOf("") }
    val matchTypes = listOf("Player", "Team")

    // Step 2 states
    var selectedDateTime by remember { mutableStateOf("") }
    var whereText by remember { mutableStateOf("") }
    var teamExpanded by remember { mutableStateOf(false) }
    var selectedTeam by remember { mutableStateOf("") }
    val teams = listOf("Team HackYou")

    var opponentText by remember { mutableStateOf("") }
    var opponentExpanded by remember { mutableStateOf(false) }
    val opponents = listOf("Team Badass", "Team Crackers")
    val filteredOpponents = opponents.filter { it.contains(opponentText, ignoreCase = true) }

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
        }

        when (step) {
            1 -> {
                // Step 1 Content...
                ExposedDropdownMenuBox(
                    expanded = modalityExpanded,
                    onExpandedChange = { modalityExpanded = !modalityExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedModality.ifEmpty { "Select modality" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modalityExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray,
                            focusedBorderColor = Color.Black
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = modalityExpanded,
                        onDismissRequest = { modalityExpanded = false }
                    ) {
                        modalities.forEach { modality ->
                            DropdownMenuItem(
                                text = { Text(modality) },
                                onClick = {
                                    if (selectedModality != modality) {
                                        selectedModality = modality
                                        selectedMatchType = ""
                                    }
                                    modalityExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedModality.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExposedDropdownMenuBox(
                        expanded = matchTypeExpanded,
                        onExpandedChange = { matchTypeExpanded = !matchTypeExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedMatchType.ifEmpty { "Select match type" },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = matchTypeExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = Color.Black
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = matchTypeExpanded,
                            onDismissRequest = { matchTypeExpanded = false }
                        ) {
                            matchTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedMatchType = type
                                        matchTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedMatchType.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) { Text("Cancel") }
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                        ) { Text("Next") }
                    }
                }
            }
            2 -> {
                // Step 2 Content...
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
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(expanded = teamExpanded, onDismissRequest = { teamExpanded = false }) {
                        teams.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { selectedTeam = it; teamExpanded = false }) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = opponentExpanded && filteredOpponents.isNotEmpty(),
                    onExpandedChange = { opponentExpanded = !opponentExpanded }
                ) {
                    OutlinedTextField(
                        value = opponentText,
                        onValueChange = { opponentText = it; opponentExpanded = true },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        placeholder = { Text("Select your opponent") },
                        trailingIcon = { Icon(Icons.Default.Search, null) }
                    )
                    ExposedDropdownMenu(expanded = opponentExpanded && filteredOpponents.isNotEmpty(), onDismissRequest = { opponentExpanded = false }) {
                        filteredOpponents.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { opponentText = it; opponentExpanded = false }) }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                val allFilled = selectedDateTime.isNotEmpty() && whereText.isNotEmpty() && selectedTeam.isNotEmpty() && opponentText.isNotEmpty()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = onCancelToHome, modifier = Modifier.weight(1f).height(56.dp)) { Text("Cancel") }
                    Button(onClick = { step = 3 }, enabled = allFilled, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("Save") }
                }
            }
            3 -> {
                // Step 3 Content...
                Column(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                        onClick = { onSave(PlannedMatch(modality = selectedModality, matchType = selectedMatchType, dateTime = selectedDateTime, location = whereText, myTeam = selectedTeam, opponent = opponentText)) },
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
    RankUpTheme { PlanMatchScreen(onSave = {}, onBack = {}, onCancelToHome = {}) }
}
