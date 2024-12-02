package com.example.dhm20.Presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController


val toggleStates = mutableStateMapOf(
        "Running" to true,
        "Walking" to true,
        "Vehicle" to true,
        "Sleep" to true,
        "Microphone" to true,
         "Location" to true
    )

@Preview()
@Composable
fun preview1(){
    val NavController= rememberNavController()
    ActivityToggleScreen(){
        activity, isChecked ->
    }
}

@Composable
fun ActivityToggleScreen(
    onToggleChanged: (String, Boolean) -> Unit
) {


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(text = "Activity Toggles",  fontWeight = FontWeight.Bold,
            fontSize = 30.sp, style = MaterialTheme.typography.bodyLarge)

        // Use forEach to iterate over entries of the map
        toggleStates.entries.forEach { entry ->
            key(entry.key) { // Add a key for better recomposition
                ToggleRow(activity = entry.key, isEnabled = entry.value) { isChecked ->
                    toggleStates[entry.key] = isChecked
                    onToggleChanged(entry.key, isChecked)
                }
            }
        }
    }
}

@Composable
fun ToggleRow(
    activity: String,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = activity, modifier = Modifier.wrapContentSize(), fontSize = 24.sp, style = MaterialTheme.typography.bodySmall)
        Switch(checked = isEnabled, onCheckedChange = onCheckedChange
        , colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Blue,      // Blue color when checked
                uncheckedThumbColor = Color.Gray,   // Gray color when unchecked
                checkedTrackColor = Color.LightGray, // Light gray for track when checked
                uncheckedTrackColor = Color.DarkGray // Dark gray for track when unchecked
            ))
    }
}
