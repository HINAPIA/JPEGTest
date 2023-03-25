package com.example.camerax.PictureModule

import android.app.Activity
import android.os.Build
import android.provider.ContactsContract.CommonDataKinds.Im
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.camerax.PictureModule.Contents.Attribute
import com.example.camerax.PictureModule.Contents.Audio
import com.example.camerax.PictureModule.Contents.ContentType
import com.example.camerax.SaveModule.SaveResolver


class Container(_activity: Activity) {
    private var saveResolver : SaveResolver
    private lateinit var activity : Activity

    private var imageContent : ImageContent = ImageContent()
    private var audioContent : AudioContent = AudioContent()
    private var textContent: TextContent = TextContent()
    private var length : Int = 0
    // 수정을 하거나 새로운 사진 그룹이 추가되면 +1
    private var groupCount : Int = 0
    init {
        activity = _activity
        saveResolver = SaveResolver(activity ,this)
    }

    fun init(){
        imageContent.init()
        audioContent.init()
        textContent.init()
        length = 0
    }
    // PictureContainer의 내용을 리셋 후 초기화
    suspend fun reFresh(byteArrayList: ArrayList<ByteArray>, type: ContentType, attribute : Attribute){
        //header 초기화
        // picture List 초기화
        init()
        when (type){
            ContentType.Image -> imageContent.reFresh(byteArrayList, attribute)
            ContentType.Audio -> audioContent.reFresh(byteArrayList, attribute)
            ContentType.Text -> textContent.reFresh(byteArrayList, attribute)
        }
        groupCount = 1
        //headerRenew()
        //save()
    }




    fun headerRenew(){
      //  header.renew(groupID, pictureList)
    }
    //PictureContainer의 데이터를 파일로 저장
    fun save(){
        saveResolver.save()
    }


}

