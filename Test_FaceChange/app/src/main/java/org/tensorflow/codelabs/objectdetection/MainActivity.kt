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
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val TAG = "TFLite - ODT"
        const val REQUEST_IMAGE_CAPTURE: Int = 1
        private const val MAX_FONT_SIZE = 96F
    }
    private lateinit var captureImageFab: Button
    private lateinit var imageView: ImageView
    private lateinit var changeButton: Button
    private lateinit var changeButton2: Button
    private lateinit var inputImageView: ImageView
    private lateinit var imgSampleOne: ImageView
    private lateinit var imgSampleTwo: ImageView
    private lateinit var imgSampleThree: ImageView
    private lateinit var imgSampleFour: ImageView
    private lateinit var tvPlaceholder: TextView
    private lateinit var currentPhotoPath: String

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        captureImageFab = findViewById(R.id.captureImageFab)
        imageView = findViewById(R.id.imageView)
        changeButton = findViewById(R.id.changeButton)
        changeButton2 = findViewById(R.id.changeButton2)
        inputImageView = findViewById(R.id.imageView)
        imgSampleOne = findViewById(R.id.imgSampleOne)
        imgSampleTwo = findViewById(R.id.imgSampleTwo)
        imgSampleThree = findViewById(R.id.imgSampleThree)
        imgSampleFour = findViewById(R.id.imgSampleFour)
        tvPlaceholder = findViewById(R.id.tvPlaceholder)

        captureImageFab.setOnClickListener(this)
        imageView.setOnTouchListener { view, motionEvent ->
            changeFaceOneByOne(getSampleImage(R.drawable.image2), getSampleImage(R.drawable.image1), motionEvent.x.toInt(), motionEvent.y.toInt())
            return@setOnTouchListener true
        }
        changeButton.setOnClickListener(this)
        changeButton2.setOnClickListener(this)
        imgSampleOne.setOnClickListener(this)
        imgSampleTwo.setOnClickListener(this)
        imgSampleThree.setOnClickListener(this)
        imgSampleFour.setOnClickListener(this)
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
        when (v?.id) {
            R.id.imageView -> {

            }
            R.id.captureImageFab -> {
                try {
                    dispatchTakePictureIntent()
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
            R.id.changeButton -> {
               // changeFace(getSampleImage(R.drawable.image2), getSampleImage(R.drawable.image1), 1500, 700)
                changeFace(getSampleImage(R.drawable.twoface), getSampleImage(R.drawable.twoface2), 500, 700)
            }
            R.id.changeButton2 -> {
                //changeFaceOneByOne(getSampleImage(R.drawable.image2), getSampleImage(R.drawable.image1), 1500, 700)
                changeFaceOneByOne(getSampleImage(R.drawable.twoface), getSampleImage(R.drawable.twoface2), 500, 700)
            }
            R.id.imgSampleOne -> {
                setViewAndDetect(getSampleImage(R.drawable.image1))
            }
            R.id.imgSampleTwo -> {
                setViewAndDetect(getSampleImage(R.drawable.image2))
            }
            R.id.imgSampleThree -> {
                setViewAndDetect(getSampleImage(R.drawable.test5))
            }
            R.id.imgSampleFour -> {
                setViewAndDetect(getSampleImage(R.drawable.test4))
            }

        }
    }

    private fun getObjectDetection(bitmap: Bitmap): List<DetectionResult> {
        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
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
        val resultToDisplay = getObjectDetection(bitmap)
        // Draw the detection result on the bitmap and show it.

        val faceResultToDisplay = getFaceDetection(bitmap)

        val faceDetectionResultImg =
            faceResultToDisplay?.let { drawDetectionResult(bitmap, it, Color.RED) }

        val imgWithResult =
            faceDetectionResultImg?.let { drawDetectionResult(it, resultToDisplay, Color.BLUE) }

        runOnUiThread {
            inputImageView.setImageBitmap(imgWithResult)
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
            Thread.sleep(500)
        }
        return faceResultToDisplay
    }

    private fun getFaceDetectionOneByOne(bitmap: Bitmap): ArrayList<Face>? {
        var faceResultToDisplay: List<DetectionResult>?  = null
        var returnFaces : ArrayList<Face>? = null
        var returnState = false

        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()

        val image = InputImage.fromBitmap(bitmap, 0)
        val detector = FaceDetection.getClient(highAccuracyOpts)
        val result = detector.process(image)
            .addOnSuccessListener { faces ->
                returnFaces = faces as ArrayList<Face>?
                returnState = true
            }
            .addOnFailureListener { e ->
                println("fail")
                returnState = true
            }
        while(!returnState) {
            System.out.println("wait")
            Thread.sleep(500)
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
                for(face in changeResultTODisplay) {
                    if (changeFaceX >= face.boundingBox.left && changeFaceX <= face.boundingBox.right &&
                        changeFaceY >= face.boundingBox.top && changeFaceY <= face.boundingBox.bottom
                    ){
                        changeFaceBoundingBox = face.boundingBox
                        break
                    }
                }
            }

//            val cropImg = cropBitmap(changeImg, changeFaceBoundingBox!!)
//
//            runOnUiThread {
//                val overlayImg = cropImg?.let { overlayBitmap(originalImg, it,
//                    originalFaceBoundingBox!!, 0,0) }
//                inputImageView.setImageBitmap(overlayImg)
//            }

        }
    }

    private fun changeFaceOneByOne(originalImg: Bitmap, changeImg: Bitmap,changeFaceX: Int, changeFaceY: Int) {

        // Run ODT and display result
        // Note that we run this in the background thread to avoid blocking the app UI because
        // TFLite object detection is a synchronised process.
        lifecycleScope.launch(Dispatchers.Default) {
            val originalFacesResult = getFaceDetectionOneByOne(originalImg)
            val changeFacesResult = getFaceDetectionOneByOne(changeImg)

            var originalFaceLandmarks : List<FaceLandmark>?  = null
            var changeFaceLandmarks : List<FaceLandmark>?  = null

            var originalFaceBoundingBox : RectF? = null
            var changeFaceBoundingBox : RectF? = null
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

            if (changeFacesResult != null) {

                for (face in changeFacesResult) {
                    if (changeFaceX >= face.boundingBox.left && changeFaceX <= face.boundingBox.right &&
                        changeFaceY >= face.boundingBox.top && changeFaceY <= face.boundingBox.bottom
                    ) {
                        changeFaceBoundingBox = RectF(face.boundingBox)
                        changeFaceLandmarks = face.allLandmarks
                        break
                    }
                }
            }

//            val imgWithResult =
//                originalImg?.let { drawLandmarkResult(it, changeFaceLandmarks!!) }
//
//            runOnUiThread {
//                inputImageView.setImageBitmap(imgWithResult)
//            }

            val addStartY =  ((changeFaceLandmarks!!.get(0).position.y - changeFaceLandmarks.get(3).position.y )/2).toInt()
            val addEndY =  ((changeFaceLandmarks.get(0).position.y - changeFaceLandmarks.get(5).position.y )/2).toInt()

            val cropImgRect = Rect(
                changeFaceLandmarks.get(2).position.x.toInt(), // left
                changeFaceLandmarks.get(3).position.y.toInt() - addStartY, // top
                changeFaceLandmarks.get(7).position.x.toInt(), // right
                changeFaceLandmarks.get(0).position.y.toInt() + addEndY  // bottom
            )

            if (originalFacesResult != null || changeFacesResult != null) {
                val cropImg = cropBitmap(changeImg, changeFaceBoundingBox!!, cropImgRect)

                runOnUiThread {
                    val overlayImg = cropImg?.let {
                        overlayBitmap(
                            originalImg, it,
                            originalFaceBoundingBox!!,
                            (originalFaceLandmarks!!.get(2).position.x).toInt(),
                            (originalFaceLandmarks.get(3).position.y - addStartY).toInt()
                        )
                    }
                    inputImageView.setImageBitmap(overlayImg)
                }
            }

        }
    }

    private fun cropBitmap(original: Bitmap, rect:RectF, optimizationRect: Rect): Bitmap? {

        val width = (optimizationRect.right - optimizationRect.left)
        val height = optimizationRect.bottom - optimizationRect.top
        var startX = optimizationRect.left
        var startY = optimizationRect.top
        if (rect.left.toInt() < 0) {
            startX = 0
        }
        if (rect.top.toInt() < 0) {
            startY = 0
        }

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
