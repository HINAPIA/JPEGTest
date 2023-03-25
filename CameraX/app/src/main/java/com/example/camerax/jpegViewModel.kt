package com.example.camerax

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.camerax.PictureModule.Container

class jpegViewModel : ViewModel() {
    var jpegContainer = MutableLiveData<Container>()

    fun setContainer(container: Container) {
        jpegContainer.value = container
    }
}