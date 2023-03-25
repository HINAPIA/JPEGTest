package com.example.test_editafterfocus

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsSeekBar
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.test_editafterfocus.databinding.FragmentCameraBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment() {

    data class pointData (var x : Float, var y : Float)

    private lateinit var camera : Camera
    private var cameraController : CameraControl ?= null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var factory: MeteringPointFactory
    private var imageCapture: ImageCapture? = null
    private var isFocusSuccess : Boolean? = null
    lateinit var mainActivity: AppCompatActivity
    private lateinit var viewBinding : FragmentCameraBinding

    private var pointArrayList: ArrayList<pointData> = arrayListOf()
    private var previewByteArrayList : ArrayList<ByteArray> = arrayListOf()

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

        if(allPermissionsGranted()){
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                mainActivity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }


        val displayHeight = resources.displayMetrics.heightPixels
        val displayWidth = resources.displayMetrics.widthPixels
        Log.v("Size Info", "hxw : ${displayHeight}x${displayWidth}")

        val params: ConstraintLayout.LayoutParams = viewBinding.viewFinder.layoutParams as ConstraintLayout.LayoutParams
        params.width = 1080
        params.height = 1440
        viewBinding.viewFinder.layoutParams = params


        /**
         * SeekBar 조절
         */
        viewBinding.seekBar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.v("Seek", "onProgressChanged")
                val distance : Float = (10f/20f)*progress.toFloat()
                Camera2CameraControl.from(camera.cameraControl).captureRequestOptions = CaptureRequestOptions.Builder()
                    .apply {
                        setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                        setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, distance) }.build()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.v("Seek", "onStartTrackingTouch")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Log.v("Seek", "onStopTrackingTouch")
            }
        })


        /**
         * 사진 1장 찍고 저장하는 Button
         */
        viewBinding.imageCaptureButton.setOnClickListener {
            takePhoto()
        }


        /**
         * 임의로 3개 좌표 지정해서 Multi Focus 찍은 후 사진 저장하는 Button
         */
        viewBinding.multiFocusButton.setOnClickListener{
            factory = viewBinding.viewFinder.meteringPointFactory

            // 임의의 좌표값 3개를 arrayList에 저장
            pointArrayList.clear()
            pointArrayList.add(pointData(289f, 800f))
            pointArrayList.add(pointData(640f, 800f))
            pointArrayList.add(pointData(760f, 800f))

            //arraylist 에 있는 좌표들에 Focus 줘서 사진찍기
//            takeFocusPhoto(0)

        }

        /**
         * 객체 인식 후, 중간 좌표 계산 후 사진 찍는 Button
         */
        viewBinding.objectDetectionButton.setOnClickListener {
            factory = viewBinding.viewFinder.meteringPointFactory

            // 임의의 좌표값 3개를 arrayList에 저장
            pointArrayList.clear()
            isFocusSuccess = false

            dispatchTakePictureIntent()
        }

        /**
         * 화면 터치 하면 Toast로 좌표 나오고 Focus 맞춤
         */
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
                    Toast.makeText(activity,
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

        // Inflate the layout for this fragment
        return viewBinding.root
    }


    /**
     * 카메라 Preview에서 Bitmap 가져오고 runObjectDetection에 넘겨주기
     */
    private fun dispatchTakePictureIntent() {
        pointArrayList.clear()
        previewByteArrayList.clear()

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                //get bitmap from image
                val bitmap = imageProxyToBitmap(image)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 1080, 1440, false)
                runObjectDetection(resizedBitmap)

                image.close()
                super.onCaptureSuccess(image)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    /**
     * ImageProxy ===> Bitmap
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * ML Kit 사용해서 객체 인식 ===> 가운데 좌표값 계산 ===> 촬영
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

                // Parse ML Kit's DetectedObject and create corresponding visualization data
                for (obj in it) {
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

                takeFocusPhoto(0, previewByteArrayList)
            }
            .addOnFailureListener{
                Log.e(TAG, it.message.toString())
            }
    }

    /**
     * Log
     */
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
     *     takeFocusPhoto() 함수에서 호출하는 함수
     */
    private fun processCapturedImages(images: ArrayList<ByteArray>) {
//        // 파일 경로 지정해서 넘겨주는 방법 생각 중 ...
//        val file = File(filePath, fileName)
//
//        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()


//        imageCapture?.takePicture(outputFileOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
//            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                // 이미지 저장 성공
//            }
//
//            override fun onError(exception: ImageCaptureException) {
//                // 이미지 저장 실패
//            }
//        })


        val bundle = Bundle()
        bundle.putSerializable("image", images)
        val fragment = EditorFragment()
        fragment.arguments = bundle
        mainActivity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragment, fragment)
            .addToBackStack(null) // 뒤로 가기 허용
            .commit()
    }


    /**
     * pointArrayList 에 있는 중간 좌표에 Focus 맞추기 ===> Preview를 ByteArray로 저장
     */
    private fun takeFocusPhoto(index: Int, images: ArrayList<ByteArray>) {
        if (index >= pointArrayList.size) {
            processCapturedImages(images)
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
                isFocusSuccess = false
                addPreviewByteArray(images)
                takeFocusPhoto(index + 1, images)
            }
        }, ContextCompat.getMainExecutor(mainActivity))
    }

    /**
     * Preview에서 초점에 맞은 화면을 ByteArray로 가져옴
     */
    // addPreviewByteArray() 함수 수정
    private fun addPreviewByteArray(images: ArrayList<ByteArray>) {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                buffer.rewind()
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                images.add(bytes)

                image.close()
                super.onCaptureSuccess(image)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    /**
     * 사진 촬영 후 저장
     */
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
            .Builder(mainActivity.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
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
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(mainActivity, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    /**
     * 카메라 Setting
     */
    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun startCamera(){
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
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // 3-2. 카메라 세팅
            // CameraSelector는 카메라 세팅을 맡는다.(전면, 후면 카메라)
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                // binding 전에 binding 초기화
                cameraProvider.unbindAll()

                // 3-3. use case와 카메라를 생명 주기에 binding
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

                cameraController = camera!!.cameraControl

//                val extender = Camera2Interop.Extender(preview)
//                extender.setCaptureRequestOption(
//                    CaptureRequest.CONTROL_AF_MODE,
//                    CameraMetadata.CONTROL_AF_MODE_OFF
//                )
//                extender.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, distance)
//

                val camera2CameraInfo = Camera2CameraInfo.from(camera.cameraInfo)
                val minFocusDistance = camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

                Log.v("LensFocusDistance", "minFocusDistance : $minFocusDistance")

                Camera2CameraControl.from(camera.cameraControl).captureRequestOptions = CaptureRequestOptions.Builder()
                    .apply {
                        setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
//                        setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)

                        // Fix focus lens distance to infinity to get focus far away (avoid to get a close focus)
                        setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, 0f)
                    }
                    .build()


//                Camera2CameraControl.from(camera.cameraControl).captureRequestOptions = CaptureRequestOptions.Builder()
//                    .apply { setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, 5f) }.build()

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(mainActivity))

    }

//    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
//    fun setFocusDistance(builder: ExtendableBuilder<*>?, distance: Float) {
//        val extender: Camera2Interop.Extender<*> = Camera2Interop.Extender<Any?>(builder)
//        extender.setCaptureRequestOption(
//            CaptureRequest.CONTROL_AF_MODE,
//            CameraMetadata.CONTROL_AF_MODE_OFF
//        )
//        extender.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, distance)
//    }

    /**
     * 카메라 권한 확인하기
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all{
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