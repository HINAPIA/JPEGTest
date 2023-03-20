package com.example.camerax.PictureModule

import android.app.Activity
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.camerax.MainActivity
import com.example.camerax.SaveModule.SaveResolver
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import java.io.Serializable


class PictureContainer(_activity: Activity) {
    private var saveResolver : SaveResolver
    private lateinit var activity : Activity
    // 저장하기 전에 만듦
    private var header : Header
    private var pictureList : ArrayList<Picture> = arrayListOf()
    private var count : Int = 0
    // 수정을 하거나 새로운 사진 그룹이 추가되면 +1
    private var groupID : Int = 0
    init {
        activity = _activity
        header = Header()
        saveResolver = SaveResolver(activity ,this)
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
        headerRenew()
        save()
    }
    fun setHeader(_header : Header){
        header = _header
    }
    fun setPictureList(_pictureList : ArrayList<Picture>){
        pictureList = _pictureList
    }
    fun setCount(_count : Int){
        count = _count
    }
    fun headerRenew(){
        header.renew(groupID, pictureList)
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

