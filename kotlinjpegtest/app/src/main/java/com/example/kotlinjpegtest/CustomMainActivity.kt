package com.example.kotlinjpegtest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.kotlinjpegtest.databinding.ActivityCustomMainBinding
import com.example.kotlinjpegtest.databinding.ActivityMainBinding
import com.example.kotlinjpegtest.databinding.FragmentMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class CustomMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomMainBinding
    var jpegConstant : JpegConstant = JpegConstant()
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap

    var sourcePhotoUri : Uri? = null
    var destPhotoUri : Uri? = null
    var resultBitMap: Bitmap? = null
    var sourceByteArray : ByteArray? = null
    var destByteArray : ByteArray? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        binding!!.imageView1.setOnClickListener {
            Log.d("이미지", "1 클릭")
            // 갤러리로 이동
            var photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            startActivityForResult(photoIntent, 0)
            // 권한 요청
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MainActivity.REQUEST_CODE
            )
        }

        //dest 이미지 클릭
        binding!!.imageView2.setOnClickListener {
            Log.d("이미지", "2 클릭")
            // 갤러리로 이동
            var photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            startActivityForResult(photoIntent, 1)
            // 권한 요청
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MainActivity.REQUEST_CODE
            )

        }

        //add 버튼 클릭
        binding!!.btnAdd.setOnClickListener{
            if(sourceByteArray != null && destByteArray != null){
                customInsertFrameToJpeg(sourceByteArray!!,destByteArray!!)
            }
        }
    }
    fun customInsertFrameToJpeg(sourceByteArray : ByteArray, destByteArray: ByteArray){

        var destFrameByteArray : ByteArray? = null
        val outputStream = ByteArrayOutputStream()

        CoroutineScope(Dispatchers.Main).launch {
            // 1. source files의 main frame을 추출
            destFrameByteArray = extractFrame(destByteArray)
            if(destFrameByteArray == null){
                System.out.println("frame을 찾을 수 없음")
                return@launch
            }
            outputStream.write(sourceByteArray)
            outputStream.write(destFrameByteArray)
            // 2. 저장
            val resultBytes = outputStream.toByteArray()
            //

            var bundle = Bundle()
            bundle.putByteArray("image",resultBytes)
            var fragment = ResultFragment()
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment,fragment)
                .commit()
        }

    }
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
                //println("마커 찾음 : ${i}: ${twoByteToNum}")
                //println("n1 : ${n1}, n2 : ${n2}")
                if (twoByteToNum == jpegConstant.SOF0_MARKER) {
                    println("SOF 마커 찾음 : ${i} : ${twoByteToNum}")
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
    //갤러리에서 돌아올 때
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //super.onActivityResult(requestCode, resultCode, data)
        Log.d("결과", " 프래그먼트 onActivityResult 호출 ${requestCode}")
        // 1번째 image view 클릭 (source image)
        if(requestCode == 0){
            if(resultCode == Activity.RESULT_OK){
                sourcePhotoUri = data?.data
                // ImageView에 image set
                binding.imageView1.setImageURI(sourcePhotoUri)
                val iStream: InputStream? = contentResolver.openInputStream(sourcePhotoUri!!)
                sourceByteArray = getBytes(iStream!!)
                Log.d("이미지", "sourceByteArray ${sourceByteArray}")
            }else{
                finish()
            }
            // 2번째 image view 클릭 (dest image)
        }else if(requestCode ==1){
            if(resultCode == Activity.RESULT_OK){
                destPhotoUri = data?.data
                // ImageView에 image set
                binding.imageView2.setImageURI(destPhotoUri)
                val iStream: InputStream? = contentResolver.openInputStream(destPhotoUri!!)
                destByteArray = getBytes(iStream!!)
                Log.d("이미지", "destByteArray ${destByteArray}")
            }else{
                finish()
            }
        }
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
}