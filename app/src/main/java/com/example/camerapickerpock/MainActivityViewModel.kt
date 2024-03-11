package com.example.camerapickerpock

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainActivityViewModel: ViewModel() {

    var hasFile :MutableState <Boolean> = mutableStateOf(false)
    var fileName :MutableState <String> = mutableStateOf("")
    var fileSize :MutableState <String> = mutableStateOf("")
    var uri : MutableState<Uri?> = mutableStateOf(null)
    var outputFileUri: MutableState<Uri?> = mutableStateOf(null)

    fun clearData() {
        hasFile.value = false
        fileName.value = ""
        fileSize.value = ""
        uri.value = null
        outputFileUri.value = null
    }

}