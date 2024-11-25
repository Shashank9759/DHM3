package com.example.dhm20

import android.content.ContentValues.TAG
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dhm20.Utils.CLIENT_ID
import com.google.android.gms.auth.api.identity.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth

import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

@Composable
fun SignInScreen(navController: NavController, auth: FirebaseAuth) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign in to your account")

        Spacer(modifier = Modifier.height(20.dp))

        val context = LocalContext.current

        val onClick: () -> Unit = {
            val credentialManager = CredentialManager.create(context)

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(CLIENT_ID)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            coroutineScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context,
                    )
                    val credential = result.credential

                    val googleIdTokenCredential = GoogleIdTokenCredential
                        .createFrom(credential.data)

                    val googleIdToken = googleIdTokenCredential.idToken

                    Log.i(TAG, googleIdToken)

                    // Sign in with Firebase using the ID token
                    signInWithGoogle(googleIdToken)

                    Toast.makeText(context, "You are signed in!", Toast.LENGTH_SHORT).show()

                    // Navigate to your DHM main screen
                    navController.navigate("home")

                } catch (e: GetCredentialException) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                } catch (e: GoogleIdTokenParsingException) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Google Sign-In Button
        Button(onClick = onClick) {
            Text("Sign in with Google")
        }
    }
}

suspend fun signInWithGoogle(idToken: String) {
    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
    Firebase.auth.signInWithCredential(firebaseCredential).await()
}