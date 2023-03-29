package com.example.camerax.PictureModule

import com.example.camerax.PictureModule.Contents.ContentAttribute
import com.example.camerax.PictureModule.Contents.ContentType

class GroupContent {

    var imageContent : ImageContent = ImageContent()
    var audioContent : AudioContent = AudioContent()
    var textContent: TextContent = TextContent()

    fun init(){
        imageContent.init()
        audioContent.init()
        textContent.init()
    }

    suspend fun setContent(byteArrayList: ArrayList<ByteArray>, type : ContentType, contentAttribute : ContentAttribute){
        when (type){
            ContentType.Image -> imageContent.setContent(byteArrayList, contentAttribute)
            ContentType.Audio -> audioContent.setContent(byteArrayList, contentAttribute)
            ContentType.Text -> textContent.setContent(byteArrayList, contentAttribute)
        }
    }



}