package com.example.camerax

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.FocusMeteringAction.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.camerax.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding : ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var isFocusSuccess : Boolean? = null
    private lateinit var factory: MeteringPointFactory
    private val REQUEST_IMAGE_CAPTURE: Int = 1
    private lateinit var currentPhotoPath: String
    private var isPointArrayFull: Boolean = false

    // CameraController
    private lateinit var camera : Camera
    private var cameraController : CameraControl ?= null
    private lateinit var cameraInfo: CameraInfo

    data class pointData (var x : Float, var y : Float)

    private var pointArrayList: ArrayList<pointData> = arrayListOf<pointData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        Log.v("Test num", "Build.VERSION.SDK_INT = " + Build.VERSION.SDK_INT)

        // 카메라 권한 요청
        if(allPermissionsGranted()){
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

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

                Log.v("Test num", "pointArray size : ${pointArrayList.size}")
                // Parse ML Kit's DetectedObject and create corresponding visualization data
                val detectedObjects = it.map { obj ->
                    var text = "Unknown"

                    // We will show the top confident detection result if it exist
                    if (obj.labels.isNotEmpty()) {
                        val firstLabel = obj.labels.first()
                        text = "${firstLabel.text}, ${firstLabel.confidence.times(100).toInt()}%"
                    }
//      BoxWithText(obj.boundingBox, text)
                    try{
                        pointArrayList.add(pointData((obj.boundingBox.left + ((obj.boundingBox.right - obj.boundingBox.left)/2)).toFloat()
                            , (obj.boundingBox.top + ((obj.boundingBox.bottom - obj.boundingBox.top)/2)).toFloat()))
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
                runObjectDetection(bitmap)
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

        var point = factory.createPoint(pointArrayList[index].x, pointArrayList[index].y)
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

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
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
}