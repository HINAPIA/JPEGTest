package com.example.camerax.PictureModule

import com.example.camerax.PictureModule.Contents.Attribute
import com.example.camerax.PictureModule.Contents.Text

class TextContent {
    var textList : ArrayList<Text> = arrayListOf()
    var length = 0
    var textCount = 0

    fun init(){
        textList.clear()
        length = 0
        textCount = 0
    }

    suspend fun reFresh(byteArrayList: ArrayList<ByteArray>, attribute : Attribute){
        for(i in 0..byteArrayList.size-1){
            var text = Text(byteArrayList.get(i), attribute)
            insertText(text)
        }
    }
    fun insertText(text : Text){
        textList.add(text)
        textCount = textCount + 1
    }
}