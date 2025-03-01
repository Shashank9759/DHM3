package com.example.dhm30.Presentation

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.annotations.ApiStatus.Experimental
import androidx.compose.runtime.getValue

import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.example.dhm30.Data.Database.AudioDB
import com.example.dhm30.Data.Database.SurveyDb
import com.example.dhm30.Data.Entities.SurveyLog
import com.example.dhm30.TrackingService.Companion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

var isSurveyReceived by mutableStateOf(false)
@Preview(showBackground = true)
@Composable
fun preview3(){
    val list= listOf("what is your name","kfhiuhf","fgfrifhkifu","fjgkhkhku")
  //  SurveyScreen(list)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyScreen(questions: List<String>, navController: NavController) {

    val context= LocalContext.current
    // State variables
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, String>() }
    val ratings = remember { mutableStateMapOf<Int, Int>() }  // Store ratings
    val options = listOf("None of the time", "Rarely", "Some of the time", "Often", "All of the time")


    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Survey") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Question and Progress
            Column {
                Text(
                    text = "Question ${currentQuestionIndex + 1}/${questions.size}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = questions[currentQuestionIndex],
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
               if(currentQuestionIndex==questions.size-1){
                   val textState = remember { mutableStateOf("") }

                   TextField(
                       value = textState.value,
                       onValueChange = { newText -> textState.value = newText
                           answers[currentQuestionIndex] = textState.value},
                       label = { Text("Enter text") },
                       placeholder = { Text("Type here...") }
                   )
               }else{
                   // Options
                   options.forEachIndexed { index,option ->
                       Row(
                           modifier = Modifier
                               .fillMaxWidth()
                               .clickable { answers[currentQuestionIndex] = option },
                           verticalAlignment = Alignment.CenterVertically
                       ) {
                           RadioButton(
                               selected = answers[currentQuestionIndex] == option,
                               onClick = { answers[currentQuestionIndex] = option
                                   ratings[currentQuestionIndex] = index+1}
                           )

                           Text(text = option, modifier = Modifier.padding(start = 8.dp))
                       }
                   }
               }


//
//                // Rating Bar (Slider)
//                Text(
//                    text = "Rate this question",
//                    style = MaterialTheme.typography.bodyMedium,
//                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
//                )
//
//                Slider(
//                    value = ratings[currentQuestionIndex]?.toFloat() ?: 1f,
//                    onValueChange = { newValue ->
//                        // Convert the slider value to an integer and store it
//                        ratings[currentQuestionIndex] = newValue.toInt()
//                    },
//                    valueRange = 1f..10f,
//                    steps = 8,  // 9 possible values (1, 2, 3, ..., 10)
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Text(
//                    text = "Rating: ${ratings[currentQuestionIndex] ?: 1}",
//                    style = MaterialTheme.typography.bodySmall,
//                    modifier = Modifier.padding(top = 4.dp)
//                )
            }





        // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentQuestionIndex > 0) {
                    Button(onClick = { currentQuestionIndex-- }) {
                        Text("Prev")
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp)) // Placeholder for alignment
                }

                if (currentQuestionIndex < questions.size - 1) {
                    Button(onClick = {
                        if(answers[currentQuestionIndex]==null){
                            Toast.makeText(context,"Select Any Option",Toast.LENGTH_SHORT).show()
                        }else{
                            currentQuestionIndex++
                        }

                    
                    }) {
                        Text("Next")
                    }
                } else {
                    Button(onClick = {
                        if(answers[currentQuestionIndex]==null){
                            Toast.makeText(context,"Select Any Option",Toast.LENGTH_SHORT).show()
                        }else{
                            val db = SurveyDb.getInstance(context)
                            val dao = db.surveyLogDao()
                            var score=0;
                             val ansermap= answers.mapKeys {
                                 it.key.toString()
                             }
                            val ratingmap=ratings.mapKeys {
                                it.key.toString()
                            }
                            ratings.forEach{index,value->
                                score+=value

                            }
                            CoroutineScope(Dispatchers.IO).launch{
                                dao.insert(SurveyLog(
                                    answersMap=ansermap,
                                    ratingsMap= ratingmap,
                                    timestamp=  SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(
                                        Date()
                                    ),
                                    finalscore= score
                                ))
                                Log.d("finalscore",score.toString())
                            }

                            
                            // Mark the survey as completed in shared preferences

                            val sharedPreferences = context.getSharedPreferences("survey_prefs", Context.MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putBoolean("survey_completed", true)
                                apply()
                            }
                            isSurveyReceived = true


                            navController.navigate("home")
                        }

                        Log.d("Survey", "Answers: $answers")
                    }) {
                        Text("Submit")
                    }
                }
            }
        }
    }}

