package com.example.test_cinemagraph

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toRect
import com.example.test_cinemagraph.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.lang.reflect.Array.get

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var testDrawble : ArrayList<Int> = arrayListOf()
    private val ImageAttribute = "Cinemagraphs"
    val boundingBox: ArrayList<List<Int>> = arrayListOf()
    private val cropBitmapList : ArrayList<Bitmap> = arrayListOf()
    private val objectClassification: ArrayList<String> = arrayListOf()
    private var currentObject = ""
    private lateinit var context: Context
    private lateinit var mainBitmap: Bitmap

    private var resultToDisplay: List<DetectionResult>? = null

    companion object {
        private const val MAX_FONT_SIZE = 50F
        const val BITMAP_RESIZE = 500
    }

    private lateinit var imageView: ImageView
    private lateinit var editButton: Button
    private lateinit var viewButton: Button

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        testDrawble = arrayListOf(R.drawable.auto_rewind_best, R.drawable.auto_rewind1,
            R.drawable.auto_rewind2, R.drawable.auto_rewind3,
            R.drawable.auto_rewind4, R.drawable.auto_rewind5)

        imageView = binding.imageView
        editButton = binding.editButton
        viewButton = binding.viewButton

        imageView.setOnTouchListener { view, motionEvent ->

           if(motionEvent.action == MotionEvent.ACTION_DOWN) {
               // click 좌표를 bitmap에 해당하는 좌표로 변환
               val point = getBitmapClickPoint(PointF(motionEvent.x, motionEvent.y), imageView)

               println("point::" + point)

               getBoundingBox(point)
           }
            return@setOnTouchListener true
        }
        editButton.setOnClickListener {
            runObjectDetection(getSampleImage(testDrawble[0]))
        }
        viewButton.setOnClickListener {

        }


        mainBitmap = getSampleImage(testDrawble[0])
        //mainBitmap = Bitmap.createScaledBitmap(mainBitmap, BITMAP_RESIZE, mainBitmap.height/(mainBitmap.width/BITMAP_RESIZE), true)

        runOnUiThread {
            imageView.setImageBitmap(mainBitmap)
        }

        context = this

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

    private fun getBoundingBox(point: Point) {
        boundingBox.clear()
        binding.cropImageLayout.removeAllViews()
        CoroutineScope(Dispatchers.Main).launch {
            for (i in 0 until testDrawble.size) {
                println("111111: " + i)
                var bitmap = getSampleImage(testDrawble[i])
                //bitmap = Bitmap.createScaledBitmap(bitmap, BITMAP_RESIZE, bitmap.height/(bitmap.width/BITMAP_RESIZE), true)

                getObjectBoundingBoxList(bitmap, point.x, point.y)
            }
            println("=======================================================")
            for (i in 0 until boundingBox.size)
                println(i.toString() + " || " + boundingBox[i])
        }
    }

    private fun getBitmapClickPoint( clickPoint: PointF, imageView: ImageView) : Point {

        var bitmap:Bitmap = imageView.drawable.toBitmap()
        //bitmap = Bitmap.createScaledBitmap(bitmap, 1080, 810, true)

        // imageView width, height 가져오기
        val viewWidth = imageView.width
        val viewHeight = imageView.height

        // 실제 이미지일 때 포인트 위치
        val newPointX = (bitmap.width * clickPoint.x)  / viewWidth
        val newPointY =(bitmap.height * clickPoint.y)  / viewHeight

        return Point(newPointX.toInt(), newPointY.toInt())
    }

    private fun cinemagraphRun(bitmapList: ArrayList<Int>) {

    }

    private fun getObjectBoundingBoxList(bitmap: Bitmap, changeFaceX: Int, changeFaceY: Int) {
        // Run ODT and display result
        // Note that we run this in the background thread to avoid blocking the app UI because
        // TFLite object detection is a synchronised process.
        val detectionResult = getObjectDetection(bitmap)

        if (detectionResult != null) {
            for (i in 0 until detectionResult.size) {
                var  it = detectionResult[i]

                // 겹치는 다른 객체가 추출되는 것을 방지하기 위해
                if(i == 0) {
                    
                }

                if (currentObject.equals("") || currentObject.equals(it.text)) {
                    println("[i] x: " + it.boundingBox.left + " ~ " + it.boundingBox.right)
                    println("[i] y: " + it.boundingBox.top + " ~ " + it.boundingBox.bottom)
                    if (changeFaceX >= it.boundingBox.left && changeFaceX <= it.boundingBox.right &&
                        changeFaceY >= it.boundingBox.top && changeFaceY <= it.boundingBox.bottom) {
                        val arrayBounding = listOf(
                            i,
                            it.boundingBox.left.toInt(),
                            it.boundingBox.top.toInt(),
                            it.boundingBox.right.toInt(),
                            it.boundingBox.bottom.toInt()
                        )
                        println("result I: " + i)
                        boundingBox.add(arrayBounding)

                        // 넣고자 하는 layout 불러오기
                        val cropLayout = layoutInflater.inflate(R.layout.crop_image_array, null)

                        // 위 불러온 layout에서 변경을 할 view가져오기
                        val cropImageView: ImageView =
                            cropLayout.findViewById(R.id.cropImageView)

                        // bitmap를 잘라 위에 불러온 view에 bitmap 삽입
                        val cropImage = cropBitmap(bitmap, it.boundingBox.toRect())
                        cropImageView.setImageBitmap(cropImage)

                        // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                        binding.cropImageLayout.addView(cropLayout)

                        break
                    }
                }
            }
        }
    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     */
    private fun runObjectDetection(bitmap: Bitmap ) {

        // Object Detection
        resultToDisplay = getObjectDetection(bitmap)

        // ObjectDetection 결과(bindingBox) 그리기
        val objectDetectionResult =
            drawDetectionResult(bitmap, resultToDisplay!!, Color.BLUE)

        //추가할 커스텀 레이아웃 가져오기
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater


        for (i in 0 until resultToDisplay!!.size) {
            val text = resultToDisplay!![i].text
            if (!objectClassification.contains(text)) {
                objectClassification.add(text)

                val customLayout = layoutInflater.inflate(R.layout.object_classification, null)

                val classificationBtn: Button =
                    customLayout.findViewById<Button>(R.id.classificationBtn)
                classificationBtn.text = text

                classificationBtn.setOnClickListener {
                    currentObject = text
                    val objectDetectionResult =
                        drawDetectionResult(getSampleImage(testDrawble[0]), resultToDisplay!!, Color.BLUE)

                    runOnUiThread {
                        imageView.setImageBitmap(objectDetectionResult)
                    }
                }

                binding.classificationLayout.addView(customLayout)
            }
        }

        runOnUiThread {
            imageView.setImageBitmap(objectDetectionResult)
        }
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
            val text = "${category.label}"

            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, text)
        }
        return resultToDisplay
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
            if(currentObject.equals("") || currentObject.equals(it.text)) {
                // draw bounding box
                pen.color = Color.parseColor("#B8C5BB")
                pen.strokeWidth = floatToDp(10F).toFloat()
                pen.style = Paint.Style.STROKE
                val box = it.boundingBox
                canvas.drawRoundRect(box, floatToDp(10F).toFloat(), floatToDp(10F).toFloat(), pen)

                val tagSize = Rect(0, 0, 0, 0)
            }
        }
        return outputBitmap
    }

    private fun floatToDp(f : Float):Int {
        return (f * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
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
        canvas.drawBitmap(add, startX.toFloat(), startY.toFloat(), null)

        return resultOverlayBmp
    }

}

data class DetectionResult(val boundingBox: RectF, val text: String)
