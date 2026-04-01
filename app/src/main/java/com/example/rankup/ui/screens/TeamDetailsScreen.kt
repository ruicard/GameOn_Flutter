package com.example.rankup.ui.screens

import android.Manifest
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.example.rankup.PlannedTeam
import com.example.rankup.UserProfile
import com.example.rankup.SportModel
import com.google.android.gms.location.LocationServices
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailsScreen(
    team: PlannedTeam,
    currentUser: UserProfile,
    allUsers: List<UserProfile>,
    availableSports: List<SportModel>,
    onPickIcon: ((String) -> Unit) -> Unit,
    onBack: () -> Unit,
    onSave: (PlannedTeam, Uri?) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Team info", "Players")

    // Only the captain can edit the team
    val isCurrentUserCaptain = currentUser.id == team.captainId

    var name by remember { mutableStateOf(team.name) }
    var city by remember { mutableStateOf(team.city) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedIconUrl by remember { mutableStateOf(team.profilePictureUrl) }
    
    val selectedMembers = remember { mutableStateListOf<UserProfile>() }
    var playerSearchText by remember { mutableStateOf("") }
    var playerSearchExpanded by remember { mutableStateOf(false) }

    var selectedCaptain by remember { mutableStateOf<UserProfile?>(null) }
    var captainSearchText by remember { mutableStateOf("") }
    var captainExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    val selectedSportModel = availableSports.find { it.name == team.sport }
    val maxPlayers = selectedSportModel?.maxPlayersTeam ?: 10

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

    // Initialize members and captain
    LaunchedEffect(team, allUsers) {
        selectedMembers.clear()
        val members = allUsers.filter { team.members.contains(it.id) }
        selectedMembers.addAll(members)
        
        val captain = allUsers.find { it.id == team.captainId }
        selectedCaptain = captain
        captainSearchText = captain?.name ?: captain?.email ?: ""
    }

    val eligiblePlayers = allUsers.filter { user ->
        val genderMatch = user.gender == team.gender || team.gender == "UNKNOWN"
        genderMatch && selectedMembers.none { it.id == user.id }
    }
    val filteredPlayers = eligiblePlayers.filter { it.name?.contains(playerSearchText, ignoreCase = true) == true }
    val filteredCaptains = allUsers.filter { it.name?.contains(captainSearchText, ignoreCase = true) == true }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Team Details",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp)
            )
        }

        // Team Picture clickable to pick icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp), 
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = selectedIconUrl ?: team.profilePictureUrl ?: "https://cdn-icons-png.flaticon.com/512/166/166344.png",
                imageLoader = imageLoader,
                contentDescription = "Team Picture",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable(enabled = isCurrentUserCaptain) {
                        onPickIcon { url ->
                            selectedIconUrl = url
                            selectedImageUri = null
                        }
                    }
            )
        }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color.Black,
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color.Black
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTabIndex == 0) {
                // Tab 1: Team info
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Team Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        enabled = isCurrentUserCaptain
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        enabled = isCurrentUserCaptain,
                        trailingIcon = {
                            if (isCurrentUserCaptain) {
                                IconButton(onClick = { locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Get Location")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = team.gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            enabled = false
                        )
                        OutlinedTextField(
                            value = team.ageGroup.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Age Group") },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            enabled = false
                        )
                    }
                }
            } else {
                // Tab 2: Players
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Captain-only notice for non-captains ──────────────
                    if (!isCurrentUserCaptain) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Only the captain can manage team members.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ── Captain Selection (captain only) ──────────────────
                    if (isCurrentUserCaptain) {
                        ExposedDropdownMenuBox(expanded = captainExpanded, onExpandedChange = { captainExpanded = !captainExpanded }) {
                            OutlinedTextField(
                                value = captainSearchText,
                                onValueChange = { captainSearchText = it; captainExpanded = true },
                                label = { Text("Captain") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(captainExpanded) },
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

                        // ── Player Search (captain only) ──────────────────
                        val searchEnabled = selectedMembers.size < maxPlayers
                        ExposedDropdownMenuBox(
                            expanded = playerSearchExpanded && filteredPlayers.isNotEmpty() && searchEnabled,
                            onExpandedChange = { if (searchEnabled) playerSearchExpanded = !playerSearchExpanded }
                        ) {
                            OutlinedTextField(
                                value = playerSearchText,
                                onValueChange = { playerSearchText = it; playerSearchExpanded = true },
                                label = { Text(if (searchEnabled) "Add player" else "Max members reached") },
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

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text("Members", style = MaterialTheme.typography.titleMedium, color = Color.Gray)

                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val sortedDisplayList = selectedMembers.sortedByDescending { it.id == selectedCaptain?.id }
                        items(sortedDisplayList) { member ->
                            val isCaptain = member.id == selectedCaptain?.id
                            val isMe = member.id == currentUser.id
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = buildString {
                                        append(member.name ?: member.email)
                                        if (isMe) append(" (you)")
                                        if (isCaptain) append(" ★ captain")
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                // Captain can remove any member except the current captain
                                if (isCurrentUserCaptain && !isCaptain) {
                                    IconButton(onClick = { selectedMembers.removeAll { it.id == member.id } }) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Gray)
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isCurrentUserCaptain) {
                Button(
                    onClick = {
                        onSave(team.copy(
                            name = name,
                            city = city,
                            members = selectedMembers.map { it.id },
                            captainId = selectedCaptain?.id ?: team.captainId,
                            profilePictureUrl = selectedIconUrl
                        ), selectedImageUri)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) {
                    Text("Save Changes", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { onDelete(team.id) }) {
                    Text("Delete Team", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
