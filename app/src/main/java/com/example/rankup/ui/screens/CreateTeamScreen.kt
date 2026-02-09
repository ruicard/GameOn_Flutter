package com.example.rankup.ui.screens

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rankup.PlannedTeam
import com.example.rankup.UserProfile
import com.example.rankup.SportModel
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
    availableSports: List<SportModel>,
    onPickIcon: ((String) -> Unit) -> Unit,
    onBack: () -> Unit,
    onSave: (PlannedTeam, Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1 State
    var name by remember { mutableStateOf("") }
    var selectedModalityName by remember { mutableStateOf("") }
    var selectedAgeGroup by remember { mutableStateOf(AgeGroup.SENIOR_18_45) }
    var selectedGender by remember { mutableStateOf(Gender.UNKNOWN) }
    var city by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedIconUrl by remember { mutableStateOf<String?>("https://cdn-icons-png.flaticon.com/512/166/166344.png") }

    // Step 2 State
    var selectedCaptain by remember { mutableStateOf(currentUser) }
    var captainSearchText by remember { mutableStateOf(currentUser.name ?: "") }
    var captainExpanded by remember { mutableStateOf(false) }
    
    var playerSearchText by remember { mutableStateOf("") }
    var playerSearchExpanded by remember { mutableStateOf(false) }
    val selectedMembers = remember { mutableStateListOf<UserProfile>(currentUser) }

    val selectedSportModel = availableSports.find { it.name == selectedModalityName }
    val maxPlayers = selectedSportModel?.maxPlayersTeam ?: 10
    val minPlayersForMatch = selectedSportModel?.minPlayersMatch ?: 4
    val minPlayersNeededForCreation = minPlayersForMatch / 2

    val context = LocalContext.current
    
    // Filter users for player search: same gender as team, and not already a member
    val eligiblePlayers = allUsers.filter { user ->
        val genderMatch = user.gender == selectedGender.name || selectedGender == Gender.UNKNOWN
        genderMatch && selectedMembers.none { it.id == user.id }
    }
    
    val filteredCaptains = allUsers.filter { it.name?.contains(captainSearchText, ignoreCase = true) == true }
    val filteredPlayers = eligiblePlayers.filter { it.name?.contains(playerSearchText, ignoreCase = true) == true }

    // Location Logic
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(it.latitude, it.longitude, 1) { addresses ->
                                if (addresses.isNotEmpty()) { city = addresses[0].locality ?: "" }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                            if (!addresses.isNullOrEmpty()) { city = addresses[0].locality ?: "" }
                        }
                    }
                }
            } catch (e: SecurityException) { Log.e("Location", "Denied") }
        }
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (step == 2) step = 1 else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Team", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp))
        }

        if (step == 1) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = selectedIconUrl ?: selectedImageUri ?: "https://cdn-icons-png.flaticon.com/512/166/166344.png", 
                        contentDescription = null, 
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .clickable { 
                                onPickIcon { url ->
                                    selectedIconUrl = url
                                    selectedImageUri = null // Clear local URI if an icon is picked
                                }
                            }
                    )
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                Spacer(modifier = Modifier.height(16.dp))
                
                var modalityExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = modalityExpanded, onExpandedChange = { modalityExpanded = !modalityExpanded }) {
                    OutlinedTextField(value = selectedModalityName.ifEmpty { "Select modality" }, onValueChange = {}, readOnly = true, label = { Text("Modality") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modalityExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    ExposedDropdownMenu(expanded = modalityExpanded, onDismissRequest = { modalityExpanded = false }) {
                        availableSports.forEach { sport -> DropdownMenuItem(text = { Text(sport.name) }, onClick = { selectedModalityName = sport.name; modalityExpanded = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                var ageExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = ageExpanded, onExpandedChange = { ageExpanded = !ageExpanded }) {
                    OutlinedTextField(value = selectedAgeGroup.name.replace("_", " "), onValueChange = {}, readOnly = true, label = { Text("Age Group") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ageExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    ExposedDropdownMenu(expanded = ageExpanded, onDismissRequest = { ageExpanded = false }) {
                        AgeGroup.entries.forEach { opt -> DropdownMenuItem(text = { Text(opt.name.replace("_", " ")) }, onClick = { selectedAgeGroup = opt; ageExpanded = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                var genderExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }) {
                    OutlinedTextField(value = selectedGender.name, onValueChange = {}, readOnly = true, label = { Text("Gender") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(genderExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                        Gender.entries.forEach { opt -> DropdownMenuItem(text = { Text(opt.name) }, onClick = { selectedGender = opt; genderExpanded = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, trailingIcon = { IconButton(onClick = { locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) { Icon(Icons.Default.LocationOn, null) } }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                // Captain Selection
                ExposedDropdownMenuBox(expanded = captainExpanded, onExpandedChange = { captainExpanded = !captainExpanded }) {
                    OutlinedTextField(
                        value = captainSearchText, 
                        onValueChange = { captainSearchText = it; captainExpanded = true }, 
                        label = { Text("Capitain") }, 
                        modifier = Modifier.menuAnchor().fillMaxWidth(), 
                        shape = MaterialTheme.shapes.medium
                    )
                    if (filteredCaptains.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = captainExpanded, onDismissRequest = { captainExpanded = false }) {
                            filteredCaptains.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user.name ?: user.email) },
                                    onClick = {
                                        selectedCaptain = user
                                        captainSearchText = user.name ?: user.email
                                        captainExpanded = false
                                        if (selectedMembers.none { it.id == user.id }) {
                                            selectedMembers.add(user)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Player Search
                val searchEnabled = selectedMembers.size < maxPlayers
                ExposedDropdownMenuBox(expanded = playerSearchExpanded && filteredPlayers.isNotEmpty() && searchEnabled, onExpandedChange = { if (searchEnabled) playerSearchExpanded = !playerSearchExpanded }) {
                    OutlinedTextField(
                        value = playerSearchText, 
                        onValueChange = { playerSearchText = it; playerSearchExpanded = true }, 
                        label = { Text(if (searchEnabled) "Invite players" else "Max members reached") }, 
                        trailingIcon = { Icon(Icons.Default.Search, null) }, 
                        modifier = Modifier.menuAnchor().fillMaxWidth(), 
                        shape = MaterialTheme.shapes.medium, 
                        enabled = searchEnabled
                    )
                    ExposedDropdownMenu(expanded = playerSearchExpanded && filteredPlayers.isNotEmpty(), onDismissRequest = { playerSearchExpanded = false }) {
                        filteredPlayers.forEach { user -> 
                            DropdownMenuItem(
                                text = { Text(user.name ?: user.email) }, 
                                onClick = { 
                                    selectedMembers.add(user)
                                    playerSearchText = ""
                                    playerSearchExpanded = false 
                                }
                            ) 
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Invites", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedMembers) { member ->
                        val isCaptain = member.id == selectedCaptain.id
                        val isMe = member.id == currentUser.id
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = buildString {
                                    append(member.name ?: member.email)
                                    if (isMe) append(" (you)")
                                    if (isCaptain) append(" (capitain)")
                                },
                                style = MaterialTheme.typography.bodyLarge, 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (member.id != currentUser.id && !isCaptain) {
                                IconButton(onClick = { selectedMembers.remove(member) }) { Icon(Icons.Default.Delete, null, tint = Color.Gray) }
                            }
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = { if (step == 2) step = 1 else onBack() }, 
                modifier = Modifier.weight(1f).height(56.dp), 
                shape = RoundedCornerShape(28.dp), 
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
            ) {
                Text(if (step == 1) "Cancel" else "Back", color = Color.Black)
            }
            
            if (step == 1) {
                val step1Filled = name.isNotEmpty() && selectedModalityName.isNotEmpty() && city.isNotEmpty()
                Button(
                    onClick = { step = 2 }, 
                    enabled = step1Filled, 
                    modifier = Modifier.weight(1f).height(56.dp), 
                    shape = RoundedCornerShape(28.dp), 
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (step1Filled) Color(0xFF333333) else Color(0xFFE0E0E0), 
                        disabledContainerColor = Color(0xFFE0E0E0)
                    )
                ) {
                    Text("Next")
                }
            } else {
                val canCreate = selectedMembers.size >= minPlayersNeededForCreation && selectedCaptain.id.isNotEmpty()
                Button(
                    onClick = { 
                        onSave(PlannedTeam(
                            name = name, 
                            sport = selectedModalityName, 
                            members = selectedMembers.map { it.id }, 
                            creatorId = currentUser.id, 
                            gender = selectedGender.name, 
                            ageGroup = selectedAgeGroup.name, 
                            city = city, 
                            captainId = selectedCaptain.id,
                            profilePictureUrl = selectedIconUrl
                        ), selectedImageUri) 
                    }, 
                    enabled = canCreate, 
                    modifier = Modifier.weight(1f).height(56.dp), 
                    shape = RoundedCornerShape(28.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), disabledContainerColor = Color(0xFFE0E0E0))
                ) {
                    Text("Create Team")
                }
            }
        }
    }
}
