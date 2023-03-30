package com.example.camerax.PictureModule

import android.app.Activity
import android.util.Log
import com.example.camerax.PictureModule.Contents.ContentAttribute
import com.example.camerax.PictureModule.Contents.ContentType
import com.example.camerax.SaveModule.SaveResolver


class MCContainer(_activity: Activity) {
    private var saveResolver : SaveResolver
    private lateinit var activity : Activity
    var header : Header

    var groupContentList : ArrayList<GroupContent> = arrayListOf()

    // 수정을 하거나 새로운 사진 그룹이 추가되면 +1
    private var groupCount : Int = 0
    init {
        activity = _activity
        saveResolver = SaveResolver(activity ,this)
        header = Header(this)
    }

    fun init(){
        groupContentList.clear()
        groupCount = 0
    }

    fun setContainer(_groupContentList : ArrayList<GroupContent>){
        init()
        groupContentList = _groupContentList
        groupCount = _groupContentList.size

    }


    /*Edit modules에서 호출하는 함수*/
    //해당 그룹에 존재하는 picture 모두를 list로 제공
    fun getPictureList(groupID:Int) : ArrayList<Picture>{
        return groupContentList.get(groupID).imageContent.pictureList
    }
    // 해당 그룹에 존재하는 picture 중 해당 attribute 속성인 것들만 list로 제공
    fun getPictureList(groupID: Int, attribute: ContentAttribute) : ArrayList<Picture>{
        var pictureList = groupContentList.get(groupID).imageContent.pictureList
        var resultPictureList :ArrayList<Picture> = arrayListOf()
        for(i in 0..pictureList.size -1){
            var picture = pictureList.get(i)
            if(picture.contentAttribute == attribute)
                resultPictureList.add(picture)
        }
        return resultPictureList
    }
    //해당 그룹에 존재하는 modifiedPicture 제공
    fun getModifiedPicture(groupID: Int): Picture{
        return groupContentList.get(groupID).imageContent.modifiedPicture
    }
    // 해당 그룹의 Modified Picture 변경
//    fun setModifiedPicture(groupID: Int, modifyPicture: ByteArray, attribute: ContentAttribute): Picture {
//        var picture = Picture(modifyPicture, attribute)
//        groupContentList.get(groupID).imageContent.modifiedPicture = picture
//    }

    /*Edit modules에서 호출하는 함수 끝 */

    // 사진을 찍은 후에 호출되는 함수로 MC Container를 초기화하고 찍은 사진 내용으로 MC Container를 채운다
    fun reFresh(byteArrayList: ArrayList<ByteArray>, type: ContentType, contentAttribute : ContentAttribute){
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
    //Container의 데이터를 파일로 저장
    fun save(){
        saveResolver.save()
    }


}

