package com.example.rankup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rankup.ui.components.UserProfileTopBar
import com.example.rankup.ui.screens.AccountScreen
import com.example.rankup.ui.screens.HomeScreen
import com.example.rankup.ui.screens.TeamsScreen
import com.example.rankup.ui.screens.PlanMatchScreen
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
    var showPlanMatch by rememberSaveable { mutableStateOf(false) }
    
    val userProfile: UserProfile? by userViewModel.userProfile.collectAsStateWithLifecycle()
    val plannedMatches: List<PlannedMatch> by userViewModel.plannedMatches.collectAsStateWithLifecycle()
    
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (userProfile == null) {
            userViewModel.signIn(context)
        }
    }

    if (showPlanMatch) {
        PlanMatchScreen(
            onSave = { match ->
                userViewModel.addMatch(context, match)
                showPlanMatch = false
            },
            onBack = { showPlanMatch = false },
            onCancelToHome = { showPlanMatch = false }
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                // Account item
                item(
                    icon = { 
                        Icon(
                            AppDestinations.ACCOUNT.icon, 
                            contentDescription = null,
                            tint = if (userProfile != null) Color.Unspecified else Color.LightGray
                        ) 
                    },
                    label = { 
                        Text(
                            AppDestinations.ACCOUNT.label,
                            color = if (userProfile != null) Color.Unspecified else Color.LightGray
                        ) 
                    },
                    selected = currentDestination == AppDestinations.ACCOUNT,
                    onClick = { if (userProfile != null) currentDestination = AppDestinations.ACCOUNT },
                    enabled = userProfile != null
                )
                // Home item
                item(
                    icon = { Icon(AppDestinations.HOME.icon, contentDescription = null) },
                    label = { Text(AppDestinations.HOME.label) },
                    selected = currentDestination == AppDestinations.HOME,
                    onClick = { currentDestination = AppDestinations.HOME }
                )
                // Teams item
                item(
                    icon = { 
                        Icon(
                            AppDestinations.TEAMS.icon, 
                            contentDescription = null,
                            tint = if (userProfile != null) Color.Unspecified else Color.LightGray
                        ) 
                    },
                    label = { 
                        Text(
                            AppDestinations.TEAMS.label,
                            color = if (userProfile != null) Color.Unspecified else Color.LightGray
                        ) 
                    },
                    selected = currentDestination == AppDestinations.TEAMS,
                    onClick = { if (userProfile != null) currentDestination = AppDestinations.TEAMS },
                    enabled = userProfile != null
                )
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (userProfile != null) {
                        UserProfileTopBar(userProfile)
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentDestination) {
                        AppDestinations.HOME -> HomeScreen(
                            userProfile = userProfile,
                            plannedMatches = plannedMatches,
                            onPlanMatchClick = { showPlanMatch = true },
                            onSignInClick = { userViewModel.signIn(context) }
                        )
                        AppDestinations.ACCOUNT -> AccountScreen(
                            userProfile = userProfile,
                            onUpdateClick = { username, gender, age, city ->
                                userViewModel.updateProfile(context, username, gender, age, city)
                            },
                            onSignInClick = { userViewModel.signIn(context) },
                            onSignOutClick = { userViewModel.signOut(context) }
                        )
                        AppDestinations.TEAMS -> TeamsScreen()
                    }
                }
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    ACCOUNT("Account", Icons.Default.Person),
    HOME("Home", Icons.Default.Home),
    TEAMS("Teams", Icons.Default.Groups),
}
