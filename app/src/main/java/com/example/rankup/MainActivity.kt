package com.example.rankup

import android.net.Uri
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rankup.ui.components.UserProfileTopBar
import com.example.rankup.ui.screens.*
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
    var showCreateTeam by rememberSaveable { mutableStateOf(false) }
    var selectedMatchForDetails by remember { mutableStateOf<PlannedMatch?>(null) }
    var matchToEdit by remember { mutableStateOf<PlannedMatch?>(null) }
    var selectedTeamForDetails by remember { mutableStateOf<PlannedTeam?>(null) }
    
    // Icon Picker logic
    var showIconPicker by remember { mutableStateOf(false) }
    var iconPickerCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val userProfile by userViewModel.userProfile.collectAsStateWithLifecycle()
    val plannedMatches by userViewModel.plannedMatches.collectAsStateWithLifecycle()
    val userTeams by userViewModel.userTeams.collectAsStateWithLifecycle()
    val allTeams by userViewModel.allTeams.collectAsStateWithLifecycle()
    val allUsers by userViewModel.allUsers.collectAsStateWithLifecycle()
    val availableSports by userViewModel.availableSports.collectAsStateWithLifecycle()
    val isInitializing by userViewModel.isInitializing.collectAsStateWithLifecycle()
    val isRefreshing by userViewModel.isRefreshing.collectAsStateWithLifecycle()
    
    val context = LocalContext.current

    LaunchedEffect(isInitializing, userProfile) {
        if (!isInitializing && userProfile == null) {
            userViewModel.signIn(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isInitializing) {
            Box(modifier = Modifier.fillMaxSize())
        } else if (showPlanMatch) {
            PlanMatchScreen(
                availableSports = availableSports,
                userTeams = userTeams,
                allTeams = allTeams,
                allUsers = allUsers,
                onSave = { match ->
                    userViewModel.addMatch(context, match)
                    showPlanMatch = false
                },
                onBack = { showPlanMatch = false },
                onCancelToHome = { showPlanMatch = false }
            )
        } else if (showCreateTeam && userProfile != null) {
            CreateTeamScreen(
                currentUser = userProfile!!,
                allUsers = allUsers,
                availableSports = availableSports,
                onPickIcon = { callback ->
                    iconPickerCallback = callback
                    showIconPicker = true
                },
                onBack = { showCreateTeam = false },
                onSave = { team, imageUri ->
                    userViewModel.createTeam(context, team, imageUri)
                    showCreateTeam = false
                }
            )
        } else if (selectedTeamForDetails != null && userProfile != null) {
            TeamDetailsScreen(
                team = selectedTeamForDetails!!,
                currentUser = userProfile!!,
                allUsers = allUsers,
                availableSports = availableSports,
                onPickIcon = { callback ->
                    iconPickerCallback = callback
                    showIconPicker = true
                },
                onBack = { selectedTeamForDetails = null },
                onSave = { updatedTeam, imageUri ->
                    userViewModel.updateTeam(context, updatedTeam, imageUri)
                    selectedTeamForDetails = null
                },
                onDelete = { teamId ->
                    userViewModel.deleteTeam(context, teamId)
                    selectedTeamForDetails = null
                }
            )
        } else if (matchToEdit != null) {
            EditMatchScreen(
                match = matchToEdit!!,
                availableSports = availableSports,
                userTeams = userTeams,
                allTeams = allTeams,
                allUsers = allUsers,
                onSave = { updatedMatch ->
                    userViewModel.updateMatch(context, updatedMatch)
                    matchToEdit = null
                    selectedMatchForDetails = updatedMatch
                },
                onBack = { matchToEdit = null }
            )
        } else if (selectedMatchForDetails != null) {
            MatchDetailsScreen(
                match = selectedMatchForDetails!!,
                allUsers = allUsers,
                onBack = { selectedMatchForDetails = null },
                onEdit = { matchToEdit = selectedMatchForDetails },
                onSaveResults = { scoreMyTeam, scoreOpponent ->
                    userViewModel.updateMatch(context, selectedMatchForDetails!!.copy(
                        scoreMyTeam = scoreMyTeam,
                        scoreOpponent = scoreOpponent
                    ))
                    selectedMatchForDetails = null
                },
                onCancelMatch = {
                    userViewModel.cancelMatch(context, selectedMatchForDetails!!.id)
                    selectedMatchForDetails = null
                }
            )
        } else {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    item(
                        icon = { 
                            Icon(
                                AppDestinations.ACCOUNT.icon, 
                                null,
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
                    item(
                        icon = { Icon(AppDestinations.HOME.icon, null) },
                        label = { Text(AppDestinations.HOME.label) },
                        selected = currentDestination == AppDestinations.HOME,
                        onClick = { currentDestination = AppDestinations.HOME }
                    )
                    item(
                        icon = { 
                            Icon(
                                AppDestinations.TEAMS.icon, 
                                null,
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
                    topBar = { if (userProfile != null) UserProfileTopBar(userProfile!!) }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentDestination) {
                            AppDestinations.HOME -> HomeScreen(
                                userProfile = userProfile,
                                plannedMatches = plannedMatches,
                                allTeams = allTeams,
                                isRefreshing = isRefreshing,
                                onPlanMatchClick = { showPlanMatch = true },
                                onSignInClick = { userViewModel.signIn(context) },
                                onMatchClick = { match -> selectedMatchForDetails = match },
                                onRefresh = { userViewModel.refreshData() }
                            )
                            AppDestinations.ACCOUNT -> AccountScreen(
                                userProfile = userProfile,
                                onUpdateClick = { username, gender, age, city ->
                                    userViewModel.updateProfile(context, username, gender, age, city)
                                },
                                onSignInClick = { userViewModel.signIn(context) },
                                onSignOutClick = { userViewModel.signOut(context) }
                            )
                            AppDestinations.TEAMS -> TeamsScreen(
                                teams = userTeams,
                                onTeamClick = { team -> selectedTeamForDetails = team },
                                onCreateTeamClick = { showCreateTeam = true }
                            )
                        }
                    }
                }
            }
        }

        // Overlay Icon Picker
        if (showIconPicker) {
            IconPickerScreen(
                onIconSelected = { iconUrl ->
                    iconPickerCallback?.invoke(iconUrl)
                    showIconPicker = false
                },
                onBack = { showIconPicker = false }
            )
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    ACCOUNT("Account", Icons.Default.Person),
    HOME("Home", Icons.Default.Home),
    TEAMS("Teams", Icons.Default.Groups),
}
