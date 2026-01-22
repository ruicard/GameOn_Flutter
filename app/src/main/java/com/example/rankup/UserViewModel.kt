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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class UserProfile(
    val id: UUID,
    val name: String?,
    val email: String,
    val profilePictureUrl: String?,
    val username: String = "",
    val gender: Gender = Gender.UNKNOWN,
    val ageGroup: AgeGroup = AgeGroup.SENIOR_18_45,
    val city: String = ""
)

data class PlannedMatch(
    val id: UUID = UUID.randomUUID(),
    val modality: String,
    val matchType: String,
    val dateTime: String,
    val location: String,
    val myTeam: String,
    val opponent: String
)

class UserViewModel : ViewModel() {
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _plannedMatches = MutableStateFlow<List<PlannedMatch>>(emptyList())
    val plannedMatches: StateFlow<List<PlannedMatch>> = _plannedMatches.asStateFlow()

    private var isSigningIn = false

    fun signIn(context: Context) {
        if (isSigningIn) return
        isSigningIn = true

        val credentialManager = CredentialManager.create(context)
        val db = AppDatabase.getDatabase(context)
        val userDao = db.userDao()

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
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val email = credential.id
                    
                    val existingUser = userDao.getUserByEmail(email)
                    
                    if (existingUser != null) {
                        _userProfile.value = UserProfile(
                            id = existingUser.id,
                            name = credential.displayName,
                            email = email,
                            profilePictureUrl = credential.profilePictureUri?.toString(),
                            username = existingUser.username,
                            gender = existingUser.gender,
                            ageGroup = existingUser.ageGroup,
                            city = existingUser.city
                        )
                        loadMatches(context, existingUser.id)
                    } else {
                        val newUser = User(
                            username = credential.displayName ?: "",
                            email = email,
                            hashedPassword = "",
                            gender = Gender.UNKNOWN,
                            ageGroup = AgeGroup.SENIOR_18_45,
                            isCoach = false,
                            isAdmin = false,
                            city = ""
                        )
                        userDao.insertUser(newUser)
                        
                        _userProfile.value = UserProfile(
                            id = newUser.id,
                            name = credential.displayName,
                            email = email,
                            profilePictureUrl = credential.profilePictureUri?.toString(),
                            username = newUser.username
                        )
                        loadMatches(context, newUser.id)
                    }
                    Toast.makeText(context, "Welcome ${credential.displayName}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NoCredentialException) {
                Log.e("UserViewModel", "No credentials available.")
            } catch (e: GetCredentialException) {
                Log.e("UserViewModel", "SignIn Error: ${e.message}")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Unknown Error: ${e.message}")
            } finally {
                isSigningIn = false
            }
        }
    }

    private fun loadMatches(context: Context, userId: UUID) {
        val db = AppDatabase.getDatabase(context)
        viewModelScope.launch {
            db.matchDao().getMatchesByUser(userId).collectLatest { matches ->
                _plannedMatches.value = matches.map { 
                    PlannedMatch(
                        id = it.id,
                        modality = it.location ?: "Futsal", // Simplified for now
                        matchType = it.matchType.name,
                        dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it.scheduledDate)),
                        location = it.location ?: "",
                        myTeam = "Team HackYou", // Needs to be real
                        opponent = it.city ?: "Opponent" // Using city field as opponent for now
                    )
                }
            }
        }
    }

    fun updateProfile(context: Context, username: String, gender: Gender, ageGroup: AgeGroup, city: String) {
        val current = _userProfile.value ?: return
        val db = AppDatabase.getDatabase(context)
        val userDao = db.userDao()

        viewModelScope.launch {
            try {
                val userToUpdate = User(
                    id = current.id,
                    username = username,
                    email = current.email,
                    hashedPassword = "",
                    gender = gender,
                    ageGroup = ageGroup,
                    isCoach = false,
                    isAdmin = false,
                    city = city
                )
                userDao.insertUser(userToUpdate)
                _userProfile.value = current.copy(username = username, gender = gender, ageGroup = ageGroup, city = city)
                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Update Error: ${e.message}")
            }
        }
    }

    fun signOut(context: Context) {
        val credentialManager = CredentialManager.create(context)
        viewModelScope.launch {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _userProfile.value = null
            _plannedMatches.value = emptyList()
        }
    }

    fun addMatch(context: Context, match: PlannedMatch) {
        val user = _userProfile.value ?: return
        val db = AppDatabase.getDatabase(context)
        
        viewModelScope.launch {
            try {
                val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(match.dateTime)
                val matchEntity = Match(
                    id = match.id,
                    sportId = UUID.randomUUID(), // Placeholder
                    matchType = ParticipantType.TEAM,
                    scheduledDate = date?.time ?: System.currentTimeMillis(),
                    location = match.location,
                    city = match.opponent, // Store opponent in city field for this prototype
                    status = MatchStatus.SCHEDULED,
                    createdByUserId = user.id
                )
                db.matchDao().insertMatch(matchEntity)
                // Local state will update via loadMatches collection if we observe correctly
                // For now manual append to state flow to show immediate UI update
                _plannedMatches.value = _plannedMatches.value + match
            } catch (e: Exception) {
                Log.e("UserViewModel", "Add Match Error: ${e.message}")
            }
        }
    }
}
