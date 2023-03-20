package com.example.camerax

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.camerax.LoadModule.LoadResolver
import com.example.camerax.PictureModule.PictureContainer
import com.example.camerax.databinding.ActivityLoadBinding
import com.example.camerax.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class LoadActivity : AppCompatActivity() {
    private lateinit var binding : ActivityLoadBinding
    private var loadResolver : LoadResolver = LoadResolver(this)
    private var pictureContainer : PictureContainer = PictureContainer(this)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityLoadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //갤러리로 이동
        binding.imageView.setOnClickListener {
            // 갤러리로 이동
            var photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            startActivityForResult(photoIntent, 0)
            // 권한 요청
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }
        binding.btnSave.setOnClickListener{
            pictureContainer.save()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //super.onActivityResult(requestCode, resultCode, data)
        Log.d("결과", " 프래그먼트 onActivityResult 호출 ${requestCode}")
        // 1번째 image view 클릭 (source image)
        if(requestCode == 0){
            if(resultCode == Activity.RESULT_OK){
                var sourcePhotoUri = data?.data
                // ImageView에 image set
                binding.imageView.setImageURI(sourcePhotoUri)
                val iStream: InputStream? = contentResolver.openInputStream(sourcePhotoUri!!)
                var sourceByteArray = getBytes(iStream!!)
                // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
                loadResolver.createPictureContainer(this,pictureContainer,sourceByteArray)

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