package com.example.camerax.LoadModule

import android.app.Activity
import android.media.Image
import com.example.camerax.PictureModule.*
import com.example.camerax.PictureModule.Contents.ContentAttribute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LoadResolver(_activity: Activity) {

    fun ByteArraytoInt(byteArray: ByteArray, stratOffset : Int): Int {
        var intNum :Int = ((byteArray[stratOffset].toInt() and 0xFF) shl 24) or
                ((byteArray[stratOffset+1].toInt() and 0xFF) shl 16) or
                ((byteArray[stratOffset+2].toInt() and 0xFF) shl 8) or
                ((byteArray[stratOffset+3].toInt() and 0xFF))
        return intNum
    }


    fun imageContentParsing(sourceByteArray: ByteArray, imageInfoByteArray: ByteArray) : ArrayList<Picture> {
        var pictureList : ArrayList<Picture> = arrayListOf()
        var startIndex = 0
        var imageDataStartOffset = ByteArraytoInt(imageInfoByteArray, startIndex)
        startIndex++
        var imageCount = ByteArraytoInt(imageInfoByteArray, startIndex*4)
        startIndex++
        for(i in 0..imageCount -1){
            var offset = ByteArraytoInt(imageInfoByteArray, (startIndex*4))
            startIndex++
            var size = ByteArraytoInt(imageInfoByteArray, startIndex*4)
            startIndex++
            var attribute = ByteArraytoInt(imageInfoByteArray, startIndex*4)
            startIndex++
            var embeddedDataSize = ByteArraytoInt(imageInfoByteArray, startIndex*4)
            startIndex++
            var embeddedData : ArrayList<Int> = arrayListOf()
            if (embeddedDataSize > 0){
                var curInt : Int = 0
                for(j in 0..embeddedDataSize/4 -1){
                    // 4개씩 쪼개서 Int 생성
                    curInt = ByteArraytoInt(imageInfoByteArray, startIndex*4)
                    embeddedData.add(curInt)
                    startIndex++
                }
            }
            // picture 생성
            var picture = Picture(offset, sourceByteArray.copyOfRange(imageDataStartOffset + offset,
                imageDataStartOffset + offset + size -1), ContentAttribute.fromCode(attribute), embeddedDataSize,embeddedData)
            pictureList.add(picture)
        }
        return pictureList
    }

    fun createMCContainer(activity: Activity, MCContainer: MCContainer, sourceByteArray: ByteArray) {
        var APP3_startOffset = 4
        var groupContentList : ArrayList<GroupContent> = arrayListOf()
        // var header : Header = Header()
        var dataFieldLength = ByteArraytoInt(sourceByteArray, APP3_startOffset)
        var groupCount = ByteArraytoInt(sourceByteArray, APP3_startOffset + 4)

        //groups parsing
        for(i in 0..groupCount-1){
            var groupContnt = GroupContent()
            var groupStartOffset = ByteArraytoInt(sourceByteArray, APP3_startOffset + 8)
            var GroupInfoSize = ByteArraytoInt(sourceByteArray, APP3_startOffset + 12)
            // 1. ImageContent
            var imageContentInfoSize = ByteArraytoInt(sourceByteArray, APP3_startOffset + 16)
            var pictureList = imageContentParsing(sourceByteArray, sourceByteArray.copyOfRange(APP3_startOffset + 20, APP3_startOffset + 20 + imageContentInfoSize))
            groupContnt.imageContent.setContent(pictureList)
            // 2. TextContent
            // 3. AudioContent
            groupContentList.add(groupContnt)
        }
        MCContainer.setContainer(groupContentList)

    }

    private var activity : Activity
    init{
        activity = _activity
    }

}