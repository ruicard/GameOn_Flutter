package com.example.rankup

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.data.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

enum class MatchStatus {
    SCHEDULED, CONFIRMED, INPROGRESS, COMPLETED, CANCELLED
}

enum class InvitationStatus {
    NO_ANSWER, ACCEPTED, TENTATIVE, DECLINED
}

data class UserProfile(
    val id: String = "",
    val name: String? = null,
    val email: String = "",
    val profilePictureUrl: String? = null,
    val username: String = "",
    val gender: String = Gender.UNKNOWN.name,
    val ageGroup: String = AgeGroup.SENIOR_18_45.name,
    val city: String = "",
    val fcmToken: String = ""
)

data class PlannedMatch(
    val id: String = UUID.randomUUID().toString(),
    val modality: String = "",
    val matchType: String = "",
    val dateTime: String = "",
    val location: String = "",
    val myTeam: String = "",
    val opponent: String = "",
    val createdByUserId: String = "",
    val invitedPlayers: List<String> = emptyList(),
    val scoreMyTeam: Int? = null,
    val scoreOpponent: Int? = null,
    val status: String = MatchStatus.SCHEDULED.name,
    val teamAPlayers: List<String> = emptyList(),
    val teamBPlayers: List<String> = emptyList(),
    val playerInvitations: Map<String, String> = emptyMap()
)

data class PlannedTeam(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val sport: String = "",
    val members: List<String> = emptyList(),
    val creatorId: String = "",
    val gender: String = Gender.UNKNOWN.name,
    val ageGroup: String = AgeGroup.SENIOR_18_45.name,
    val city: String = "",
    val captainId: String = "",
    val profilePictureUrl: String? = null
)

