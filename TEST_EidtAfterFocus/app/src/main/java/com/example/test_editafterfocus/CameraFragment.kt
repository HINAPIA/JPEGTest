package com.example.test_editafterfocus

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.test_editafterfocus.databinding.FragmentCameraBinding
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.objects.DetectedObject
//import com.google.mlkit.vision.objects.ObjectDetection
//import com.google.mlkit.vision.objects.ObjectDetector
//import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import org.tensorflow.lite.support.image.TensorImage
import java.lang.Thread.sleep
import java.lang.reflect.Array.set
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import org.tensorflow.lite.task.vision.detector.ObjectDetector
import kotlin.math.max

@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
class CameraFragment : Fragment() {

    data class pointData(var x: Float, var y: Float)
    data class DetectionResult(val boundingBox: RectF, val text: String)

    private var pointArrayList: ArrayList<pointData> = arrayListOf() // Object Focus
    private var previewByteArrayList: ArrayList<ByteArray> = arrayListOf()

    lateinit var mainActivity: AppCompatActivity
    private lateinit var viewBinding: FragmentCameraBinding
    private lateinit var mediaPlayer : MediaPlayer

    // Camera
    private lateinit var camera: Camera
    private var cameraController: CameraControl? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera2CameraInfo: Camera2CameraInfo
    private var imageCapture: ImageCapture? = null

    // TFLite
    private lateinit var customObjectDetector: ObjectDetector
    private lateinit var detectedList: List<DetectionResult>

    // Distance Focus
    private var lensDistanceSteps: Float = 0F
    private var minFocusDistance: Float = 0F

    // Object Focus
    private lateinit var factory: MeteringPointFactory
    private var isFocusSuccess: Boolean? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewBinding = FragmentCameraBinding.inflate(inflater, container, false)

        factory = viewBinding.viewFinder.meteringPointFactory
        mediaPlayer = MediaPlayer.create(context, R.raw.end_sound)

        // Initialize the detector object
        setDetecter()

        /**
         * 카메라 권한 확인 후 카메라 세팅
         */
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                mainActivity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        /**
         * radioGroup.setOnCheckedChangeListener
         *      1. Basic 버튼 눌렸을 때, Single Mode나 Burst Mode 선택 버튼이 나타나게 하기
         *      2. Basic 버튼 안 누르면 사라지게 하기
         *      3. Option에 따른 카메라 설정
         */
        viewBinding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId){
                viewBinding.basicRadio.id -> {
                    viewBinding.basicToggle.visibility = View.VISIBLE
                    turnOnAEMode()
                }

                viewBinding.distanceFocusRadio.id -> {
                    viewBinding.basicToggle.visibility = View.INVISIBLE
                    turnOffAFMode(0F)
                }

                else -> {
                    viewBinding.basicToggle.visibility = View.INVISIBLE
                    turnOnAEMode()
                }
            }
        }

        /**
         * shutterButton.setOnClickListener{ }
         *      - 셔터 버튼 눌렀을 때 Option에 따른 촬영
         */
        viewBinding.shutterButton.setOnClickListener {
            // previewByteArrayList 초기화
            previewByteArrayList.clear()

            // Basic Mode
            if(viewBinding.basicRadio.isChecked){
                if(!(viewBinding.basicToggle.isChecked)){
                    // Single Mode
//                    takePhotoIndex(0, 1)
                    previewToByteArray()
                    mediaPlayer.start()
                }
                else{
                    // Burst Mode
//                    takePhotoIndex(0, 10)
                    takeBurstMode(0,10)
                }
            }

            else if(viewBinding.objectFocusRadio.isChecked){
                pointArrayList.clear()
                isFocusSuccess = false

                startObjectFocusMode()
            }

            else if(viewBinding.distanceFocusRadio.isChecked){
                controlLensFocusDistance(0)
            }

            else if(viewBinding.autoRewindRadio.isChecked){

            }
        }


        /**
         * 사진 1장 찍고 저장하는 Button
         */
//        viewBinding.imageCaptureButton.setOnClickListener {
//            takePhoto()
//        }

        /**
         * Lens Focus Distance별로 사진 찍기
         */
