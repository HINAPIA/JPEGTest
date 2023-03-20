package com.example.camerax.PictureModule

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.camerax.MainActivity
import com.example.camerax.SaveModule.SaveResolver

class PictureContainer(_mainActivity: MainActivity) {
    private var saveResolver : SaveResolver
    private var mainActivity : MainActivity
    // 저장하기 전에 만듦
    private var header : Header
    private var pictureList : ArrayList<Picture> = arrayListOf()
    private var count : Int = 0
    // 수정을 하거나 새로운 사진 그룹이 추가되면 +1
    private var groupID : Int = 0
    init {
        mainActivity = _mainActivity
        header = Header(pictureList)
        saveResolver = SaveResolver(mainActivity,this)
    }
    // PictureContainer의 내용을 리셋 후 초기화
    @RequiresApi(Build.VERSION_CODES.Q)
    fun refresh(byteArrayList: ArrayList<ByteArray>, type: ImageType){
        //header 초기화
        // picture List 초기화
        pictureList.clear()
        for(i in 0..byteArrayList.size-1){
            // Picture 객체 생성
            var picture = Picture(byteArrayList.get(i), type)
            pictureList.add(picture)
            //header 수정
        }
        count = byteArrayList.size
        Log.d("Picture Module",
            "[Picture Container] size :${pictureList.size}, count: ${count}")
        headerCreate()
        save()
    }

    fun headerCreate(){
        header.create(groupID)
    }
    //PictureContainer의 데이터를 파일로 저장
    fun save(){
        saveResolver.save()
    }

    fun getCount(): Int{
        return count
    }
    fun getHeader(): Header{
        return header
    }
    fun getPictureList():  ArrayList<Picture>{
        return pictureList
    }
}