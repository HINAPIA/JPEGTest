package com.example.camerax

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.util.ArrayList
import java.util.HashMap

class JpegCreator {
    var jpegConstant : JpegConstant = JpegConstant()
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap

    fun insertFrameToJpeg(context : Context, imageArrayList : ArrayList<ByteArray>){
        var sourceByteArray : ByteArray? = null
        var destFrameByteArray : ByteArray? = null
        val outputStream = ByteArrayOutputStream()

       // var context: Context = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("이미지","insertFrameToJpeg 시작")
            // 1.첫번째 이미지 먼저 버퍼에 write
            sourceByteArray = imageArrayList.get(0)
            outputStream.write(sourceByteArray)

            if(imageArrayList.size != 1){
                Log.d("이미지","소스 파일 write 시작")
                // 2. 첫번째 이미지 file을 뺀 나머지 이미지 file들의 frame 부분을 추출 하여
                // 버퍼에 write
                for(i:Int in 1..imageArrayList.size-1){
                    var imageByte = imageArrayList.get(i)
                    if(imageByte == null){
                        Log.e("user error", "destByte frame not found" )
                    }
                    extractFrame(imageByte)?.let {
                        Log.d("이미지","${i}번째 파일 프레임 추출 성공")
                        //destImageByteList.add(it)
                        outputStream.write(it)
                        //  var resultBitMap = byteArrayToBitmap(resultByteArray!!)

                    }
                }
            }

            //3. 하나의 파일로 저장
            Log.d("이미지","저장 시작")
            val resultBytes = outputStream.toByteArray()
            val resultBitMap = byteArrayToBitmap(resultBytes!!)
            var bitmap = drawBitmap(resultBitMap)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
                if (bitmap != null) {
                   // saveImageOnAboveAndroidQ(bitmap)
                    Log.d("이미지","insertFrameToJpeg 저장")
                }
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
                val writePermission = context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
                if (writePermission == PackageManager.PERMISSION_GRANTED) {
                    if (bitmap != null) {
                        //saveImageOnUnderAndroidQ(bitmap)
                    }
                    // Toast.makeText(context, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val requestExternalStorageCode = 1

                    val permissionStorage = arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    ActivityCompat.requestPermissions(
                        context as Activity,
                        permissionStorage,
                        requestExternalStorageCode
                    )
                }
            }
            Log.d("이미지","insertFrameToJpeg 끝")
        }
//            var bundle = Bundle()
//            bundle.putByteArray("image",resultBytes)
//            var fragment = ResultFragment()
//            fragment.arguments = bundle
//            mainActivity.supportFragmentManager.beginTransaction()
//                .replace(R.id.fragment,fragment)
//                .commit()
        // }

    }

    // 화면에 나타난 View를 Bitmap에 그릴 용도.
    private fun drawBitmap(bitmap:Bitmap): Bitmap {
        val backgroundWidth = bitmap?.width!!.toInt()
        val backgroundHeight = bitmap?.height!!.toInt()

        val totalBitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.ARGB_8888) // 비트맵 생성
        val canvas = Canvas(totalBitmap) // 캔버스에 비트맵을 Mapping.

        val imageViewLeft = 0
        val imageViewTop = 0

        canvas.drawBitmap(bitmap!!, imageViewLeft.toFloat(),imageViewTop.toFloat(), null)

        return totalBitmap
    }
    // 한 파일에서 SOF~EOI 부분의 바이너리 데이터를 찾아 ByteArray에 담아 리턴
    suspend fun extractFrame(jpegBytes: ByteArray): ByteArray? {
        Log.d("이미지","extractFrame 시작")
        var n1: Int
        var n2: Int
        val resultByte: ByteArray
        var startIndex = 0
        var endIndex = jpegBytes.size
        var startCount = 0
        var endCount = 0
        var startMax = 1
        val endMax = 1
        var isFindStartMarker = false // 시작 마커를 찾았는지 여부
        var isFindEndMarker = false // 종료 마커를 찾았는지 여부


        for (i in 0 until jpegBytes.size - 1) {
            n1 = Integer.valueOf(jpegBytes[i].toInt())
            if (n1 < 0) {
                n1 += 256
            }
            n2 = Integer.valueOf(jpegBytes[i+1].toInt())
            if (n2 < 0) {
                n2 += 256
            }

            val twoByteToNum = n1 + n2
            if (markerHashMap.containsKey(twoByteToNum) && n1 == 255) {
                if (twoByteToNum == jpegConstant.SOF0_MARKER) {
                    Log.d("이미지","SOF 마커 찾음 : ${i}")
                    println("SOF 마커 찾음 : ${i}")
                    startCount++
                    if (startCount == startMax) {
                        startIndex = i
                        isFindStartMarker = true
                    }
                }
                if (isFindStartMarker) { // 조건에 부합하는 start 마커를 찾은 후, end 마커 찾기
                    if (twoByteToNum == jpegConstant.EOI_MARKER) {
                        Log.d("이미지","EOI 마커 찾음 : ${i}")
                        endCount++
                        if (endCount == endMax) {
                            endIndex = i
                            isFindEndMarker = true
                        }
                    }
                }
            }
        }
        if (!isFindStartMarker || !isFindEndMarker) {
            println("startIndex :${startIndex}")
            println("endIndex :${endIndex}")
            Log.d("이미지","Error: 찾는 마커가 존재하지 않음")
            println("Error: 찾는 마커가 존재하지 않음")
            return null
        }
        // 추출
        resultByte = ByteArray(endIndex - startIndex + 2)
        // start 마커부터 end 마커를 포함한 영역까지 복사해서 resultBytes에 저장
        System.arraycopy(jpegBytes, startIndex, resultByte, 0, endIndex - startIndex + 2)
        return resultByte


    }
    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }

    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}