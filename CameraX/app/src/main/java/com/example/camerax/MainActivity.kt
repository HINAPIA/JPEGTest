package com.example.camerax

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.FocusMeteringAction.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerax.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    var jpegConstant : JpegConstant = JpegConstant()
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap

    private lateinit var viewBinding : ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var isFocusSuccess : Boolean? = null
    private lateinit var factory: MeteringPointFactory
    private val REQUEST_IMAGE_CAPTURE: Int = 1
    private lateinit var currentPhotoPath: String
    private var isPointArrayFull: Boolean = false
    private var isImageArrayFull: Boolean = false

    // CameraController
    private lateinit var camera : Camera
    private var cameraController : CameraControl ?= null
    private lateinit var cameraInfo: CameraInfo

    private var pointArrayList: ArrayList<pointData> = arrayListOf<pointData>()
    private var imageArrayList : ArrayList<ByteArray> = arrayListOf<ByteArray>()
    private var destImageByteList : ArrayList<ByteArray> = arrayListOf<ByteArray>()
    data class pointData (var x : Float, var y : Float)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // 카메라 권한 요청
        if(allPermissionsGranted()){
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        val displayHeight = resources.displayMetrics.heightPixels
        val displayWidth = resources.displayMetrics.widthPixels
        Log.v("Size Info", "hxw : ${displayHeight}x${displayWidth}")


        val params: ConstraintLayout.LayoutParams = viewBinding.viewFinder.layoutParams as ConstraintLayout.LayoutParams
        params.width = 1080
        params.height = 1440
        viewBinding.viewFinder.layoutParams = params

        viewBinding.imageCaptureButton.setOnClickListener {
            takePhoto()
        }

        // 버튼에 이벤트 리스너 추가
        viewBinding.multiFocusButton.setOnClickListener{
            factory = viewBinding.viewFinder.meteringPointFactory

            // 임의의 좌표값 3개를 arrayList에 저장
            pointArrayList.clear()
            pointArrayList.add(pointData(289f, 800f))
            pointArrayList.add(pointData(640f, 800f))
            pointArrayList.add(pointData(760f, 800f))

            //arraylist 에 있는 좌표들에 Focus 줘서 사진찍기
            takeFocusPhoto(0)

        }

        viewBinding.objectDetectionButton.setOnClickListener {
            factory = viewBinding.viewFinder.meteringPointFactory

            // 임의의 좌표값 3개를 arrayList에 저장
            pointArrayList.clear()
//            pointArrayList.add(pointData(289f, 800f))
//            pointArrayList.add(pointData(640f, 800f))
//            pointArrayList.add(pointData(760f, 800f))

            dispatchTakePictureIntent()

            //arraylist 에 있는 좌표들에 Focus 줘서 사진찍기
            //takeFocusPhoto(0)
        }

        viewBinding.viewFinder.setOnTouchListener { v : View, event : MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performClick()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {

                    Log.v("Size Info", "viewBinding.viewFinder.width : ${viewBinding.viewFinder.width}")
                    Log.v("Size Info", "viewBinding.viewFinder.height : ${viewBinding.viewFinder.height}")

                    // Get the MeteringPointFactory from PreviewView
                    val factory = viewBinding.viewFinder.meteringPointFactory

                    // Create a MeteringPoint from the tap coordinates
                    val point = factory.createPoint(event.x, event.y)
                    Log.v("Touch Point", "${event.x}, ${event.y}")
                    Toast.makeText(this,
                        "${event.x}, ${event.y}",
                        Toast.LENGTH_SHORT).show()
                    // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode
                    val action = FocusMeteringAction.Builder(point)
                        .build()

                    // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                    // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                    var result = cameraController?.startFocusAndMetering(action)!!

                    v.performClick()
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * ML Kit Object Detection function. We'll add ML Kit code here in the codelab.
     */
    private fun runObjectDetection(bitmap: Bitmap) {


        // Step 1. create ML Kit's InputImage object
        val image = InputImage.fromBitmap(bitmap, 0)

        // Step 2. acquire detector object
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE) // 검사 프로그램 모드( 단일이미지 또는 스트림)
            .enableMultipleObjects() // 감지 모드 (단일 또는 여러 객체 감지)
            .build()
        val objectDetector = ObjectDetection.getClient(options)

        // Setp 3. feed given image to detector and setup callback
        objectDetector.process(image)
            .addOnSuccessListener {
                // Task completed successfully
                debugPrint(it)

                pointArrayList.clear()
                imageArrayList.clear()
                Log.v("Test num", "pointArray size : ${pointArrayList.size}")
                // Parse ML Kit's DetectedObject and create corresponding visualization data
                val detectedObjects = it.map { obj ->
                    Log.v("PointCheck", "obj.boundingBox.left : ${obj.boundingBox.left}")
                    Log.v("PointCheck", "obj.boundingBox.top : ${obj.boundingBox.top}")
                    Log.v("PointCheck", "obj.boundingBox.right : ${obj.boundingBox.right}")
                    Log.v("PointCheck", "obj.boundingBox.bottom : ${obj.boundingBox.bottom}")

                    var text = "Unknown"

                    // We will show the top confident detection result if it exist
                    if (obj.labels.isNotEmpty()) {
                        val firstLabel = obj.labels.first()
                        text = "${firstLabel.text}, ${firstLabel.confidence.times(100).toInt()}%"
                    }
//      BoxWithText(obj.boundingBox, text)
                    try{

                        var pointX : Float = (obj.boundingBox.left + ((obj.boundingBox.right - obj.boundingBox.left)/2)).toFloat()
                        var pointY : Float = (obj.boundingBox.top + ((obj.boundingBox.bottom - obj.boundingBox.top)/2)).toFloat()
                        Log.v("PointCheck", "x,y : ${pointX}, ${pointY}")

                        pointArrayList.add(pointData(pointX, pointY))

                    } catch ( e: IllegalAccessException) {
                        e.printStackTrace();
                    } catch ( e: InvocationTargetException) {
                        e.targetException.printStackTrace(); //getTargetException
                    }

                }
                isPointArrayFull = true

            }
            .addOnFailureListener{
                Log.e(TAG, it.message.toString())
            }

        Log.v("Test num", "takeFocusPhoto()")
        while(!isPointArrayFull){
            Log.v("Test num", "Wait... ${isPointArrayFull}")
            if(isPointArrayFull == true){
                Log.v("Test num", "!!!!!!!!!! : ${isPointArrayFull}")
                isPointArrayFull = false
                takeFocusPhoto(0)
                break
            }
        }
        // 이미지가 다 저장되기를 기다리는 while문
            while(!isImageArrayFull){
                Log.v("Test num", "Wait22... ${isPointArrayFull}")
               if(isImageArrayFull == true){
                    Log.d("이미지","${imageArrayList.size.toString()}개의 파일 합치기 시작: ")
                    // SOF~EOI합치기
                   insertFrameToJpeg()
                }

            }
    }
    fun insertFrameToJpeg(){
        var sourceByteArray : ByteArray? = null


        var destFrameByteArray : ByteArray? = null
        val outputStream = ByteArrayOutputStream()

        var context: Context = applicationContext
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
                        destImageByteList.add(it)
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
                    saveImageOnAboveAndroidQ(bitmap)
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
                        saveImageOnUnderAndroidQ(bitmap)
                    }
                   // Toast.makeText(context, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val requestExternalStorageCode = 1

                    val permissionStorage = arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    ActivityCompat.requestPermissions(
                        this@MainActivity,
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
    private fun debugPrint(detectedObjects: List<DetectedObject>) {
        detectedObjects.forEachIndexed { index, detectedObject ->
            val box = detectedObject.boundingBox

            Log.d(TAG, "Detected object: $index")
            Log.d(TAG, " trackingId: ${detectedObject.trackingId}")
            Log.d(TAG, " boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")
            detectedObject.labels.forEach {
                Log.d(TAG, " categories: ${it.text}")
                Log.d(TAG, " confidence: ${it.confidence}")
            }

        }

    }


    /**
     *  convert image proxy to bitmap
     *  @param image
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * Open a camera app to take photo.
     */
    private fun dispatchTakePictureIntent() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                //get bitmap from image
                val bitmap = imageProxyToBitmap(image)
                image.close()
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 1080, 1440, false)
                runObjectDetection(resizedBitmap)
                super.onCaptureSuccess(image)

            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }

        })

    }



    private fun takeFocusPhoto( index : Int ){
        Log.v("Test num", "${pointArrayList.size}")
        Log.v("Test num", "takeFocusPhoto ${index}")

        if(index >= pointArrayList.size)
            return

        Log.v("Test num", "${pointArrayList.get(index)}")
        Log.v("Test num", "takeFocusPhoto ${index}")

        var point = factory.createPoint(pointArrayList.get(index).x, pointArrayList.get(index).y)
        var action = FocusMeteringAction.Builder(point)
            .build()

        Log.v("Test num", "takeFocusPhoto ${index}")

        var result = cameraController?.startFocusAndMetering(action)
        result?.addListener({
            try {
                Log.v("Test num", "takeFocusPhoto ${result}")
                Log.v("Test num", "takeFocusPhoto ${isFocusSuccess}")
                isFocusSuccess = result.get().isFocusSuccessful
                Log.v("Test num", "takeFocusPhoto ${isFocusSuccess}")
            } catch (e:IllegalAccessException){
                Log.e("Error", "IllegalAccessException")
            } catch (e:InvocationTargetException){
                Log.e("Error", "InvocationTargetException")
            }

            if (isFocusSuccess == true) {
                isFocusSuccess = false
                takePhoto()
                takeFocusPhoto(index+1)
            }
        }, ContextCompat.getMainExecutor(this))

//        var result = cameraController?.startFocusAndMetering(action)!!
//        result?.addListener({
//            try {
//                Log.v("Test num", "takeFocusPhoto ${result}")
//                Log.v("Test num", "takeFocusPhoto ${isFocusSuccess}")
//                isFocusSuccess = result?.get()?.isFocusSuccessful
//                Log.v("Test num", "takeFocusPhoto ${isFocusSuccess}")
//            }catch (e:IllegalAccessException){
//                Log.e("Error", "IllegalAccessException")
//            }catch (e: InvocationTargetException){
//                Log.e("Error", "InvocationTargetException")
//            }
//
//            if (isFocusSuccess == true) {
//                isFocusSuccess = false
//                takePhoto()
//                takeFocusPhoto(index+1)
//            }
//        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(){
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val iStream: InputStream? = contentResolver.openInputStream(output.savedUri!!)
                    var sourceByteArray = getBytes(iStream!!)
                    imageArrayList.add(sourceByteArray)
                    Log.d("이미지", "takePhoto: "  +imageArrayList.size.toString())
                      // 초점 사진들이 모두 저장 완료 되었을 때
                    if(imageArrayList.size == pointArrayList.size){
                        Log.d("이미지", "모두 저장 완료: "  +imageArrayList.size.toString())
                        isImageArrayFull = true
                    }
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    private fun captureVideo(){}

    private fun startCamera(){
        // 1. CameraProvider 요청
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // 2. CameraProvier 사용 가능 여부 확인
            // 생명주기에 binding 할 수 있는 ProcessCameraProvider 객체 가져옴
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 3. 카메라를 선택하고 use case를 같이 생명주기에 binding

            // 3-1. Preview를 생성 → Preview를 통해서 카메라 미리보기 화면을 구현.
            // surfaceProvider는 데이터를 받을 준비가 되었다는 신호를 카메라에게 보내준다.
            // setSurfaceProvider는 PreviewView에 SurfaceProvider를 제공해준다.
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // 3-2. 카메라 세팅
            // CameraSelector는 카메라 세팅을 맡는다.(전면, 후면 카메라)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // binding 전에 binding 초기화
                cameraProvider.unbindAll()

                // 3-3. use case와 카메라를 생명 주기에 binding
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

                cameraController = camera!!.cameraControl

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraX"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = // Array<String>
            mutableListOf (
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
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