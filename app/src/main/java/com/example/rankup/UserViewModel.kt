package com.example.rankup

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
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
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class UserProfile(
    val id: String = "",
    val name: String? = null,
    val email: String = "",
    val profilePictureUrl: String? = null,
    val username: String = "",
    val gender: String = Gender.UNKNOWN.name,
    val ageGroup: String = AgeGroup.SENIOR_18_45.name,
    val city: String = ""
)

data class PlannedMatch(
    val id: String = UUID.randomUUID().toString(),
    val modality: String = "",
    val matchType: String = "",
    val dateTime: String = "",
    val location: String = "",
    val myTeam: String = "",
    val opponent: String = "",
    val createdByUserId: String = ""
)

data class PlannedTeam(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val sport: String = "",
    val members: List<String> = emptyList(),
    val creatorId: String = ""
)

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val matchesCollection = db.collection("matches")
    private val teamsCollection = db.collection("teams")

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _plannedMatches = MutableStateFlow<List<PlannedMatch>>(emptyList())
    val plannedMatches: StateFlow<List<PlannedMatch>> = _plannedMatches.asStateFlow()

    private val _userTeams = MutableStateFlow<List<PlannedTeam>>(emptyList())
    val userTeams: StateFlow<List<PlannedTeam>> = _userTeams.asStateFlow()

    private var isSigningIn = false

    init {
        // Automatic login if session exists
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
                    }
                } catch (e: Exception) {
                    Log.e("UserViewModel", "Auto-login failed: ${e.message}")
                }
            }
        }
    }

    fun signIn(context: Context) {
        if (isSigningIn) return
        isSigningIn = true

        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("591467195110-mta77nrn25lqj7gns75cvuf809b83100.apps.googleusercontent.com")
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                
                if (credential is GoogleIdTokenCredential) {
                    val idToken = credential.idToken ?: throw Exception("No ID token found")
                    val email = credential.id ?: throw Exception("No email found")
                    val displayName = credential.displayName

                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val firebaseUser = authResult.user ?: throw Exception("Firebase Auth failed")
                    
                    val userDoc = usersCollection.document(email).get().await()

                    if (userDoc.exists()) {
                        _userProfile.value = userDoc.toObject<UserProfile>()
                    } else {
                        val newProfile = UserProfile(
                            id = firebaseUser.uid,
                            name = displayName ?: firebaseUser.displayName,
                            email = email,
                            profilePictureUrl = firebaseUser.photoUrl?.toString(),
                            username = displayName ?: ""
                        )
                        usersCollection.document(email).set(newProfile).await()
                        _userProfile.value = newProfile
                    }
                    
                    listenToMatches()
                    listenToTeams()
                    Toast.makeText(context, "Welcome ${_userProfile.value?.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "SignIn Error: ${e.message}", e)
                Toast.makeText(context, "Login failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isSigningIn = false
            }
        }
    }

    private fun listenToMatches() {
        // Listen to all matches (Global pool for interaction)
        matchesCollection.addSnapshotListener { snapshot, e ->
            if (snapshot != null) {
                val matches = snapshot.documents.mapNotNull { it.toObject<PlannedMatch>() }
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                _plannedMatches.value = matches.sortedBy { 
                    try { sdf.parse(it.dateTime)?.time ?: Long.MAX_VALUE } catch (ex: Exception) { Long.MAX_VALUE }
                }
            }
        }
    }

    private fun listenToTeams() {
        val user = _userProfile.value ?: return
        // Listen to teams where user is a creator or member
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
                val updatedProfile = current.copy(
                    username = username,
                    gender = gender.name,
                    ageGroup = ageGroup.name,
                    city = city
                )
                usersCollection.document(current.email).set(updatedProfile).await()
                _userProfile.value = updatedProfile
                Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Update Error: ${e.message}")
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
                val matchWithUser = match.copy(createdByUserId = user.id)
                matchesCollection.document(matchWithUser.id).set(matchWithUser).await()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Add Match Error: ${e.message}")
            }
        }
    }
}
