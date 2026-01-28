package com.example.rankup.ui.screens

import android.Manifest
import android.content.Context
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import coil.compose.AsyncImage
import com.example.rankup.PlannedTeam
import com.example.rankup.UserProfile
import com.example.rankup.data.AgeGroup
import com.example.rankup.data.Gender
import com.example.rankup.ui.theme.RankUpTheme
import com.google.android.gms.location.LocationServices
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(
    currentUser: UserProfile,
    allUsers: List<UserProfile>,
    onBack: () -> Unit,
    onSave: (PlannedTeam) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedModality by remember { mutableStateOf("") }
    var selectedAgeGroup by remember { mutableStateOf(AgeGroup.SENIOR_18_45) }
    var selectedGender by remember { mutableStateOf(Gender.UNKNOWN) }
    var city by remember { mutableStateOf("") }
    var selectedCaptain by remember { mutableStateOf(currentUser) }
    var captainSearchText by remember { mutableStateOf(currentUser.name ?: "") }

    var modalityExpanded by remember { mutableStateOf(false) }
    var ageExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var captainExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val modalities = listOf("Futsal", "Futebol 11", "Padel")
    val filteredUsers = allUsers.filter { it.name?.contains(captainSearchText, ignoreCase = true) == true }

    // Location Logic
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        city = addresses[0].locality ?: addresses[0].subAdminArea ?: ""
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
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
                text = "Create Team",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp)
            )
        }

        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = "https://cdn-icons-png.flaticon.com/512/166/166344.png",
                contentDescription = null,
                modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.LightGray)
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Modality
        ExposedDropdownMenuBox(expanded = modalityExpanded, onExpandedChange = { modalityExpanded = !modalityExpanded }) {
            OutlinedTextField(
                value = selectedModality,
                onValueChange = {},
                readOnly = true,
                label = { Text("Modality") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modalityExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(expanded = modalityExpanded, onDismissRequest = { modalityExpanded = false }) {
                modalities.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { selectedModality = option; modalityExpanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Age Group
        ExposedDropdownMenuBox(expanded = ageExpanded, onExpandedChange = { ageExpanded = !ageExpanded }) {
            OutlinedTextField(
                value = selectedAgeGroup.name.replace("_", " "),
                onValueChange = {},
                readOnly = true,
                label = { Text("Age Group") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ageExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(expanded = ageExpanded, onDismissRequest = { ageExpanded = false }) {
                AgeGroup.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.name.replace("_", " ")) }, onClick = { selectedAgeGroup = option; ageExpanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gender
        ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }) {
            OutlinedTextField(
                value = selectedGender.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(genderExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                Gender.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.name) }, onClick = { selectedGender = option; genderExpanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // City with Location Fetch
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City") },
            trailingIcon = { 
                IconButton(onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) { 
                    Icon(Icons.Default.LocationOn, null) 
                } 
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Captain Search
        ExposedDropdownMenuBox(expanded = captainExpanded, onExpandedChange = { captainExpanded = !captainExpanded }) {
            OutlinedTextField(
                value = captainSearchText,
                onValueChange = { captainSearchText = it; captainExpanded = true },
                label = { Text("Capitain") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(captainExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            if (filteredUsers.isNotEmpty()) {
                ExposedDropdownMenu(expanded = captainExpanded, onDismissRequest = { captainExpanded = false }) {
                    filteredUsers.forEach { user ->
                        DropdownMenuItem(
                            text = { Text(user.name ?: user.email) },
                            onClick = {
                                selectedCaptain = user
                                captainSearchText = user.name ?: user.email
                                captainExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                onSave(PlannedTeam(
                    name = name,
                    sport = selectedModality,
                    creatorId = currentUser.id,
                    members = listOf(currentUser.id, selectedCaptain.id).distinct(),
                    gender = selectedGender.name,
                    ageGroup = selectedAgeGroup.name,
                    city = city,
                    captainId = selectedCaptain.id
                ))
            },
            enabled = name.isNotEmpty() && selectedModality.isNotEmpty() && city.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
        ) {
            Text("Create Team", color = Color.White, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
