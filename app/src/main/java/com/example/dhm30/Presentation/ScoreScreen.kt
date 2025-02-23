package com.example.dhm30.Presentation


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.dhm30.R
import androidx.compose.runtime.getValue

import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState

@Preview(showBackground = true)
@Composable
fun preview4(){


  //  ScoreScreen(10)
}

@Composable
fun ScoreScreen(score: Int,navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center   ,
        horizontalAlignment = Alignment.CenterHorizontally // Align content horizontally
    ) {
        // Text Display
        Text(
            text = "Your Score is :",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 45.sp),
            modifier = Modifier.padding(bottom = 10.dp)

        )
        Text(
            text = "$score",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 120.sp),
            modifier = Modifier.padding(bottom = 8.dp)

        )

        // Load the Lottie animation
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.animation_achviement)
        )

        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever // Repeat animation infinitely
        )

        // Centered Lottie Animation
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(250.dp) // Set a fixed size
        ) {
            LottieAnimation(
                composition = composition,
                progress = progress
            )
        }


        Button(onClick = {
           navController.navigate("home")
        }, modifier = Modifier.padding(10.dp).width(300.dp), colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary, // Primary color from theme
            contentColor = MaterialTheme.colorScheme.onPrimary // Text color based on primary color
        ) ) {
            Text("Continue", modifier = Modifier.padding(5.dp))
        }
    }

}


