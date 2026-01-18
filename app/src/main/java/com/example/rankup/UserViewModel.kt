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
import kotlinx.coroutines.launch
import java.util.UUID

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
                    
                    // Check if user exists in database
                    val existingUser = userDao.getUserByEmail(email)
                    
                    if (existingUser != null) {
                        // Load existing user info
                        _userProfile.value = UserProfile(
                            id = existingUser.id,
                            name = credential.displayName, // Always take fresh name from Google
                            email = email,
                            profilePictureUrl = credential.profilePictureUri?.toString(),
                            username = existingUser.username,
                            gender = existingUser.gender,
                            ageGroup = existingUser.ageGroup,
                            city = existingUser.city
                        )
                    } else {
                        // Create new user in database
                        val newUser = User(
                            username = credential.displayName ?: "",
                            email = email,
                            hashedPassword = "", // Google handles auth
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
                    }
                    Toast.makeText(context, "Welcome ${credential.displayName}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NoCredentialException) {
                Log.e("UserViewModel", "No credentials available.")
                Toast.makeText(context, "No accounts found", Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialException) {
                Log.e("UserViewModel", "SignIn Error: ${e.message}")
                if (e.message?.contains("cancelled") == false) {
                    handleDeveloperError(context, e)
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Unknown Error: ${e.message}")
            } finally {
                isSigningIn = false
            }
        }
    }

    fun updateProfile(context: Context, username: String, gender: Gender, ageGroup: AgeGroup, city: String) {
        val current = _userProfile.value ?: return
        val db = AppDatabase.getDatabase(context)
        val userDao = db.userDao()

        viewModelScope.launch {
            try {
                // Update local state
                _userProfile.value = current.copy(
                    username = username,
                    gender = gender,
                    ageGroup = ageGroup,
                    city = city
                )

                // Update database
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
                userDao.insertUser(userToUpdate) // insert with REPLACE strategy
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Update Error: ${e.message}")
                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleDeveloperError(context: Context, e: GetCredentialException) {
        val msg = if (e.message?.contains("10") == true) {
            "Developer Error (10): Check SHA-1/Package Name."
        } else {
            "Login failed: ${e.message}"
        }
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    fun signOut(context: Context) {
        val credentialManager = CredentialManager.create(context)
        viewModelScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                _userProfile.value = null
                Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("UserViewModel", "SignOut Error: ${e.message}")
            }
        }
    }

    fun addMatch(match: PlannedMatch) {
        _plannedMatches.value = _plannedMatches.value + match
    }
}