data class SportModel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    @get:PropertyName("maxPlayersPerTeam") @set:PropertyName("maxPlayersPerTeam") var maxPlayersTeam: Int = 5,
    @get:PropertyName("minPlayersPerMatch") @set:PropertyName("minPlayersPerMatch") var minPlayersMatch: Int = 2
)

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersCollection = db.collection("users")
    private val matchesCollection = db.collection("matches")
    private val teamsCollection = db.collection("teams")
    private val sportsCollection = db.collection("sports")

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _plannedMatches = MutableStateFlow<List<PlannedMatch>>(emptyList())
    val plannedMatches: StateFlow<List<PlannedMatch>> = _plannedMatches.asStateFlow()

    private val _userTeams = MutableStateFlow<List<PlannedTeam>>(emptyList())
    val userTeams: StateFlow<List<PlannedTeam>> = _userTeams.asStateFlow()

    private val _allTeams = MutableStateFlow<List<PlannedTeam>>(emptyList())
    val allTeams: StateFlow<List<PlannedTeam>> = _allTeams.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    private val _availableSports = MutableStateFlow<List<SportModel>>(emptyList())
    val availableSports: StateFlow<List<SportModel>> = _availableSports.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var isSigningIn = false

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val email = currentUser.email ?: return@launch
                    val userDoc = usersCollection.document(email).get().await()
                    if (userDoc.exists()) {
                        _userProfile.value = userDoc.toObject<UserProfile>()
                        listenToMatches()
                        listenToTeams()
                        fetchAllUsers()
                        fetchAllTeams()
                        fetchSports()
                        // Always refresh the FCM token on auto-login so Firestore stays up-to-date
                        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                            saveFcmToken(email, token)
                            Log.d("UserViewModel", "FCM token refreshed on auto-login")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UserViewModel", "Auto-login failed: ${e.message}")
                } finally {
                    _isInitializing.value = false
                }
            }
        } else {
            _isInitializing.value = false
            fetchSports()
        }
    }

    fun refreshData() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val email = currentUser.email ?: ""
                    if (email.isNotEmpty()) {
                        val userDoc = usersCollection.document(email).get().await()
                        if (userDoc.exists()) {
                            _userProfile.value = userDoc.toObject<UserProfile>()
                        }
                    }
                }
                fetchAllUsers()
                val sportsSnapshot = sportsCollection.get().await()
                _availableSports.value = sportsSnapshot.documents.mapNotNull { it.toObject<SportModel>()?.copy(id = it.id) }.sortedBy { it.name }
                delay(1000)
            } catch (e: Exception) {
                Log.e("UserViewModel", "Refresh failed: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun fetchSports() {
        viewModelScope.launch {
            try {
                sportsCollection.addSnapshotListener { snapshot, e ->
                    if (snapshot != null) {
                        _availableSports.value = snapshot.documents.mapNotNull { it.toObject<SportModel>()?.copy(id = it.id) }.sortedBy { it.name }
                    }
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error fetching sports: ${e.message}")
            }
        }
    }

    private fun fetchAllUsers() {
        viewModelScope.launch {
            try {
                val snapshot = usersCollection.get().await()
                _allUsers.value = snapshot.documents.mapNotNull { it.toObject<UserProfile>() }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error fetching users: ${e.message}")
            }
        }
    }

    private fun fetchAllTeams() {
        teamsCollection.addSnapshotListener { snapshot, e ->
            if (snapshot != null) {
                _allTeams.value = snapshot.documents.mapNotNull { it.toObject<PlannedTeam>() }
            }
        }
    }

    fun signIn(context: Context) {
        if (isSigningIn) return
        isSigningIn = true

        val credentialManager = CredentialManager.create(context)
        val serverClientId = context.getString(R.string.default_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (
                    credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val firebaseUser = authResult.user ?: throw Exception("Firebase Auth failed")
                    val email = firebaseUser.email ?: googleCredential.id
                    val userDoc = usersCollection.document(email).get().await()
                    if (userDoc.exists()) {
                        _userProfile.value = userDoc.toObject<UserProfile>()
                    } else {
                        val newProfile = UserProfile(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName,
                            email = email,
                            profilePictureUrl = firebaseUser.photoUrl?.toString(),
                            username = firebaseUser.displayName ?: ""
                        )
                        usersCollection.document(email).set(newProfile).await()
                        _userProfile.value = newProfile
                    }
                    listenToMatches()
                    listenToTeams()
                    fetchAllUsers()
                    fetchAllTeams()
                    fetchSports()
                    // Save FCM token so this user can receive push notifications
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        saveFcmToken(email, token)
                    }
                    Toast.makeText(context, "Welcome ${_userProfile.value?.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("UserViewModel", "Unexpected credential type: ${credential::class.java.simpleName}")
                    Toast.makeText(context, "Google Sign-In unavailable on this device", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NoCredentialException) {
                Log.e("UserViewModel", "No Google account available for sign-in: ${e.message}")
                Toast.makeText(context, "No Google account found on emulator", Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialException) {
                Log.e("UserViewModel", "Credential Manager error: ${e.message}")
                Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "SignIn Error: ${e.message}")
            } finally {
                isSigningIn = false
            }
        }
    }

    private fun listenToMatches() {
        matchesCollection.addSnapshotListener { snapshot, e ->
            if (snapshot != null) {
                val now = Calendar.getInstance().time
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val matches = snapshot.documents.mapNotNull { it.toObject<PlannedMatch>() }
                
                // Identify matches that should be marked as COMPLETED
                matches.forEach { match ->
                    try {
                        val matchDate = sdf.parse(match.dateTime)
                        if (matchDate != null && matchDate.before(now) && 
                            match.status != MatchStatus.COMPLETED.name && 
                            match.status != MatchStatus.CANCELLED.name) {
                            // Update status in Firestore
                            matchesCollection.document(match.id).update("status", MatchStatus.COMPLETED.name)
                        }
                    } catch (ex: Exception) {
                        Log.e("UserViewModel", "Error parsing date for match status update: ${ex.message}")
                    }
                }

                _plannedMatches.value = matches.sortedBy { 
                    try { sdf.parse(it.dateTime)?.time ?: Long.MAX_VALUE } catch (ex: Exception) { Long.MAX_VALUE }
                }
            }
        }
    }

    private fun listenToTeams() {
        val user = _userProfile.value ?: return
        teamsCollection.whereArrayContains("members", user.id).addSnapshotListener { snapshot, e ->
            if (snapshot != null) {
                _userTeams.value = snapshot.documents.mapNotNull { it.toObject<PlannedTeam>() }
            }
        }
    }

    fun updateProfile(context: Context, username: String, gender: Gender, ageGroup: AgeGroup, city: String) {
        val current = _userProfile.value ?: return
        viewModelScope.launch {
            try {
                val updatedProfile = current.copy(username = username, gender = gender.name, ageGroup = ageGroup.name, city = city)
                usersCollection.document(current.email).set(updatedProfile).await()
                _userProfile.value = updatedProfile
                Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Update Error: ${e.message}")
            }
        }
    }

    fun saveFcmToken(userEmail: String, token: String) {
        viewModelScope.launch {
            try {
                usersCollection.document(userEmail)
                    .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                Log.d("UserViewModel", "FCM token saved for $userEmail")
            } catch (e: Exception) {
                Log.e("UserViewModel", "FCM token save error: ${e.message}")
            }
        }
    }

    fun signOut(context: Context) {
        val credentialManager = CredentialManager.create(context)
        viewModelScope.launch {
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _userProfile.value = null
            _plannedMatches.value = emptyList()
            _userTeams.value = emptyList()
        }
    }

    fun addMatch(context: Context, match: PlannedMatch) {
        val user = _userProfile.value ?: return
        viewModelScope.launch {
            try {
                val finalInvitedPlayers = match.invitedPlayers.toMutableSet()
                
                if (match.matchType == "Team") {
                    val myTeamObj = _allTeams.value.find { it.name == match.myTeam }
                    val opponentObj = _allTeams.value.find { it.name == match.opponent }
                    
                    myTeamObj?.members?.let { finalInvitedPlayers.addAll(it) }
                    opponentObj?.members?.let { finalInvitedPlayers.addAll(it) }
                }
                
                val initialInvitations = finalInvitedPlayers.associateWith { InvitationStatus.NO_ANSWER.name }
                
                val matchWithUser = match.copy(
                    createdByUserId = user.id,
                    invitedPlayers = finalInvitedPlayers.toList(),
                    playerInvitations = initialInvitations,
                    status = MatchStatus.SCHEDULED.name
                )
                matchesCollection.document(matchWithUser.id).set(matchWithUser).await()
                Toast.makeText(context, "Match saved!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Add Match Error: ${e.message}")
            }
        }
    }

    private fun normalizeInvitationState(match: PlannedMatch): PlannedMatch {
        // For Team matches, always rebuild invitedPlayers from the current team rosters
        // so that adding/changing teams immediately reflects in the invitation list.
        val baseInvitedPlayers: List<String> = if (match.matchType == "Team") {
            val myTeamMembers = _allTeams.value.find { it.name == match.myTeam }?.members ?: emptyList()
            val opponentMembers = _allTeams.value.find { it.name == match.opponent }?.members ?: emptyList()
            (myTeamMembers + opponentMembers).distinct()
        } else {
            match.invitedPlayers.distinct()
        }

        // Preserve existing answer statuses; new players get NO_ANSWER.
        val mergedInvitations = baseInvitedPlayers.associateWith { playerId ->
            match.playerInvitations[playerId] ?: InvitationStatus.NO_ANSWER.name
        }

        // Remove players from team-slot assignments if they are no longer invited.
        val cleanedTeamA = match.teamAPlayers.filter { it in baseInvitedPlayers }
        val cleanedTeamB = match.teamBPlayers.filter { it in baseInvitedPlayers }

        return match.copy(
            invitedPlayers = baseInvitedPlayers,
            playerInvitations = mergedInvitations,
            teamAPlayers = cleanedTeamA,
            teamBPlayers = cleanedTeamB
        )
    }

    fun updateMatch(context: Context, match: PlannedMatch) {
        viewModelScope.launch {
            try {
                val normalizedMatch = normalizeInvitationState(match)
                matchesCollection.document(normalizedMatch.id).set(normalizedMatch).await()
                Toast.makeText(context, "Match updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Update Match Error: ${e.message}")
            }
        }
    }

    fun cancelMatch(context: Context, matchId: String) {
        viewModelScope.launch {
            try {
                // Delete the match document from Firestore
                matchesCollection.document(matchId).delete().await()
                Toast.makeText(context, "Match cancelled and deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Cancel Match Error: ${e.message}")
            }
        }
    }

    private suspend fun uploadTeamPicture(teamId: String, imageUri: Uri): String {
        val ref = storage.reference.child("teams/$teamId.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    fun createTeam(context: Context, team: PlannedTeam, imageUri: Uri? = null) {
        viewModelScope.launch {
            try {
                var finalTeam = team
                if (imageUri != null) {
                    val url = uploadTeamPicture(team.id, imageUri)
                    finalTeam = team.copy(profilePictureUrl = url)
                }
                teamsCollection.document(finalTeam.id).set(finalTeam).await()
                Toast.makeText(context, "Team created!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Create Team Error: ${e.message}")
            }
        }
    }

    fun updateTeam(context: Context, team: PlannedTeam, imageUri: Uri? = null) {
        viewModelScope.launch {
            try {
                var finalTeam = team
                if (imageUri != null) {
                    val url = uploadTeamPicture(team.id, imageUri)
                    finalTeam = team.copy(profilePictureUrl = url)
                }
                teamsCollection.document(finalTeam.id).set(finalTeam).await()
                Toast.makeText(context, "Team updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Update Team Error: ${e.message}")
            }
        }
    }

    fun deleteTeam(context: Context, teamId: String) {
        viewModelScope.launch {
            try {
                teamsCollection.document(teamId).delete().await()
                Toast.makeText(context, "Team deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Delete Team Error: ${e.message}")
            }
        }
    }
}