//        viewBinding.multiFocusButton.setOnClickListener {
//            previewByteArrayList.clear()
//            controlLensFocusDistance(0)
////            captureLensFocusDistanceSeries(5)
//        }



        /**
         * 화면 터치
         * 터치한 좌표로 초점 맞추기
         */
        viewBinding.viewFinder.setOnTouchListener { v: View, event: MotionEvent ->
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

        // Inflate the layout for this fragment
        return viewBinding.root
    }

    private fun setDetecter() {
        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(10)          // 최대 결과 (모델에서 감지해야 하는 최대 객체 수)
            .setScoreThreshold(0.2f)    // 점수 임계값 (감지된 객체를 반환하는 객체 감지기의 신뢰도)
            .build()
        customObjectDetector = ObjectDetector.createFromFileAndOptions(
            mainActivity,
            "lite-model_efficientdet_lite0_detection_metadata_1.tflite",
            options
        )
    }

    private fun turnOffAFMode(distance : Float){
        Camera2CameraControl.from(camera.cameraControl).captureRequestOptions =
            CaptureRequestOptions.Builder()
                .apply {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_OFF
                    )
                    // Fix focus lens distance to infinity to get focus far away (avoid to get a close focus)
                    setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, distance)
                }.build()
    }

    private fun turnOnAEMode(){
        Camera2CameraControl.from(camera.cameraControl).captureRequestOptions =
            CaptureRequestOptions.Builder()
                .apply {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_AUTO
                    )
                }.build()
    }

    /**
     * < TEST >
     * takePhotoIndex(index : Int, maxIndex : Int)
     *      - 사진 촬영 시, 저장
     */
    private fun takePhotoIndex(index : Int, maxIndex : Int) {
        if(index >= maxIndex){
            mediaPlayer.start()
            return
        }
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                mainActivity.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(mainActivity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(mainActivity, msg, Toast.LENGTH_SHORT).show()
                    takePhotoIndex(index + 1, maxIndex)
                }
            }
        )
    }

    /**
     * burstMode(index : Int, maxIndex : Int)
     *      - 연속 촬영 모드
     *          재귀로 돌면서 preview를 ByteArray로 변환
     *          previewByteArrayList에 저장
     */
    private fun takeBurstMode(index : Int, maxIndex : Int){
        if(index >= maxIndex){
            mediaPlayer.start()
            return
        }

        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                buffer.rewind()
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                previewByteArrayList.add(bytes)
                val msg = "previewByteArrayList.Size : ${previewByteArrayList.size}"
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
//                Log.v("previewByteArrayList", "previewByteArrayList.Size : ${previewByteArrayList.size}")
                image.close()
                //다시 호출 ( 재귀 함수 )
                takeBurstMode(index + 1, maxIndex)
                super.onCaptureSuccess(image)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    private fun previewToByteArray(){
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                buffer.rewind()
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                previewByteArrayList.add(bytes)
                val msg = "previewByteArrayList.Size : ${previewByteArrayList.size}"
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                image.close()
                super.onCaptureSuccess(image)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    /**
     * startObjectFocusMode()
     *      - Preview에서 Bitmap 가져오고 runObjectDetection에 넘겨주기
     */
    private fun startObjectFocusMode() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("RestrictedApi")
            override fun onCaptureSuccess(image: ImageProxy) {
                //get bitmap from image
                val bitmap = imageProxyToBitmap(image)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 1080, 1440, false)

                // resizedBitmap 에서 객체 인식하기
                runObjectDetection(resizedBitmap)

                // 객체 별로 초점 맞춰서 저장
                takeObjectFocusMode(0)

                image.close()
                super.onCaptureSuccess(image)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    /**
     * imageProxyToBitmap(image: ImageProxy): Bitmap
     *      - ImageProxy를 Bitmap으로 변환
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      - Tensorflow Lite 객체 인식
     *          객체 별 가운데 좌표 계산 > 초점 > 촬영
     */
    private fun runObjectDetection(bitmap: Bitmap) {
        // 객체 인지 후 객체 정보 받기
        detectedList = getObjectDetection(bitmap)

        for (obj in detectedList) {
            try {
                var pointX: Float =
                    (obj.boundingBox.left + ((obj.boundingBox.right - obj.boundingBox.left) / 2))
                var pointY: Float =
                    (obj.boundingBox.top + ((obj.boundingBox.bottom - obj.boundingBox.top) / 2))

                pointArrayList.add(pointData(pointX, pointY))

            } catch (e: IllegalAccessException) {
                e.printStackTrace();
            } catch (e: InvocationTargetException) {
                e.targetException.printStackTrace(); //getTargetException
            }
        }
    }

    /**
     * getObjectDetection(bitmap: Bitmap):
     *         ObjectDetection 결과(bindingBox) 및 category 그리기
     */
    private fun getObjectDetection(bitmap: Bitmap): List<DetectionResult> {
        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 3: Feed given image to the detector
        val results = customObjectDetector.detect(image)

        // Step 4: Parse the detection result and show it
        val resultToDisplay = results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            val text = "${category.label}"

            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, text)
        }
        return resultToDisplay
    }

    /**
     * takeObjectFocusMode(index: Int)
     *      - 감지된 객체 별로 초점을 맞추고
     *          Preview를 ByteArray로 저장
     */
    private fun takeObjectFocusMode(index: Int) {
        if(index >= detectedList.size){
            mediaPlayer.start()
            return
        }

        val point = factory.createPoint(pointArrayList[index].x, pointArrayList[index].y)
        val action = FocusMeteringAction.Builder(point)
            .build()

        val result = cameraController?.startFocusAndMetering(action)
        result?.addListener({
            try {
                isFocusSuccess = result.get().isFocusSuccessful
            } catch (e: IllegalAccessException) {
                Log.e("Error", "IllegalAccessException")
            } catch (e: InvocationTargetException) {
                Log.e("Error", "InvocationTargetException")
            }

            if (isFocusSuccess == true) {
//                takePhoto()
                previewToByteArray()
                takeObjectFocusMode(index + 1)
                isFocusSuccess = false
            }
        }, ContextCompat.getMainExecutor(mainActivity))
    }


    /**
     * Lens Focus Distance 바꾸면서 사진 찍기
     */
    private fun controlLensFocusDistance(photoCnt: Int) {
        if (photoCnt >= DISTANCE_FOCUS_PHOTO_COUNT){
            mediaPlayer.start()
            return
        }

        val distance: Float? = 0F + lensDistanceSteps * photoCnt
        turnOffAFMode(distance!!)

        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                buffer.rewind()
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                previewByteArrayList.add(bytes)
                image.close()
                controlLensFocusDistance(photoCnt + 1)
                super.onCaptureSuccess(image)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    /**
     * takePhoto()
     *      - 사진 촬영 후 저장
     *          오로지 저장이 잘 되는지 확인하는 용도
     */
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                mainActivity.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(mainActivity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(mainActivity, msg, Toast.LENGTH_SHORT).show()
//                    mediaPlayer.start()
                }
            }
        )
    }

    /**
     * startCamera()
     *      - 카메라 Setting
     */
    private fun startCamera() {
        // 1. CameraProvider 요청
        val cameraProviderFuture = ProcessCameraProvider.getInstance(mainActivity)
        cameraProviderFuture.addListener({

            // 2. CameraProvier 사용 가능 여부 확인
            // 생명주기에 binding 할 수 있는 ProcessCameraProvider 객체 가져옴
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 3. 카메라를 선택하고 use case를 같이 생명주기에 binding

            // 3-1. Preview를 생성 → Preview를 통해서 카메라 미리보기 화면을 구현.
            // surfaceProvider는 데이터를 받을 준비가 되었다는 신호를 카메라에게 보내준다.
            // setSurfaceProvider는 PreviewView에 SurfaceProvider를 제공해준다.
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // Preview 4:3 비율
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
                    this, cameraSelector, preview, imageCapture
                )

                cameraController = camera!!.cameraControl
                camera2CameraInfo = Camera2CameraInfo.from(camera.cameraInfo)

                // 스마트폰 기기 별 min Focus Distance 알아내기 ( 가장 `가까운` 곳에 초점을 맞추기 위한 렌즈 초점 거리 )
                // 대부분 10f
                minFocusDistance =
                    camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)!!
                // 연속 사진 촬영 장수에 따른 Step 거리
                lensDistanceSteps = minFocusDistance / (DISTANCE_FOCUS_PHOTO_COUNT.toFloat())

                Camera2CameraControl.from(camera.cameraControl).captureRequestOptions =
                    CaptureRequestOptions.Builder()
                        .apply {
                            setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                        }
                        .build()

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(mainActivity))

    }

    /**
     * 카메라 권한 확인하기
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(mainActivity, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "TEST_EditAfterFocus"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val DISTANCE_FOCUS_PHOTO_COUNT = 10
        private val REQUIRED_PERMISSIONS = // Array<String>
            mutableListOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}