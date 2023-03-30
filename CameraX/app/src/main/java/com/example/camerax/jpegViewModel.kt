package com.example.camerax

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.camerax.PictureModule.MCContainer

class jpegViewModel : ViewModel() {

    var jpegMCContainer = MutableLiveData<MCContainer>()

    private var imageUriList = mutableListOf<String>() // 임시 데이터(원본이 사라져도)
    private val _imageUriLiveData = MutableLiveData<List<String>>() // 내부처리 데이터
    val imageUriLiveData: LiveData<List<String>> get() = _imageUriLiveData // client 읽기 전용

    fun setContainer(MCContainer: MCContainer) {
        jpegMCContainer.value = MCContainer
    }

    fun updateImageUriData(uriList: MutableList<String>) {
        this.imageUriList = uriList
        _imageUriLiveData.value = imageUriList
    }
}