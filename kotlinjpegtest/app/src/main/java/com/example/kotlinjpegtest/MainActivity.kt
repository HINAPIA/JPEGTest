package com.example.kotlinjpegtest

import android.Manifest
import android.app.ActionBar
import android.app.ActionBar.*
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.kotlinjpegtest.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*


class MainActivity : AppCompatActivity() {
    var jpegConstant : JpegConstant = JpegConstant()
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap


    var sourcePhotoUri : Uri? = null
    var destPhotoUri : Uri? = null
    var photoBitmap: Bitmap? = null
    var resultBitMap: Bitmap? = null
    var sourceByteArray : ByteArray? = null
    var destByteArray : ByteArray? = null
    private lateinit var binding: ActivityMainBinding
    companion object {
        const val REQUEST_CODE = 1
        const val UPLOAD_FOLDER = "upload_images/"
    }

//    val handler: Handler = object : Handler() {
//        fun handleMessage(msg: Message?) {
//            // image View에 띄우기
//            binding.resultImageView.setImageBitmap(resultBitMap)
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        binding.btnSave.setVisibility(View.GONE);
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

        //save 버튼 클릭
        binding!!.btnSave.setOnClickListener{
            val bitmap = drawBitmap()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
                if (bitmap != null) {
                    saveImageOnAboveAndroidQ(bitmap)
                }
                Toast.makeText(baseContext, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
                val writePermission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

                if(writePermission == PackageManager.PERMISSION_GRANTED) {
                    if (bitmap != null) {
                        saveImageOnUnderAndroidQ(bitmap)
                    }
                    Toast.makeText(baseContext, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val requestExternalStorageCode = 1

                    val permissionStorage = arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    ActivityCompat.requestPermissions(this, permissionStorage, requestExternalStorageCode)
                }
            }
        }
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
    fun insertFrameToJpeg(sourceByteArray : ByteArray, destByteArray: ByteArray){

        var destFrameByteArray : ByteArray? = null
        val outputStream = ByteArrayOutputStream()

        CoroutineScope(Dispatchers.Main).launch {
            // 1. source files의 main frame을 추출해
            destFrameByteArray = extractFrame(destByteArray)
            if(destFrameByteArray == null){
                System.out.println("frame을 찾을 수 없음")
                return@launch
            }
            outputStream.write(sourceByteArray)
            outputStream.write(destFrameByteArray)
            // 2. 저장
            val resultBytes = outputStream.toByteArray()
            resultBitMap = byteArrayToBitmap(resultBytes)

            // 기존의 view 안보이게 함
            binding.imageView1.setVisibility(View.GONE);
            binding.imageView2.setVisibility(View.GONE);
            binding.btnAdd.setVisibility(View.GONE);

            binding.resultImageView.setImageBitmap(resultBitMap)
            binding.btnSave.setVisibility(View.VISIBLE);
        }

    }

    // 화면에 나타난 View를 Bitmap에 그릴 용도.
    private fun drawBitmap(): Bitmap {
        val backgroundWidth = resultBitMap?.width!!.toInt()
        val backgroundHeight = resultBitMap?.height!!.toInt()

        val totalBitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.ARGB_8888) // 비트맵 생성
        val canvas = Canvas(totalBitmap) // 캔버스에 비트맵을 Mapping.

        val imageViewLeft = 0
        val imageViewTop = 0

        canvas.drawBitmap(resultBitMap!!, imageViewLeft.toFloat(),imageViewTop.toFloat(), null)

        return totalBitmap
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
    // Bitmap을 Byte로 변환
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray? {
        val stream = ByteArrayOutputStream()
        // compress : image를 압축하여 Byte 형태로 변환
            // 매개변수 (압축된 이미지의 형식, 퀄리티 , 압축된 데이터를 쓰는 출력 stream)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }
    //Android Q (Android 10, API 29 이상에서는 이 메서드를 통해서 이미지를 저장한다.)
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageOnAboveAndroidQ(bitmap: Bitmap) {
        val fileName = System.currentTimeMillis().toString() + ".jpg" // 파일이름 현재시간.jpg
        /*
        * ContentValues() 객체 생성.
        * ContentValues는 ContentResolver가 처리할 수 있는 값을 저장해둘 목적으로 사용된다.
        * */
        val contentValues = ContentValues()
        contentValues.apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ImageSave") // 경로 설정
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName) // 파일이름을 put해준다.
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            put(MediaStore.Images.Media.IS_PENDING, 1) // 현재 is_pending 상태임을 만들어준다.
            // 다른 곳에서 이 데이터를 요구하면 무시하라는 의미로, 해당 저장소를 독점할 수 있다.
        }

        // 이미지를 저장할 uri를 미리 설정해놓는다.
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if(uri != null) {
                val image = contentResolver.openFileDescriptor(uri, "w", null)
                // write 모드로 file을 open한다.

                if(image != null) {
                    val fos = FileOutputStream(image.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    //비트맵을 FileOutputStream를 통해 compress한다.
                    fos.close()

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // 저장소 독점을 해제한다.
                    contentResolver.update(uri, contentValues, null, null)
                }
            }
        } catch(e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun saveImageOnUnderAndroidQ(bitmap: Bitmap) {
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

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            //파일 아웃풋 스트림 객체를 통해서 Bitmap 압축.

            fos.close() // 파일 아웃풋 스트림 객체 close

            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))
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