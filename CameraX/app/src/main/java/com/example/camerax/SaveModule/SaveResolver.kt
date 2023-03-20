package com.example.camerax.SaveModule

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.camerax.MainActivity
import com.example.camerax.PictureModule.PictureContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*


class SaveResolver(_mainActivity: MainActivity, _pictureContainer: PictureContainer) {
    private var pictureContainer : PictureContainer
    private var mainActivity : MainActivity
    init{
        pictureContainer = _pictureContainer
        mainActivity = _mainActivity
    }

    fun save(){
        val byteBuffer = ByteArrayOutputStream()
        var firstPicture = pictureContainer.getPictureList().get(0)
        //SOI 쓰기
        byteBuffer.write(firstPicture.getByteArray(),0,2)
        //헤더 쓰기
        byteBuffer.write(pictureContainer.getHeader().convertBinaryData())
        //나머지 첫번째 사진의 데이터 쓰기
        byteBuffer.write(firstPicture.getByteArray(),2,firstPicture.getByteArray().size-3)
        // write
        for(i in 1.. pictureContainer.getCount()-1){
            var picture = pictureContainer.getPictureList().get(i)
            byteBuffer.write(/* b = */ picture.getByteArray())
        }
        var resultByteArray = byteBuffer.toByteArray()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
            saveImageOnAboveAndroidQ(resultByteArray)
            Log.d("Picture Module", "이미지 저장 함수 :saveImageOnAboveAndroidQ ")
        } else {
            // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
            val writePermission = mainActivity?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            if (writePermission == PackageManager.PERMISSION_GRANTED) {

                saveImageOnUnderAndroidQ(resultByteArray)

                // Toast.makeText(context, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                val requestExternalStorageCode = 1

                val permissionStorage = arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

                ActivityCompat.requestPermissions(
                    mainActivity as Activity,
                    permissionStorage,
                    requestExternalStorageCode
                )
            }
        }
        Log.d("이미지","insertFrameToJpeg 끝")
    }

//    //Android Q (Android 10, API 29 이상에서는 이 메서드를 통해서 이미지를 저장한다.)
//    @RequiresApi(Build.VERSION_CODES.Q)
//    fun saveImage() {
//        val fileName = System.currentTimeMillis().toString() + ".jpg" // 파일이름 현재시간.jpg
//
//        val values = ContentValues()
//        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ImageSave")
//        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
//        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//        val uri: Uri? =
//            mainActivity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
//        try {
//            val outputStream: OutputStream? = uri?.let {
//                mainActivity.getContentResolver().openOutputStream(
//                    it
//                )
//            }
//
//            if (outputStream != null) {
//                outputStream.close()
//            }
//            Toast.makeText(mainActivity, "Image saved successfully", Toast.LENGTH_SHORT).show()
//        } catch (e: IOException) {
//            e.printStackTrace()
//            Toast.makeText(mainActivity, "Failed to save image", Toast.LENGTH_SHORT).show()
//        } catch(e: FileNotFoundException) {
//            e.printStackTrace()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    //Android Q (Android 10, API 29 이상에서는 이 메서드를 통해서 이미지를 저장한다.)
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageOnAboveAndroidQ(byteArray: ByteArray) {
        Log.d("Picture Module", "이미지 저장 함수 :saveImageOnAboveAndroidQ 111")

        val fileName = System.currentTimeMillis().toString() + ".jpg" // 파일이름 현재시간.jpg
        /*
        * ContentValues() 객체 생성.
        * ContentValues는 ContentResolver가 처리할 수 있는 값을 저장해둘 목적으로 사용된다.
        * */

        val values = ContentValues()
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ImageSave")
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        // 이미지를 저장할 uri를 미리 설정해놓는다.
        val uri: Uri? =
            mainActivity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        Log.d("Picture Module", "이미지 저장 함수 :saveImageOnAboveAndroidQ 2222")

        try {
            val outputStream: OutputStream? = uri?.let {
                mainActivity.getContentResolver().openOutputStream(
                    it
                )
            }
            if (outputStream != null) {
                outputStream.write(byteArray)
                outputStream.close()
                Toast.makeText(mainActivity, "Image saved successfully", Toast.LENGTH_SHORT).show()

            }


        } catch(e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveImageOnUnderAndroidQ(byteArray: ByteArray) {
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val path = "$externalStorage/DCIM/imageSave"
        val dir = File(path)

        if(dir.exists().not()) {
            dir.mkdirs() // 폴더 없을경우 폴더 생성
        }

        try {
            val fileItem = File("$dir/$fileName")
            fileItem.createNewFile()
            //0KB 파일 생성.

            val fos = FileOutputStream(fileItem) // 파일 아웃풋 스트림
            fos.write(byteArray)
           // bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            //파일 아웃풋 스트림 객체를 통해서 Bitmap 압축.

            fos.close() // 파일 아웃풋 스트림 객체 close

            mainActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))
            // 브로드캐스트 수신자에게 파일 미디어 스캔 액션 요청. 그리고 데이터로 추가된 파일에 Uri를 넘겨준다.
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}