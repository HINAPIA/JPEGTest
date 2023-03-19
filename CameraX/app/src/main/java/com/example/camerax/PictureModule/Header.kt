package com.example.camerax.PictureModule

import android.util.Log
import java.nio.ByteBuffer

class Header(_pictureList: ArrayList<Picture>) {
    private val INFO_SIZE : Int = 16
    private var pictureList : ArrayList<Picture>
    private var pictureInfoList : ArrayList<PictureInfo>? = arrayListOf()
    private var count = 0
    private var offset = 0
    init{
        pictureList = _pictureList
    }

    fun create(groupID : Int){
        // group ID와 offset 정보를 얻어서 heaㅇd
        pictureInfoList?.clear()
        offset = 0
        // pictureList의 정보를 읽어서 header 작성
        this.count = pictureList.size

        for(index in 0..count-1){
            offset = offset +  pictureList.get(index).getByteArray().size
            var pictureInfo = PictureInfo(groupID, pictureList.get(index))
            pictureInfo.setOffset(offset)
            pictureInfoList?.add(pictureInfo)
        }
        Log.d("Picture Module", pictureInfoList?.size.toString())

    }
    // 헤더의 내용을 바이너리 데이터로 변환하는 함수
    fun convertBinaryData(): ByteArray{
         var bufferSize  = pictureInfoList?.size!! * INFO_SIZE +6
        //App3 마커
        val buffer: ByteBuffer = ByteBuffer.allocate(bufferSize)
        buffer.put("ff".toInt(16).toByte())
        buffer.put("e3".toInt(16).toByte())
        // 리스트 개수
        buffer.putInt(pictureInfoList?.size!!)
        //infoList
        for(i in 0..(pictureInfoList?.size?.minus(1) ?:1 )){
            var pictureInfo = pictureInfoList?.get(i)
            buffer.putInt(pictureInfo!!.groupID!!)
            buffer.putInt(pictureInfo!!.typeCode!!)
            buffer.putInt(pictureInfo!!.offset!!)
            buffer.putInt(pictureInfo!!.size!!)
        }
        val byteArray = buffer.array()
        return byteArray
    }
}