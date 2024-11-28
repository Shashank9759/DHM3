package com.example.dhm20.Presentation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StateViewModel: ViewModel() {
    private var _authState = MutableStateFlow<Boolean>(false)
    var authState = _authState.asStateFlow()






    fun updateMessage(newMessage: Boolean) {
        _authState.value = newMessage
    }
    fun getMessage():Boolean{
        return authState.value;
    }
}