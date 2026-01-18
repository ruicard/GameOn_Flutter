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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    val userProfile by userViewModel.userProfile.collectAsState()
    val plannedMatches by userViewModel.plannedMatches.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (userProfile == null) {
            userViewModel.signIn(context)
        }
    }

    if (showPlanMatch) {
        PlanMatchScreen(
            viewModel = userViewModel,
            onBack = { showPlanMatch = false },
            onCancelToHome = { showPlanMatch = false }
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
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
                        AppDestinations.HOME -> HomeScreen(
                            userProfile = userProfile,
                            plannedMatches = plannedMatches,
                            onPlanMatchClick = { showPlanMatch = true }
                        )
                        AppDestinations.ACCOUNT -> AccountScreen(userViewModel)
                        AppDestinations.TEAMS -> TeamsScreen()
                    }
                }
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
