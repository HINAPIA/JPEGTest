package com.example.camerax.ViewerModule

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore

import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerax.LoadModule.LoadResolver
import com.example.camerax.PictureModule.MCContainer
import com.example.camerax.R
import com.example.camerax.databinding.ActivityLoadBinding
import com.example.camerax.databinding.ActivityViewerEditorBinding
import com.example.camerax.jpegViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class ViewerEditorActivity : AppCompatActivity() {

    private lateinit var binding : ActivityViewerEditorBinding
    private var loadResolver : LoadResolver = LoadResolver()
    private val viewerFragment = ViewerFragment()
    private val jpegViewModels: jpegViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 외부저장소 읽기 권한 부여 확인
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            // 권한 요청
            ActivityCompat.requestPermissions(this@ViewerEditorActivity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
        } else {
            // 권한이 이미 허용됨
            Log.d("[ViewerEditorActivity] 외부저장소 읽기 권한: ","confirm")
            getAllPhotosURI() // 갤러리 이미지 uri 가져오기
        }

        jpegViewModels.imageUriLiveData.observe(this){
           var size = jpegViewModels.imageUriLiveData.value?.size
            Log.d("[ViewerEditorActivity] imageUriLIst size : ",""+size)
            if (size != 0){
                supportFragmentManager  // fragment 전환
                    .beginTransaction()
                    .replace(R.id.framelayout,viewerFragment)
                    .addToBackStack(null)
                    .commit()
            }
            else {
                // TODO: 갤러리에 사진이 아무것도 없을 때 -> Empty Fragment 만들기
                Log.d("[ViewerEditorActivity]","갤러리에 사진이 아무것도 없을 때 처리해야함")
            }
        }
    }

    private fun getAllPhotosURI(){ // 이미지는 glider 써서 불러와야함
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")
        val uriList = mutableListOf<String>()
        if(cursor!=null){
            while(cursor.moveToNext()){
                // 사진 경로 Uri 가져오기
                val uri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                uriList.add(uri)
            }
            cursor.close()
            jpegViewModels.updateImageUriData(uriList)
        }
    }
}