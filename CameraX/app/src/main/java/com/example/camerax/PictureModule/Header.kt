package com.example.camerax.PictureModule

import java.nio.ByteBuffer

class Header(_MC_container : MCContainer) {
    val INFO_SIZE : Int = 16
    private lateinit var pictureList : ArrayList<Picture>
    private var count = 0
    private var offset = 0
    private var MCContainer : MCContainer

    init {
        MCContainer =_MC_container
    }
    // 파일을 parsing하여 새로운 Header를 만들 때 호출하는 생성자
//    constructor(_pictureInfoList:ArrayList<PictureInfo>) : this(){
//        pictureInfoList = _pictureInfoList
//        count = pictureInfoList!!.size
//    }
    /* picture container가 갱신되어 새로운 헤더 정보가 필요할 때 호출 하여
    Header를 갱신하는 함수 */
    fun getHeaderInfo() : ByteArray{

        // image Cotentn
        var imageInfoByteArray :ByteArray= MCContainer.imageContent.getHeaderInfo()
        //text Content

        //audio Content
        val buffer: ByteBuffer = ByteBuffer.allocate(imageInfoByteArray.size +2)
        buffer.put("ff".toInt(16).toByte())
        buffer.put("e3".toInt(16).toByte())
        buffer.put(imageInfoByteArray)
        return buffer.array()

    }
     //헤더의 내용을 바이너리 데이터로 변환하는 함수
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