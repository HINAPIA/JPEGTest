/**
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.codelabs.objectdetection

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toRectF
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.collections.indices as indices1


class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val TAG = "TFLite - ODT"
        const val REQUEST_IMAGE_CAPTURE: Int = 1
        private const val MAX_FONT_SIZE = 96F
    }
    private lateinit var captureImageFab: Button
    private lateinit var eyeOpenButton: Button
    private lateinit var smileButton: Button
    private lateinit var allButton: Button
    private lateinit var imageView: ImageView
    private lateinit var inputImageView: ImageView
    private lateinit var imgSampleOne: ImageView
    private lateinit var imgSampleTwo: ImageView
    private lateinit var imgSampleThree: ImageView
    private lateinit var imgSampleFour: ImageView
    private lateinit var tvPlaceholder: TextView
    private lateinit var errorMessage: TextView
    private lateinit var currentPhotoPath: String
    private lateinit var testDrawble : Array<Int>
    private var imageNum: Int = 0

    private lateinit var detector: FaceDetector

    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        captureImageFab = findViewById(R.id.captureImageFab)
        eyeOpenButton = findViewById(R.id.eyeOpenButton)
        allButton = findViewById(R.id.allButton)
        smileButton = findViewById(R.id.smileButton)
        imageView = findViewById(R.id.imageView)
        inputImageView = findViewById(R.id.imageView)
        imgSampleOne = findViewById(R.id.imgSampleOne)
        imgSampleTwo = findViewById(R.id.imgSampleTwo)
        imgSampleThree = findViewById(R.id.imgSampleThree)
        imgSampleFour = findViewById(R.id.imgSampleFour)
        tvPlaceholder = findViewById(R.id.tvPlaceholder)
        errorMessage = findViewById(R.id.errorMessage)

        imageView.setOnTouchListener { view, motionEvent ->
            runOnUiThread {
                errorMessage.setText("실행 중")
            }

            // click 좌표를 bitmap에 해당하는 좌표로 변환
            val point = getBitmapClickPoint(PointF(motionEvent.x, motionEvent.y), imageView)

            val drawable = imageView.drawable
            if(imageNum == 1)
                changeFaceOneByOne(drawable.toBitmap(), getSampleImage(R.drawable.image2), point.x, point.y)
            else if (imageNum == 2)
                changeFaceOneByOne(drawable.toBitmap(), getSampleImage(R.drawable.twoface2), point.x, point.y)

            return@setOnTouchListener true
        }
        captureImageFab.setOnClickListener(this)
        imgSampleOne.setOnClickListener(this)
        imgSampleTwo.setOnClickListener(this)
        imgSampleThree.setOnClickListener(this)
        imgSampleFour.setOnClickListener(this)
        eyeOpenButton.setOnClickListener(this)
        smileButton.setOnClickListener(this)
        allButton.setOnClickListener(this)

        setFaceDetecter()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE &&
            resultCode == Activity.RESULT_OK
        ) {
            setViewAndDetect(getCapturedImage())
        }
    }

    /**
     * onClick(v: View?)
     *      Detect touches on the UI components
     */
    override fun onClick(v: View?) {
        imageNum = 0
        when (v?.id) {
            R.id.captureImageFab -> {
                try {
                    dispatchTakePictureIntent()
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
            R.id.imgSampleOne -> {
                //setViewAndDetect(getSampleImage(R.drawable.image1))
                imageNum = 1
                runOnUiThread {
                    inputImageView.setImageBitmap(getSampleImage(R.drawable.image1))
                }
            }
            R.id.imgSampleTwo -> {
                //setViewAndDetect(getSampleImage(R.drawable.image2))
                imageNum = 2
                testDrawble = arrayOf(R.drawable.twoface, R.drawable.twoface2)

                runOnUiThread {
                    inputImageView.setImageBitmap(getSampleImage(R.drawable.twoface))
                }
            }
            R.id.imgSampleThree -> {
                testDrawble = arrayOf(R.drawable.image1, R.drawable.image2)

                runOnUiThread {
                    inputImageView.setImageBitmap(getSampleImage(R.drawable.image2))
                }
            }
            R.id.imgSampleFour -> {
                testDrawble = arrayOf(R.drawable.auto_rewind_best, R.drawable.auto_rewind1,
                    R.drawable.auto_rewind2, R.drawable.auto_rewind3,
                    R.drawable.auto_rewind4, R.drawable.auto_rewind5)

                runOnUiThread {
                    inputImageView.setImageBitmap(getSampleImage(R.drawable.auto_rewind1))
                }
            }
            R.id.eyeOpenButton -> {
                runOnUiThread {
                    errorMessage.setText("실행 중")
                }
                autoBestFaceChange(testDrawble,0)
            }
            R.id.smileButton -> {
                runOnUiThread {
                    errorMessage.setText("실행 중")
                }
                autoBestFaceChange(testDrawble,1)
            }
            R.id.allButton -> {
                runOnUiThread {
                    errorMessage.setText("실행 중")
                }
                autoBestFaceChange(testDrawble,2)
            }

        }
    }

    private fun autoBestFaceChange(testDrawble: Array<Int>, bestFaceStandard: Int) {

        var basicFacesResult: List<Face>
        val basicInformation: HashMap<Int, Face> = hashMapOf()

        val faceList: ArrayList<List<Face>> = ArrayList()

        CoroutineScope(Dispatchers.Default).launch {
            basicFacesResult = getFaceDetectionOneByOne(getSampleImage(testDrawble[0]))!!
            faceList.add(basicFacesResult)

            val checkFaceDetection: ArrayList<Boolean> = arrayListOf()

            for(i in 0 until testDrawble.size)
                checkFaceDetection.add(false)

            for (j in 0 until testDrawble.size) {
                //CoroutineScope(Dispatchers.Default).launch {
                    if(j==0) {
                        for (i in basicFacesResult.indices1) {
                            basicInformation[i] = basicFacesResult[i]
                        }
                    }
                    // j 번째 사진 faces 정보 얻기
                    val facesResult =
                        getFaceDetectionOneByOne(getSampleImage(testDrawble[j]))
                    faceList.add(facesResult!!)
                    checkFaceDetection[j] = true
                //}
                //Thread.sleep(3000)
            }
//            while (true) {
//                var allTrue = true
//                for (i in 0 until testDrawble.size) {
//                    if (!checkFaceDetection[i]) {
//                        allTrue = false
//                        break
//                    }
//                }
//                Thread.sleep(500)
//                if (allTrue)
//                    break
//            }

            val bestFaceMap = getBestFaceList(faceList, bestFaceStandard)
            var resultBitmap = getSampleImage(testDrawble[0])

            for (i in 0 until bestFaceMap.size) {
                // 현재 face가 비교될 face 배열의 index 값
                val index = getBoxComparisonIndex(bestFaceMap[i]!!, basicInformation)
                resultBitmap = getFaceChangeBitmap(
                    resultBitmap, getSampleImage(testDrawble[i]),
                    basicInformation[index]!!.boundingBox.toRectF(),
                    basicInformation[index]!!.allLandmarks, bestFaceMap[i]!!.allLandmarks
                )
            }

            runOnUiThread {
                inputImageView.setImageBitmap(resultBitmap)
                errorMessage.setText("")
            }
        }

    }

    private fun getBestFaceList(faceList : ArrayList<List<Face>>, bestFaceStandard: Int) : HashMap<Int, Face> {
        val bestFaceMap : HashMap<Int, Face> = hashMapOf()

        for(j in 0 until faceList.size){
            val facesResult = faceList[j]
            if(j == 0) {
                // 첫번째 인덱스일 경우 비교를 위한 변수 초기화
                for(checkFace in facesResult) {
                    bestFaceMap[j] = checkFace
                }
            }

            else  {
            // 그 이후 인덱스일 경우, 각 face을 비교
                for (checkFace in facesResult) {
                    // 현재 face가 비교될 face 배열의 index 값
                    var index = getBoxComparisonIndex(checkFace, bestFaceMap)

                    // bestFace 정보 알아내기
                    val best = bestFaceMap[index]

                    if(bestFaceStandard == 0) { // 베스트 사진의 기준이 눈일 때
                        // 가장 눈을 뜬 사진 알아내기
                        if(best!!.leftEyeOpenProbability!! < checkFace.leftEyeOpenProbability!! ||
                            best.rightEyeOpenProbability!! < checkFace.rightEyeOpenProbability!!) {
                            bestFaceMap[j] = checkFace
                        }
                    }
                    else if(bestFaceStandard == 1) { // 베스트 사진의 기준이 입일 때
                        if(best!!.smilingProbability!! < checkFace.smilingProbability!!) {
                            bestFaceMap[j] = checkFace
                        }
                        else {  // 베스트 사진의 기준이 눈,입일 때
                            if(best.leftEyeOpenProbability!! < checkFace.leftEyeOpenProbability!! ||
                                best.rightEyeOpenProbability!! < checkFace.rightEyeOpenProbability!! &&
                                best.smilingProbability!! < checkFace.smilingProbability!!) {
                                bestFaceMap[j] = checkFace
                            }
                        }
                    }
                }
            }
        }
        return bestFaceMap
    }

    private fun getBoxComparisonIndex(check: Face, originalHashMap: HashMap<Int, Face>) : Int{
        var index = 0
        // 현재 face가 어떤 face인지 알기 도와주는 중간 값
        val checkPointX = check.boundingBox.left + (check.boundingBox.right - check.boundingBox.left)/2
        val checkPointY = check.boundingBox.top + (check.boundingBox.bottom - check.boundingBox.top)/2
        for(k in 0 until originalHashMap.size) {
            println("~~~~~~~~~"+checkPointX+" || "+checkPointY)
            if (originalHashMap != null && checkPointX >= originalHashMap[k]!!.boundingBox.left && checkPointX <= originalHashMap[k]!!.boundingBox.right &&
                checkPointY >= originalHashMap[k]!!.boundingBox.top && checkPointY <= originalHashMap[k]!!.boundingBox.bottom) {
                index = k
                break
            }

        }
        return index
    }

    private fun getBitmapClickPoint( clickPoint: PointF, imageView: ImageView) : Point {

        val bitmap:Bitmap = imageView.drawable.toBitmap()

        // imageView width, height 가져오기
        val viewWidth = imageView.width
        val viewHeight = imageView.height

        // 실제 이미지일 때 포인트 위치
        val newPointX = (bitmap.width * clickPoint.x)  / viewWidth
        val newPointY =(bitmap.height * clickPoint.y)  / viewHeight

        return Point(newPointX.toInt(), newPointY.toInt())
    }

    private fun getObjectDetection(bitmap: Bitmap): List<DetectionResult> {
        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(10)
            .setScoreThreshold(0.3f)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            this,
            "model.tflite",
            options
        )

        // Step 3: Feed given image to the detector
        val results = detector.detect(image)

        // Step 4: Parse the detection result and show it
        val resultToDisplay = results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            val text = "${category.label}, ${category.score.times(100).toInt()}%"

            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, text)
        }
        return resultToDisplay
    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     */
    private fun runObjectDetection(bitmap: Bitmap ) {

        // Object Detection
        //val resultToDisplay = getObjectDetection(bitmap)

        // face Detection
        val faceResultToDisplay = getFaceDetection(bitmap)

        // faceDetection 결과(bindingBox) 그리기
        val faceDetectionResultImg =
            faceResultToDisplay?.let { drawDetectionResult(bitmap, it, Color.RED) }

        // ObjectDetection 결과(bindingBox) 그리기
        //val imgWithResult =
        //    faceDetectionResultImg?.let { drawDetectionResult(it, resultToDisplay, Color.BLUE) }

        runOnUiThread {
            inputImageView.setImageBitmap(faceDetectionResultImg)
        }
    }

    private fun getFaceDetection(bitmap: Bitmap): List<DetectionResult>? {
        var faceResultToDisplay: List<DetectionResult>?  = null

        var returnState = false

        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()

        val image = InputImage.fromBitmap(bitmap, 0)
        val detector = FaceDetection.getClient(highAccuracyOpts)
        val result = detector.process(image)
            .addOnSuccessListener { faces ->
                faceResultToDisplay = faces.map {
                    // Get the top-1 category and craft the display text
                    val text = it.trackingId
                    val faceRect = RectF(it.boundingBox)
                    // Create a data object to display the detection result
                    DetectionResult(faceRect, text.toString())
                }
                returnState = true
            }
            .addOnFailureListener { e ->
                println("fail")
                returnState = true
            }
        while(!returnState) {
            System.out.println("wait")
            Thread.sleep(1000)
        }
        return faceResultToDisplay
    }

    private fun setFaceDetecter() {

        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
        detector = FaceDetection.getClient(highAccuracyOpts)
    }

    private fun getFaceDetectionOneByOne(bitmap: Bitmap): ArrayList<Face>? {
        var returnFaces : ArrayList<Face>? = null
        var returnState = false

        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                returnFaces = faces as ArrayList<Face>?
                returnState = true
            }
            .addOnFailureListener { e ->
                println("fail")
                returnState = true
            }
        while(!returnState) {
            System.out.println("wait || ")
            Thread.sleep(1000)
        }
        return returnFaces
    }

    private fun changeFace(originalImg: Bitmap, changeImg: Bitmap, changeFaceX: Int, changeFaceY: Int) {

        // Run ODT and display result
        // Note that we run this in the background thread to avoid blocking the app UI because
        // TFLite object detection is a synchronised process.
        lifecycleScope.launch(Dispatchers.Default) {
            val originalResultToDisplay = getFaceDetection(originalImg)
            val changeResultTODisplay = getFaceDetection(changeImg)

            var originalFaceBoundingBox : RectF? = null
            var changeFaceBoundingBox : RectF? = null

            if (originalResultToDisplay != null) {
                for(face in originalResultToDisplay) {
                    if (changeFaceX >= face.boundingBox.left && changeFaceX <= face.boundingBox.right &&
                        changeFaceY >= face.boundingBox.top && changeFaceY <= face.boundingBox.bottom
                    ) {
                        originalFaceBoundingBox = face.boundingBox
                        break
                    }
                }
            }

            if (changeResultTODisplay != null) {
                for (face in changeResultTODisplay) {
                    if (changeFaceX >= face.boundingBox.left && changeFaceX <= face.boundingBox.right &&
                        changeFaceY >= face.boundingBox.top && changeFaceY <= face.boundingBox.bottom
                    ) {
                        changeFaceBoundingBox = face.boundingBox
                        break
                    }
                }
            }
        }
    }

    private fun changeFaceOneByOne(originalImg: Bitmap, changeImg: Bitmap,changeFaceX: Int, changeFaceY: Int) {

        // Run ODT and display result
        // Note that we run this in the background thread to avoid blocking the app UI because
        // TFLite object detection is a synchronised process.
        lifecycleScope.launch(Dispatchers.Default) {
            val originalFacesResult = getFaceDetectionOneByOne(originalImg)
            val changeFacesResult = getFaceDetectionOneByOne(changeImg)

            var originalFaceLandmarks: List<FaceLandmark>? = null
            var changeFaceLandmarks: List<FaceLandmark>? = null

            var originalFaceBoundingBox: RectF? = null
            if (originalFacesResult != null) {
                for (face in originalFacesResult) {
                    if (changeFaceX >= face.boundingBox.left && changeFaceX <= face.boundingBox.right &&
                        changeFaceY >= face.boundingBox.top && changeFaceY <= face.boundingBox.bottom
                    ) {
                        originalFaceBoundingBox = RectF(face.boundingBox)
                        originalFaceLandmarks = face.allLandmarks
                        break
                    }
                }
            }

            if (originalFaceBoundingBox == null && originalFaceLandmarks == null)
                runOnUiThread {
                    errorMessage.setText("해당 포인트에는 객체가 존재하지 않음")
                }
            else if (changeFacesResult != null) {
                for (face in changeFacesResult) {
                    if (changeFaceX >= face.boundingBox.left && changeFaceX <= face.boundingBox.right &&
                        changeFaceY >= face.boundingBox.top && changeFaceY <= face.boundingBox.bottom
                    ) {
                        changeFaceLandmarks = face.allLandmarks
                        break
                    }
                }

                if (changeFaceLandmarks == null)
                    runOnUiThread {
                        errorMessage.setText("해당 포인트에는 change 객체가 존재하지 않음")
                    }
                else {
                    val resultBitmap = getFaceChangeBitmap(originalImg, changeImg, originalFaceBoundingBox!!,
                        originalFaceLandmarks!!, changeFaceLandmarks)
                    runOnUiThread {
                        inputImageView.setImageBitmap(resultBitmap)
                    }
                }
            }
        }
    }

    private fun getFaceChangeBitmap(originalImg:Bitmap, changeImg:Bitmap, originalFaceBoundingBox: RectF, originalFaceLandmarks: List<FaceLandmark>, changeFaceLandmarks: List<FaceLandmark>) : Bitmap {
        val addStartY =
            ((changeFaceLandmarks[0].position.y - changeFaceLandmarks[3].position.y) / 2).toInt()
        val addEndY =
            ((changeFaceLandmarks[0].position.y - changeFaceLandmarks[5].position.y) / 2).toInt()

        val cropImgRect = Rect(
            changeFaceLandmarks[2].position.x.toInt(), // left
            changeFaceLandmarks[3].position.y.toInt() - addStartY, // top
            changeFaceLandmarks[7].position.x.toInt(), // right
            changeFaceLandmarks[0].position.y.toInt() + addEndY  // bottom
        )

        val cropImg = cropBitmap(changeImg, cropImgRect)?.let { circleCropBitmap(it) }
        val overlayImg = cropImg?.let {
            overlayBitmap(
                originalImg, it,
                originalFaceBoundingBox,
                (originalFaceLandmarks[2].position.x).toInt(),
                (originalFaceLandmarks[3].position.y - addStartY).toInt()
            )
        }
        return overlayImg!!
    }

    private fun faceLandmark(originalImg: Bitmap, changeFaceX: Int) {

        // Run ODT and display result
        // Note that we run this in the background thread to avoid blocking the app UI because
        // TFLite object detection is a synchronised process.
        lifecycleScope.launch(Dispatchers.Default) {
            val originalFacesResult = getFaceDetectionOneByOne(originalImg)

            var originalFaceLandmarks : List<FaceLandmark>?  = null

            var originalFaceBoundingBox : RectF? = null
            originalFaceBoundingBox = RectF(originalFacesResult!!.get(changeFaceX).boundingBox)
            originalFaceLandmarks = originalFacesResult!!.get(changeFaceX).allLandmarks

            val imgWithResult =
                drawLandmarkResult(originalImg, originalFaceLandmarks!!)

            runOnUiThread {
                inputImageView.setImageBitmap(imgWithResult)
            }

        }
    }

    private fun cropBitmap(original: Bitmap, cropRect: Rect): Bitmap? {

        var width = (cropRect.right - cropRect.left)
        var height = cropRect.bottom - cropRect.top
        var startX = cropRect.left
        var startY = cropRect.top
        if (startX < 0)
            startX = 0
        else if (startX+width > original.width)
            width = original.width-startX
        if (startY < 0)
            startY = 0
        else if(startY+height > original.height)
            height = original.height-startY

        val result = Bitmap.createBitmap(
            original
            , startX         // X 시작위치
            , startY         // Y 시작위치
            , width          // 넓이
            , height         // 높이
        )
        if (result != original) {
            original.recycle()
        }
        return result
    }

    fun circleCropBitmap(bitmap: Bitmap): Bitmap? {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(
            (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(),
            (bitmap.width / 2).toFloat(), paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    fun overlayBitmap(original: Bitmap, add: Bitmap, rect:RectF, optimizationX:Int, optimizationY:Int): Bitmap? {

        var startX = optimizationX
        var startY = optimizationY

        if (startX < 0) {
            startX = 0
        }
        if (startY < 0) {
            startY = 0
        }

        //결과값 저장을 위한 Bitmap
        val resultOverlayBmp = Bitmap.createBitmap(
            original.width, original.height, original.config
        )

        //캔버스를 통해 비트맵을 겹치기한다.
        val canvas = Canvas(resultOverlayBmp)
        canvas.drawBitmap(original, Matrix(), null)
        canvas.drawBitmap(add, startX.toFloat(), startY.toFloat(), null)

        return resultOverlayBmp
    }


    /**
     * debugPrint(visionObjects: List<Detection>)
     *      Print the detection result to logcat to examine
     */
    private fun debugPrint(results : List<Detection>) {
        for ((i, obj) in results.withIndex()) {
            val box = obj.boundingBox

            Log.d(TAG, "Detected object: ${i} ")
            Log.d(TAG, "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")

            for ((j, category) in obj.categories.withIndex()) {
                Log.d(TAG, "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()
                Log.d(TAG, "    Confidence: ${confidence}%")
            }
        }
    }

    /**
     * setViewAndDetect(bitmap: Bitmap)
     *      Set image to view and call object detection
     */
    private fun setViewAndDetect(bitmap: Bitmap) {
        // Display capture image
        inputImageView.setImageBitmap(bitmap)
        tvPlaceholder.visibility = View.INVISIBLE


        // Run ODT and display result
        // Note that we run this in the background thread to avoid blocking the app UI because
        // TFLite object detection is a synchronised process.
        lifecycleScope.launch(Dispatchers.Default) { runObjectDetection(bitmap) }
    }

    /**
     * getCapturedImage():
     *      Decodes and crops the captured image from camera.
     */
    private fun getCapturedImage(): Bitmap {
        // Get the dimensions of the View
        val targetW: Int = inputImageView.width
        val targetH: Int = inputImageView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = max(1, min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inMutable = true
        }
        val exifInterface = ExifInterface(currentPhotoPath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateImage(bitmap, 90f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateImage(bitmap, 180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateImage(bitmap, 270f)
            }
            else -> {
                bitmap
            }
        }
    }

    /**
     * getSampleImage():
     *      Get image form drawable and convert to bitmap.
     */
    private fun getSampleImage(drawable: Int): Bitmap {
        return BitmapFactory.decodeResource(resources, drawable, BitmapFactory.Options().apply {
            inMutable = true
        })
    }

    /**
     * rotateImage():
     *     Decodes and crops the captured image from camera.
     */
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    /**
     * createImageFile():
     *     Generates a temporary image file for the Camera app to write to.
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    /**
     * dispatchTakePictureIntent():
     *     Start the Camera app to take a photo.
     */
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (e: IOException) {
                    Log.e(TAG, e.message.toString())
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "org.tensorflow.codelabs.objectdetection.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    /**
     * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>
     *      Draw a box around each objects and show the object's name.
     */
    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>,
        coustomColor: Int
    ): Bitmap? {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        detectionResults.forEach {
            // draw bounding box
            pen.color = coustomColor
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox
            canvas.drawRect(box, pen)

            val tagSize = Rect(0, 0, 0, 0)

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(it.text, 0, it.text.length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F
            canvas.drawText(
                it.text, box.left + margin,
                box.top + tagSize.height().times(1F), pen
            )
        }
        return outputBitmap
    }

/**
 * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>
 *      Draw a box around each objects and show the object's name.
 */
    private fun drawLandmarkResult(
    bitmap: Bitmap,
    landmark: List<FaceLandmark>
    ): Bitmap? {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        landmark.forEach {
            // draw bounding box
            pen.color = Color.GREEN
            pen.strokeWidth = 30F
            pen.style = Paint.Style.STROKE
           canvas.drawPoint(it.position.x, it.position.y, pen)
        }
        pen.color = Color.YELLOW
        pen.strokeWidth = 30F
        pen.style = Paint.Style.STROKE
        var i = 2
        canvas.drawPoint(landmark.get(i).position.x, landmark.get(i).position.y, pen)

        return outputBitmap
    }

    private fun drawNewFace(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
    ): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        detectionResults.forEach {
            // draw bounding box
            pen.color = Color.RED
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox

            canvas.drawRect(box, pen)
        }
        return outputBitmap
    }
}

/**
 * DetectionResult
 *      A class to store the visualization info of a detected object.
 */
data class DetectionResult(val boundingBox: RectF, val text: String)
