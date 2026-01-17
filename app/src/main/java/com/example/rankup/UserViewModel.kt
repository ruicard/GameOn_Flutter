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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfile(
    val name: String?,
    val email: String,
    val profilePictureUrl: String?
)

class UserViewModel : ViewModel() {
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    fun signIn(context: Context) {
        val credentialManager = CredentialManager.create(context)

        // Error 10: [28444] is "DEVELOPER_ERROR". 
        // This almost always means the SHA-1 or Package Name in the Google Cloud Console 
        // does not match your app's signature.
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            // MUST be a "Web Client ID" from the Google Cloud Console
            .setServerClientId("5591467195110-mta77nrn25lqj7gns75cvuf809b83100.apps.googleusercontent.com")
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
                    _userProfile.value = UserProfile(
                        name = credential.displayName,
                        email = credential.id,
                        profilePictureUrl = credential.profilePictureUri?.toString()
                    )
                    Toast.makeText(context, "Welcome ${credential.displayName}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NoCredentialException) {
                Log.e("UserViewModel", "No credentials available. Are you signed into Google on this device?")
                Toast.makeText(context, "No accounts found on device", Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialException) {
                // This is where Error 10 is caught
                Log.e("UserViewModel", "SignIn Error (Code ${e.type}): ${e.message}", e)
                handleDeveloperError(context, e)
            } catch (e: Exception) {
                Log.e("UserViewModel", "Unknown Error: ${e.message}", e)
            }
        }
    }

    private fun handleDeveloperError(context: Context, e: GetCredentialException) {
        val msg = if (e.message?.contains("10") == true) {
            "Developer Error (10): Check SHA-1 and Package Name in Google Console."
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
}
