package com.example.rankup.ui.screens

import android.Manifest
import android.location.Geocoder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocationOn
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
import com.example.rankup.UserProfile
import com.example.rankup.data.AgeGroup
import com.example.rankup.data.Gender
import com.example.rankup.ui.theme.RankUpTheme
import com.google.android.gms.location.LocationServices
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    userProfile: UserProfile?,
    onUpdateClick: (String, Gender, AgeGroup, String) -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (userProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onSignInClick) {
                Text("Sign in with Google")
            }
        }
        return
    }

    val context = LocalContext.current

    var username by remember { mutableStateOf(userProfile.username) }
    var city     by remember { mutableStateOf(userProfile.city) }

    var gender by remember {
        mutableStateOf(try { Gender.valueOf(userProfile.gender) } catch (_: Exception) { Gender.UNKNOWN })
    }
    var ageGroup by remember {
        mutableStateOf(try { AgeGroup.valueOf(userProfile.ageGroup) } catch (_: Exception) { AgeGroup.SENIOR_18_45 })
    }

    var genderExpanded by remember { mutableStateOf(false) }
    var ageExpanded    by remember { mutableStateOf(false) }

    // Location — same pattern as CreateTeamScreen
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Profile picture
        if (userProfile.profilePictureUrl != null) {
            AsyncImage(
                model = userProfile.profilePictureUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(80.dp),
                tint = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = userProfile.name ?: userProfile.username,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        Text(
            text = userProfile.email,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 13.sp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Username Field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gender Dropdown
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = gender.name,
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
                Gender.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = { gender = option; genderExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Age Group Dropdown
        ExposedDropdownMenuBox(
            expanded = ageExpanded,
            onExpandedChange = { ageExpanded = !ageExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = ageGroup.name.replace("_", " "),
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
                AgeGroup.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name.replace("_", " ")) },
                        onClick = { ageGroup = option; ageExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // City Field — tapping the location icon requests permission then auto-fills city
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
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onUpdateClick(username, gender, ageGroup, city) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("Update Profile")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSignOutClick) {
            Text("Sign Out", color = Color.Red)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {
    RankUpTheme {
        AccountScreen(
            userProfile = UserProfile(
                id = UUID.randomUUID().toString(),
                name = "Rui Cardoso",
                email = "rui@example.com",
                profilePictureUrl = null,
                username = "ruicardoso",
                gender = Gender.MALE.name,
                ageGroup = AgeGroup.SENIOR_18_45.name,
                city = "Porto"
            ),
            onUpdateClick = { _, _, _, _ -> },
            onSignInClick = {},
            onSignOutClick = {}
        )
    }
}
