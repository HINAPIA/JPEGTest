package com.example.kotlinjpegtest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinjpegtest.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {
    var jpegConstant : JpegConstant = JpegConstant()
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap


    var sourcePhotoUri : Uri? = null
    var destPhotoUri : Uri? = null
    var photoBitmap: Bitmap? = null

    var sourceBitmap: Bitmap? = null
    var destBitmap: Bitmap? = null
    var sourceByteArray : ByteArray? = null
    var destByteArray : ByteArray? = null
    private lateinit var binding: ActivityMainBinding
    companion object {
        const val REQUEST_CODE = 1
        const val UPLOAD_FOLDER = "upload_images/"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        //source 이미지 클릭
        binding!!.imageView1.setOnClickListener {
            // 갤러리로 이동
            var photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            startActivityForResult(photoIntent, 0)
            // 권한 요청
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
        }

        //dest 이미지 클릭
        binding!!.imageView2.setOnClickListener {
            // 갤러리로 이동
            var photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            startActivityForResult(photoIntent, 1)
            // 권한 요청
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
        }

        //add 버튼 클릭
        binding!!.btnAdd.setOnClickListener{
            if(sourceByteArray != null && destByteArray != null){
                insertFrameToJpeg(sourceByteArray!!,destByteArray!!)
            }
        }
    }

    fun insertFrameToJpeg(sourceByteArray : ByteArray, destByteArray: ByteArray){

        var destFrameByteArray : ByteArray? = null
        var resultBitMap : Bitmap
        val outputStream = ByteArrayOutputStream()

        CoroutineScope(Dispatchers.IO).launch {
            // 2. source files의 main frame을 추출해
            destFrameByteArray = extractFrame(destByteArray)
            if(destFrameByteArray == null){
                System.out.println("frame을 찾을 수 없음")
                return@launch
            }
            outputStream.write(sourceByteArray)
            outputStream.write(destFrameByteArray)
            // 3. 저장
            val resultBytes = outputStream.toByteArray()
            resultBitMap = byteArrayToBitmap(resultBytes)

            // image View에 띄우기
            binding.resultImageView.setImageBitmap(resultBitMap)
        }
       // outputStream.write

    }

    // Byte를 Bitmap으로 변환
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    //JPEGFile에서 SOF가 나오는 부분부터 EOI까지 추출하여 byte []로 리턴하는 함수

    fun extractFrame(jpegBytes: ByteArray): ByteArray? {
        var n1: Int
        var n2: Int
        val resultByte: ByteArray
        var startIndex = 0
        var endIndex = jpegBytes.size
        var startCount = 0
        var endCount = 0
        //EOI 삽입
        //outputStream.write((byte)Integer.parseInt("ff", 16));
        //outputStream.write((byte)Integer.parseInt("d9", 16));
        var startMax = 2
        val endMax = 1
        var isFindStartMarker = false // 시작 마커를 찾았는지 여부
        var isFindEndMarker = false // 종료 마커를 찾았는지 여부


        for (i in 0 until jpegBytes.size - 1) {
            if (Integer.valueOf(jpegBytes[i].toInt()).toInt().also {
                    n1 = it
                } < 0) {
                n1 += 256
            }
            if (Integer.valueOf(jpegBytes[i + 1].toInt()).toInt().also {
                    n2 = it
                } < 0) {
                n2 += 256
            }
            val twoByteToNum = n1 + n2
            if (markerHashMap.containsKey(twoByteToNum) && n1 == 255) {
                println("마커 찾음 : ${i}")
                if (twoByteToNum == jpegConstant.SOF0_MARKER) {
                    println("SOF 마커 찾음 : ${i}")
                    startCount++
                    if (startCount == startMax) {
                        startIndex = i
                        isFindStartMarker = true
                    }
                }
                if (isFindStartMarker) { // 조건에 부합하는 start 마커를 찾은 후, end 마커 찾기
                    if (twoByteToNum == jpegConstant.EOI_MARKER) {
                        println("EOI 마커 찾음 : ${i}")
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
            println("Error: 찾는 마커가 존재하지 않음")
            return null
        }
        // 추출
        resultByte = ByteArray(endIndex - startIndex + 2)
        // start 마커부터 end 마커를 포함한 영역까지 복사해서 resultBytes에 저장
        System.arraycopy(jpegBytes, startIndex, resultByte, 0, endIndex - startIndex + 2)
        return resultByte
    }
    // image Uri to Bitmap
    @RequiresApi(Build.VERSION_CODES.P)
    fun uriToBitmap(photoUri : Uri): Bitmap? {
        var bitmap : Bitmap? = null
        try{
            bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver,
            photoUri))
        }catch (e : IOException){
            e.printStackTrace()
        }
        return bitmap
    }
    //갤러리에서 돌아올 때
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 1번째 image view 클릭 (source image)
        if(requestCode == 0){
            if(resultCode == Activity.RESULT_OK){
                sourcePhotoUri = data?.data
                // ImageView에 image set
                binding.imageView1.setImageURI(sourcePhotoUri)
                sourceBitmap = uriToBitmap(sourcePhotoUri!!)
                if(sourceBitmap!=null){
                    sourceByteArray = bitmapToByteArray(sourceBitmap!!)
                    Log.d("이미지", "sourceByteArray ${sourceByteArray}")
                }
            }else{
                finish()
            }
        // 2번째 image view 클릭 (dest image)
        }else if(requestCode ==1){
            if(resultCode == Activity.RESULT_OK){
                destPhotoUri = data?.data
                // ImageView에 image set
                binding.imageView2.setImageURI(destPhotoUri)
                destBitmap = uriToBitmap(destPhotoUri!!)
                if(destBitmap!=null){
                    destByteArray = bitmapToByteArray(destBitmap!!)
                    Log.d("이미지", "destByteArray ${destByteArray}")
                }
            }else{
                finish()
            }
        }
    }

    // Bitmap을 Byte로 변환
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray? {
        val stream = ByteArrayOutputStream()
        // compress : image를 압축하여 Byte 형태로 변환
            // 매개변수 (압축된 이미지의 형식, 퀄리티 , 압축된 데이터를 쓰는 출력 stream)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }


}