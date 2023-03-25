package com.example.camerax.PictureModule

import android.util.Log
import java.io.Serializable
import java.nio.ByteBuffer

class Header() {
    val INFO_SIZE : Int = 16
    private lateinit var pictureList : ArrayList<Picture>
    private var count = 0
    private var offset = 0


    // 파일을 parsing하여 새로운 Header를 만들 때 호출하는 생성자
//    constructor(_pictureInfoList:ArrayList<PictureInfo>) : this(){
//        pictureInfoList = _pictureInfoList
//        count = pictureInfoList!!.size
//    }
    /* picture container가 갱신되어 새로운 헤더 정보가 필요할 때 호출 하여
    Header를 갱신하는 함수 */
    fun renew(groupID : Int,_pictureList: ArrayList<Picture>){
        // group ID와 offset 정보를 얻어서 heaㅇd
//        pictureInfoList?.clear()
//        offset = 0
//        // pictureList의 정보를 읽어서 header 작성
//        this.count = _pictureList.size
//
//        for(index in 0..count-1){
//            if(index > 0){
//                offset = offset +  _pictureList.get(index-1).getByteArray().size
//            }
//
//            var pictureInfo = PictureInfo(groupID, _pictureList.get(index))
//            pictureInfo.setOffset(offset)
//            pictureInfoList?.add(pictureInfo)
//        }
//        Log.d("Picture Module", pictureInfoList?.size.toString())

    }
    // 헤더의 내용을 바이너리 데이터로 변환하는 함수
//    fun convertBinaryData(): ByteArray{
//         var bufferSize  = pictureInfoList?.size!! * INFO_SIZE +6
//        //App3 마커
//        val buffer: ByteBuffer = ByteBuffer.allocate(bufferSize)
//        buffer.put("ff".toInt(16).toByte())
//        buffer.put("e3".toInt(16).toByte())
//        // 리스트 개수
//        buffer.putInt(pictureInfoList?.size!!)
//        //infoList
//        for(i in 0..(pictureInfoList?.size?.minus(1) ?:1 )){
//            var pictureInfo = pictureInfoList?.get(i)
//            buffer.putInt(pictureInfo!!.groupID!!)
//            buffer.putInt(pictureInfo!!.typeCode!!)
//            //작성한 APP3의 크기만큼 데이터 변경. 이 작업을 여기서 해도 괜찮은지 의논 필요
//            if(i == 0){
//                buffer.putInt(pictureInfo!!.offset!!)
//                buffer.putInt(pictureInfo!!.size!! + (bufferSize))
//
//            }else{
//                buffer.putInt(pictureInfo!!.offset!!+bufferSize-1)
//                buffer.putInt(pictureInfo!!.size!!)
//            }
//        }
//        val byteArray = buffer.array()
//        return byteArray
//    }
}