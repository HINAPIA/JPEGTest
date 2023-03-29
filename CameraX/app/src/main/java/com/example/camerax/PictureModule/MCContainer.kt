package com.example.camerax.PictureModule

import android.app.Activity
import com.example.camerax.PictureModule.Contents.ContentAttribute
import com.example.camerax.PictureModule.Contents.ContentType
import com.example.camerax.SaveModule.SaveResolver


class MCContainer(_activity: Activity) {
    private var saveResolver : SaveResolver
    private lateinit var activity : Activity
    var header : Header

    var groupContentList : ArrayList<GroupContent> = arrayListOf()

    var length : Int = 0
    // 수정을 하거나 새로운 사진 그룹이 추가되면 +1
    private var groupCount : Int = 0
    init {
        activity = _activity
        saveResolver = SaveResolver(activity ,this)
        header = Header(this)
    }

    fun init(){
        groupContentList.clear()
        length = 0
    }
    // 사진을 찍은 후에 호출되는 함수로 MC Container를 초기화하고 찍은 사진 내용으로 MC Container를 채운다
    suspend fun reFresh(byteArrayList: ArrayList<ByteArray>, type: ContentType, contentAttribute : ContentAttribute){
        //header 초기화
        // picture List 초기화
        init()
        var groupContent = GroupContent()
        groupContent.setContent(byteArrayList, type, contentAttribute)
        groupContentList.add(groupContent)
        groupCount = 1
        //headerRenew()
        save()
    }



    fun settingHeaderInfo(){
        header.settingHeaderInfo()
    }
    fun convertHeaderToBinaryData() : ByteArray{
        return header.convertBinaryData()
    }
    //PictureContainer의 데이터를 파일로 저장
    fun save(){
        saveResolver.save()
    }


}

