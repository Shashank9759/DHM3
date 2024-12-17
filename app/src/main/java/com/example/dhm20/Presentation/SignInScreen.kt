package com.example.dhm20.Presentation

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dhm20.R
import com.example.dhm20.Utils.CLIENT_ID
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



@Preview()
@Composable
fun preview(){
    val NavController= rememberNavController()
    SignInScreen(NavController)
}


@Composable
fun SignInScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {




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
                    signInWithGoogle(context,googleIdToken)


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
        Image(
            painter = painterResource(id = R.drawable.app_logo), // Reference your logo drawable here
            contentDescription = "App Logo", // A description for accessibility
            modifier = Modifier.size(150.dp) // Adjust the size as per your requirement
        )


        IconButton(
            onClick = onClick, // Set the onClick listener
            modifier = Modifier.size(180.dp) // Set the size of the button


        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_signin), // Replace with your drawable
                contentDescription = "Google Sign-In Icon",
                tint = Color.Unspecified, // Preserve original colors of the icon
                modifier = Modifier.size(180.dp) // Set the icon size

            )
        }
    }
}

suspend fun signInWithGoogle(context: Context,idToken: String) {
    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
    Firebase.auth.signInWithCredential(firebaseCredential).await()
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid

    storeIdToken(context,firebaseUid as String)
}
fun storeIdToken(context: Context, idToken: String) {
    val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString("uid_token", idToken)
        apply()
    }
}
