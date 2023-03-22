package com.example.camerax

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.camerax.PictureModule.PictureContainer

class jpegViewModel : ViewModel() {
    var jpegContainer = MutableLiveData<PictureContainer>()

    fun setContainer(container: PictureContainer) {
        jpegContainer.value = container
    }
}