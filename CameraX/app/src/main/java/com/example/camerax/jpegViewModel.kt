package com.example.camerax

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.camerax.PictureModule.MCContainer
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class jpegViewModel : ViewModel() {

    var jpegMCContainer = MutableLiveData<MCContainer>()

    private var imageUriList = mutableListOf<String>() // 임시 데이터(원본이 사라져도)
    private var imageDrawableList = mutableListOf<Drawable>() // 임시 데이터(원본이 사라져도)

    private val _imageUriLiveData = MutableLiveData<List<String>>() // 내부처리 데이터
    private val _imageDrawableLiveData = MutableLiveData<List<Drawable>>() // 내부처리 데이터

    val imageUriLiveData: LiveData<List<String>> get() = _imageUriLiveData // client 읽기 전용
    val imageDrawableLiveData: LiveData<List<Drawable>> get() = _imageDrawableLiveData // client 읽기 전용


    fun setContainer(MCContainer: MCContainer) {
        jpegMCContainer.value = MCContainer
    }

    fun updateImageUriData(uriList: MutableList<String>) {
        this.imageUriList = uriList
        _imageUriLiveData.value = imageUriList
        //drawableFromUrl()
    }

    @Throws(IOException::class)
    private fun drawableFromUrl(){ // gallery image uri String to Drawable

        imageDrawableList.clear()

        var x: Bitmap
        var connection: HttpURLConnection

        for (uri in _imageUriLiveData.value!!){ // 갤러리에 사진이 아예 없을 때 위험할 수도..(예외처리는 해놓기)
            connection = URL(uri).openConnection() as HttpURLConnection
            connection.connect()
            val input: InputStream = connection.getInputStream()
            x = BitmapFactory.decodeStream(input)
            imageDrawableList.add(BitmapDrawable(x))
        }
        _imageDrawableLiveData.value = imageDrawableList
    }

}