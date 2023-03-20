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
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter
import org.tensorflow.lite.task.vision.segmenter.OutputType
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
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
    private lateinit var inputImageView: ImageView
    private lateinit var imgSampleOne: ImageView
    private lateinit var imgSampleTwo: ImageView
    private lateinit var imgSampleThree: ImageView
    private lateinit var imgSampleFour: ImageView
    private lateinit var tvPlaceholder: TextView
    private lateinit var errorMessage: TextView
    private lateinit var currentPhotoPath: String

    private var imageSegmenter: ImageSegmenter? = null
    private var objectDetector: ObjectDetector? = null

    private var mContext: Context? = null

    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        setContentView(R.layout.activity_main)

        captureImageFab = findViewById(R.id.captureImageFab)
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
            runSegmentation(drawable.toBitmap(), point.x, point.y)
            runOnUiThread {
                errorMessage.setText("")
            }
            return@setOnTouchListener true
        }
        captureImageFab.setOnClickListener(this)
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
            R.id.captureImageFab -> {
                try {
                    dispatchTakePictureIntent()
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
            R.id.imgSampleOne -> {
                //setViewAndDetect(getSampleImage(R.drawable.image1))
                runOnUiThread {
                    inputImageView.setImageBitmap(getSampleImage(R.drawable.image1))
                }
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


    private fun getBitmapClickPoint( clickPoint: PointF, imageView: ImageView) : Point {

        val bitmap:Bitmap = imageView.drawable.toBitmap()

        // imageView width, height 가져오기
        val viewWidth = imageView.width
        val viewHeight = imageView.height

        // 실제 이미지일 때 포인트 위치
//        val newPointX = clickPoint.x  * (bitmap.width/viewWidth)
//        val newPointY =clickPoint.y * (bitmap.height/viewHeight)
        val newPointX = (bitmap.width * clickPoint.x)  / viewWidth
        val newPointY =(bitmap.height * clickPoint.y)  / viewHeight

        return Point(newPointX.toInt(), newPointY.toInt())
    }

    fun clearImageSegmenter() {
        imageSegmenter = null
    }

    fun setResults(
        segmentResult: List<Segmentation>?,
        imageHeight: Int,
        imageWidth: Int
    ): Bitmap? {
        if (segmentResult != null && segmentResult.isNotEmpty()) {
            val colorLabels = segmentResult[0].coloredLabels.mapIndexed { index, coloredLabel ->
                ColorLabel(
                    index,
                    coloredLabel.getlabel(),
                    coloredLabel.argb
                )
            }

            // Create the mask bitmap with colors and the set of detected labels.
            // We only need the first mask for this sample because we are using
            // the OutputType CATEGORY_MASK, which only provides a single mask.
            val maskTensor = segmentResult[0].masks[0]
            val maskArray = maskTensor.buffer.array()
            val pixels = IntArray(maskArray.size)

            for (i in maskArray.indices) {
                // Set isExist flag to true if any pixel contains this color.
                val colorLabel = colorLabels[maskArray[i].toInt()].apply {
                    isExist = true
                }
                val color = colorLabel.getColor()
                pixels[i] = color
            }

            val image = Bitmap.createBitmap(
                pixels,
                maskTensor.width,
                maskTensor.height,
                Bitmap.Config.ARGB_8888
            )

            // PreviewView is in FILL_START mode. So we need to scale up the bounding
            // box to match with the size that the captured images will be displayed.

            var scaleBitmap = Bitmap.createScaledBitmap(image, imageWidth, imageHeight, false)

            return scaleBitmap
        }
        return null
    }

    data class ColorLabel(
        val id: Int,
        val label: String,
        val rgbColor: Int,
        var isExist: Boolean = false
    ) {

        fun getColor(): Int {
            // Use completely transparent for the background color.
        return if (id == 0) Color.BLACK else Color.WHITE
        }
    }

    private fun setupImageSegmenter() {
        // Create the base options for the segment
        val optionsBuilder =
            ImageSegmenter.ImageSegmenterOptions.builder()

        /*
        // Set general segmentation options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(2)
        // Use the specified hardware for running the model. Default to CPU
        baseOptionsBuilder.useNnapi()
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())
        */

        /*
        CATEGORY_MASK is being specifically used to predict the available objects
        based on individual pixels in this sample. The other option available for
        OutputType, CONFIDENCE_MASK, provides a gray scale mapping of the image
        where each pixel has a confidence score applied to it from 0.0f to 1.0f
         */

        /* Segmentation option 및 디렉터 설정 */
        optionsBuilder.setOutputType(OutputType.CATEGORY_MASK)
        try {
            imageSegmenter =
                ImageSegmenter.createFromFileAndOptions(
                    mContext,
                    "lite-model_deeplabv3_1_metadata_2.tflite",
                    optionsBuilder.build()
                )
        } catch (e: IllegalStateException) {
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }

        /* Object Detection option 및 디렉터 설정 */
        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.3f)
            .build()
        objectDetector = ObjectDetector.createFromFileAndOptions(
            this,
            "model.tflite",
            options
        )
    }


    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     */
    private fun runSegmentation(bitmap: Bitmap, changeFaceX: Int, changeFaceY: Int) {
        val copyBitmap: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val image = TensorImage.fromBitmap(bitmap)

        // 모델 전처리 함수 (디렉터 제작)
        setupImageSegmenter()

        // objectDetection 모델 실행
        val results = objectDetector!!.detect(image)

        // 추출할 boundingBox 알애내기
        var testObjectRect : RectF? = null
        if (results != null) {
            for (it in results) {
                if (changeFaceX >= it.boundingBox.left && changeFaceX <= it.boundingBox.right &&
                    changeFaceY >= it.boundingBox.top && changeFaceY <= it.boundingBox.bottom
                ) {
                    testObjectRect = it.boundingBox
                    break
                }
            }

        }
        if(testObjectRect == null) {
            runOnUiThread {
                errorMessage.setText("해당 포인트에는 객체가 존재하지 않음")
            }
            return
        }
        // bitmap 자르고 tensor 이미지로 변환
        val testObjectBitmap = cropBitmap(bitmap, testObjectRect!!)
        val testImage = TensorImage.fromBitmap(testObjectBitmap)

        // segmnetatin 모델 결과
        val segmentResult = imageSegmenter?.segment(testImage)

        if(results.size == 0)
            return
        // segmentatin result 결과 값 알아내기
        val segmentationResultBitmap = setResults(
            segmentResult,
            testImage.height,
            testImage.width
        )

        //  비트맵 합치기
        val resultBitmap = overlayBitmap(copyBitmap, segmentationResultBitmap!!,testObjectRect.left.toInt(), testObjectRect.top.toInt() )
        runOnUiThread {
            inputImageView.setImageBitmap(resultBitmap)
        }
        // Draw the detection result on the bitmap and show it.
    }


    private fun cropBitmap(original: Bitmap, rect:RectF): Bitmap? {
        val width = (rect.right - rect.left).toInt()
        val height = (rect.bottom - rect.top).toInt()
        var startX = rect.left.toInt()
        var startY = rect.top.toInt()
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
    fun overlayBitmap(original: Bitmap, add: Bitmap, optimizationX:Int, optimizationY:Int): Bitmap? {

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
        val paint = Paint(Color.BLACK)
        canvas.drawRect(Rect(0,0,original.width, original.height),paint)
        canvas.drawBitmap(add, startX.toFloat(), startY.toFloat(), null)

        return resultOverlayBmp
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
        lifecycleScope.launch(Dispatchers.Default) { runSegmentation(bitmap,0,0) }
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
}