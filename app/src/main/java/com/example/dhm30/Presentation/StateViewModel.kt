package com.example.dhm30.Presentation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StateViewModel(val savedStateHandle: SavedStateHandle) : ViewModel() {

    // Using SavedStateHandle to store and retrieve data
    private val _authState = savedStateHandle.getStateFlow("auth_state", false)
    val authState: StateFlow<Boolean> = _authState

    fun updateAuthState(state: Boolean) {
        // Correct way to update the value in SavedStateHandle
        savedStateHandle.set("auth_state", state)
    }
}