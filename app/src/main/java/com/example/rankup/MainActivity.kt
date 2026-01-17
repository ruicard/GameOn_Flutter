package com.example.rankup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.rankup.ui.theme.RankUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RankUpTheme {
                RankUpApp()
            }
        }
    }
}

@Composable
fun RankUpApp(userViewModel: UserViewModel = viewModel()) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val userProfile by userViewModel.userProfile.collectAsState()
    val context = LocalContext.current

    // Trigger sign in immediately when opening the app
    LaunchedEffect(Unit) {
        if (userProfile == null) {
            userViewModel.signIn(context)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            // Order: Account (Left), Home (Middle), Teams (Right)
            item(
                icon = { Icon(AppDestinations.ACCOUNT.icon, contentDescription = null) },
                label = { Text(AppDestinations.ACCOUNT.label) },
                selected = currentDestination == AppDestinations.ACCOUNT,
                onClick = { currentDestination = AppDestinations.ACCOUNT }
            )
            item(
                icon = { Icon(AppDestinations.HOME.icon, contentDescription = null) },
                label = { Text(AppDestinations.HOME.label) },
                selected = currentDestination == AppDestinations.HOME,
                onClick = { currentDestination = AppDestinations.HOME }
            )
            item(
                icon = { Icon(AppDestinations.TEAMS.icon, contentDescription = null) },
                label = { Text(AppDestinations.TEAMS.label) },
                selected = currentDestination == AppDestinations.TEAMS,
                onClick = { currentDestination = AppDestinations.TEAMS }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                UserProfileTopBar(userProfile)
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(userProfile)
                    AppDestinations.ACCOUNT -> AccountScreen(userViewModel)
                    AppDestinations.TEAMS -> GenericScreen("My Teams")
                }
            }
        }
    }
}

@Composable
fun UserProfileTopBar(userProfile: UserProfile?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (userProfile != null) {
            AsyncImage(
                model = userProfile.profilePictureUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Hello, ${userProfile.name ?: "User"}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Hello, Guest",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
fun HomeScreen(userProfile: UserProfile?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Stadium Image Placeholder
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.LightGray
        ) {
            // In a real app, you'd use a real resource or AsyncImage
            Box(contentAlignment = Alignment.Center) {
                Text("Stadium Image", color = Color.DarkGray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Plan your first football match",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Catchy sentence here, bla bla bla bla bla bla bla bla bla bla bla bla",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { /* Handle Plan Match */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("Plan Match", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun AccountScreen(viewModel: UserViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userProfile != null) {
            AsyncImage(
                model = userProfile?.profilePictureUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(100.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = userProfile?.name ?: "User", style = MaterialTheme.typography.headlineMedium)
            Text(text = userProfile?.email ?: "", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.signOut(context) }) {
                Text("Sign Out")
            }
        } else {
            Text(text = "You are not signed in", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.signIn(context) }) {
                Text("Sign in with Google")
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    ACCOUNT("Account", Icons.Default.Person),
    HOME("Home", Icons.Default.Home),
    TEAMS("Teams", Icons.Default.Groups),
}

@Composable
fun GenericScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name Screen")
    }
}
