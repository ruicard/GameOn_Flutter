package com.example.rankup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rankup.UserViewModel
import com.example.rankup.data.AgeGroup
import com.example.rankup.data.Gender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(viewModel: UserViewModel, modifier: Modifier = Modifier) {
    val userProfile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current

    if (userProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { viewModel.signIn(context) }) {
                Text("Sign in with Google")
            }
        }
        return
    }

    var username by remember { mutableStateOf(userProfile?.username ?: "") }
    var city by remember { mutableStateOf(userProfile?.city ?: "") }
    var gender by remember { mutableStateOf(userProfile?.gender ?: Gender.UNKNOWN) }
    var ageGroup by remember { mutableStateOf(userProfile?.ageGroup ?: AgeGroup.SENIOR_18_45) }

    var genderExpanded by remember { mutableStateOf(false) }
    var ageExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = userProfile?.profilePictureUrl,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = userProfile?.name ?: "User", 
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(text = userProfile?.email ?: "", color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        // Username Field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                Gender.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            gender = option
                            genderExpanded = false
                        }
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
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = ageExpanded,
                onDismissRequest = { ageExpanded = false }
            ) {
                AgeGroup.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name.replace("_", " ")) },
                        onClick = {
                            ageGroup = option
                            ageExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // City Field
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.updateProfile(context, username, gender, ageGroup, city) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("Update Profile")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { viewModel.signOut(context) }) {
            Text("Sign Out", color = Color.Red)
        }
    }
}
