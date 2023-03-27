package com.example.camerax

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.camerax.PictureModule.MCContainer

class jpegViewModel : ViewModel() {
    var jpegMCContainer = MutableLiveData<MCContainer>()

    fun setContainer(MCContainer: MCContainer) {
        jpegMCContainer.value = MCContainer
    }
}