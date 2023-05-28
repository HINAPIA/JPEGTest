package com.example.test_cinemagraph

import android.R.attr
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toRect
import com.example.test_cinemagraph.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector


class MainActivity : AppCompatActivity() {



    private lateinit var binding: ActivityMainBinding
    private lateinit var context: Context

    private var resultToDisplay: List<DetectionResult>? = null
    private lateinit var faceDetector: FaceDetector
    private lateinit var customObjectDetector: ObjectDetector
    private lateinit var FaceresultToDisplay: ArrayList<Face>

    private val ImageAttribute = "Cinemagraphs"
    private var testDrawble: ArrayList<Int> = arrayListOf()
    private var bitmapArray: ArrayList<Bitmap> = arrayListOf()
    private val boundingBox: ArrayList<List<Int>> = arrayListOf()

    private val ovelapBitmap: ArrayList<Bitmap> = arrayListOf()

    private lateinit var mainBitmap: Bitmap
    private val cropBitmapList: ArrayList<Bitmap> = arrayListOf()
    private val objectClassification: ArrayList<String> = arrayListOf()
    private var currentObject = ""

    private var touchPoint: Point = Point(0,0)

    private lateinit var imageView: ImageView
    private lateinit var editButton: Button
    private lateinit var viewButton: Button

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setDetecter()

        context = this

        // test 하고자 하는 Image
        testDrawble = arrayListOf(
            R.drawable.auto_rewind_best, R.drawable.auto_rewind1,
            R.drawable.auto_rewind2, R.drawable.auto_rewind3,
            R.drawable.auto_rewind4, R.drawable.auto_rewind5
        )

        for(i in 0 until testDrawble.size){
            bitmapArray.add(getSampleImage(testDrawble[i]))
        }

        // 가장 앞에 사진을 main 사진으로 지정
        mainBitmap = bitmapArray[0]

        imageView = binding.imageView
        editButton = binding.editButton
        viewButton = binding.viewButton

        // imageview를 클릭했을 때: 클릭된 좌표(point)에 Object가 존재하는지 확인 (Object Detection boundingBox 결과와 비교)
        imageView.setOnTouchListener { view, motionEvent ->

            // 마우스 event action 중 action_down만 객체와 비교를 실행 -> WHY? 제한을 안두면 해당 touchListener가 세번 불러지기 때문에
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                // click 좌표를 bitmap에 해당하는 좌표로 변환
                touchPoint = getBitmapClickPoint(PointF(motionEvent.x, motionEvent.y), imageView)
                println("------- click point:" + touchPoint)

                setEditObjectForPoint()   // 포인트 값을 통해 객체 편집을 도와주는 함수 호출
            }
            return@setOnTouchListener true
        }
        // editButton을 클릭했을 때: edit하는 것, 즉 magic picture을 제작하고자 함
        editButton.setOnClickListener {
//            runFaceDetection(mainBitmap)
            runObjectDetection(mainBitmap) // Main Picture 객체 감지(Object Detection) 실행
        }
        // viewButton을 클릭했을 때: magic pricture 실행 (움직이게 하기)
        viewButton.setOnClickListener {
            cinemagraphRun(bitmapArray, boundingBox)
        }

        // 화면에 image 띄우기
        runOnUiThread {
            imageView.setImageBitmap(mainBitmap)
        }
    }

    private fun cinemagraphRun(bitmapList: ArrayList<Bitmap>, boundingBox: ArrayList<List<Int>>) {
        CoroutineScope(Dispatchers.Main).launch {
            var boundingIndex = 0
            for (i in 0 until bitmapList.size) {
                if(boundingBox.size <= boundingIndex)
                    break
                val box = boundingBox[boundingIndex]
                if (box[0] == i) {
                    val original = bitmapArray[0].copy(Bitmap.Config.ARGB_8888, true)
                    val rect = Rect(box[1], box[2], box[3], box[4])
                    val cropImg =
                        cropBitmap(bitmapArray[i].copy(Bitmap.Config.ARGB_8888, true), rect)
                    ovelapBitmap.add(
                        overlayBitmap(original, cropImg!!, boundingBox[boundingIndex][1], boundingBox[0][2])!!
                    )
                    boundingIndex++
                }
            }

            val handler = Handler()
            var currentImageIndex = 0

            val runnable = object : Runnable {
                override fun run() {
                    imageView.setImageBitmap(ovelapBitmap[currentImageIndex])
                    currentImageIndex++
                    if (currentImageIndex >= ovelapBitmap.size) {
                        currentImageIndex = 0
                    }
                    handler.postDelayed(this, 300)
                }
            }
            handler.postDelayed(runnable, 300,)
            CoroutineScope(Dispatchers.Default).launch {
                Thread.sleep(10000)
                handler.removeMessages(0)
            }
        }

    }


    /**
     * getSampleImage():
     *      drawble 이미지를 비트맵으로 변환해 반환한다.
     */
    private fun getSampleImage(drawable: Int): Bitmap {
        return BitmapFactory.decodeResource(resources, drawable, BitmapFactory.Options().apply {
            inMutable = true
        })
    }

    /**
     * getBitmapClickPoint( clickPoint: PointF, imageView: ImageView):
     *      imageView에 clickEvent가 발생했을 때,
     *      clickPoint를 사진 Bitmap에 맞춰 비율 조정을 해서
     *      비율 보정된 새로운 point를 반환한다.
     */
    private fun getBitmapClickPoint(clickPoint: PointF, imageView: ImageView): Point {

        var bitmap: Bitmap = imageView.drawable.toBitmap()
        //bitmap = Bitmap.createScaledBitmap(bitmap, 1080, 810, true)

        // imageView width, height 가져오기
        val viewWidth = imageView.width
        val viewHeight = imageView.height

        // 실제 이미지일 때 포인트 위치
        val newPointX = (bitmap.width * clickPoint.x) / viewWidth
        val newPointY = (bitmap.height * clickPoint.y) / viewHeight

        // 수정된 이미지 포인트
        return Point(newPointX.toInt(), newPointY.toInt())
    }

    /**
     * getBoundingBox(point: Point):
     *     click된 포인트를 알려주면,
     *     해당 포인트가 객체 감지 결과 bounding Box 속에 존재하는지 찾아서
     *     만약 포인트를 포함하는 boundingBox를 찾으면 boundingBox 라는 ArrayList에 추가한다.
     *     추가적으로 이렇게 추출된 boundingBox는 crop하여 사진 아래에 보여준다
     */
    private fun setEditObjectForPoint() {
        // 사용할 변수 값 초기화
        boundingBox.clear() // boundingBox ArrayList 비우기
        binding.cropImageLayout.removeAllViews() // view에 존재하는 crop된 이미지 띄운거 지우기

        CoroutineScope(Dispatchers.Main).launch {
            // boundingBox를 알아내고 이미지 크롭하는 함수 호출
            setStandardBoundingBox()
//            getFaceBoundingBox()
        }
    }

    /**
     * checkPointInRect(point: Point, rect: Rect):
     *          point 가 rect 안에 존재하는 지를 알아낸다.
     *          존재하면 true 존재하지 않으면 false
     */
    private fun checkPointInRect(point: Point, rect: Rect): Boolean {
        // 포인트가 boundingBox 안에 존재할 경우에만 실행
        // -> boundinBox.left <= x <= boundingBox.right && boundingBox.top <= y <= boundingBox.bottom
        return point.x >= rect.left && point.x <= rect.right &&
                point.y >= rect.top && point.y <= rect.bottom
    }

    /**
     * compareRectAndRect(originalRect: Rect, rect: Rect):
     *          두개의 rect를 받아 두 rect 가 동일한 위치의 객체인지 비교한다.
     *          동일할 경우 true 아닐 경우 false
     */
    private fun compareRectAndRect(originalRect: Rect, rect: Rect) : Boolean {
        val range = 100

        val originalW = originalRect.right - originalRect.left
        val originalH = originalRect.bottom - originalRect.top
        val w = rect.right - rect.left
        val h = rect.bottom - rect.top

        if(originalW - range <= w && w <= originalW + range &&
            originalH - range <= h && h <= originalH + range)
            return true

        return false
    }

    /**
     *  cropImgListAddAndView(cropImage: Bitmap):
     *           이미지를 자르고 화면에 띄어준다.
     */
    private fun cropImgListAddAndView() {
        // 감지된 모든 boundingBox 출력
        println("=======================================================")
        for (i in 0 until boundingBox.size) {
            println(i.toString() + " || " + boundingBox[i])

            // bounding rect 알아내기
            val rect = boundingBox[i]
            // bitmap를 자르기
            val cropImage = cropBitmap(
                bitmapArray[i].copy(Bitmap.Config.ARGB_8888, true),
                Rect(rect[1], rect[2], rect[3], rect[4])
            )
            // 크롭이미지 배열에 값 추가
            cropBitmapList.add(cropImage!!)

            // 넣고자 하는 layout 불러오기
            val cropLayout = layoutInflater.inflate(R.layout.crop_image_array, null)

            // 위 불러온 layout에서 변경을 할 view가져오기
            val cropImageView: ImageView =
                cropLayout.findViewById(R.id.cropImageView)

            // 자른 사진 이미지뷰에 붙이기
            cropImageView.setImageBitmap(cropImage)

            // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
            binding.cropImageLayout.addView(cropLayout)
        }
    }

    /**
     * setStandardBoundingBox():
     *        기준이 되는 사진(메인 사진)을 분석하고, 포인트가 주어졌을 때 포인트에 잡히는 바운딩 박스를 알아낸다.
     *        (객체가 포인트에 존재하는지 확인)
     *        겹친 바운딩 박스가 2개 이상일 경우, 다이얼로그로 사용자에게 선택한 사진이 맞는지 확인 시킨다.
     *        1개일 경우, 해당 boundingBox를 기준으로 하고 남은 사진을 분석한다.
     */
    private fun setStandardBoundingBox() {
        var standardRect: Rect = Rect(-1, 0, -1, 0)

        var bitmap = bitmapArray[0]

        // Object Detection을 통한 결과 값을 얻을 수 있는 함수 호출
        val detectionResult = getObjectDetection(bitmap)

        // image[0]에서 클릭된 좌표에 존재하는 겹쳐진 바운딩 박스를 모은 박스 리스트
        val overlapBoundingBoxList = arrayListOf<List<Int>>()

        for (i in 0 until detectionResult.size) {
            val it = detectionResult[i]

            // 해당 객체가 구분이 되었을 때, 해당 객체의 구분자(category)와 비교해 같을 경우만 실행
            if (currentObject.equals("") || currentObject.equals(it.text)) {

                println("[i] x: " + it.boundingBox.left + " ~ " + it.boundingBox.right)
                println("[i] y: " + it.boundingBox.top + " ~ " + it.boundingBox.bottom)

                // 포인트가 boundingBox 안에 존재할 경우에만 실행
                if (checkPointInRect(touchPoint, it.boundingBox.toRect())) {
                    print("currentObject: " + it.text)

                    // index, boundingBox를 list로 담기
                    val arrayBounding = listOf(
                        0,
                        it.boundingBox.left.toInt(),
                        it.boundingBox.top.toInt(),
                        it.boundingBox.right.toInt(),
                        it.boundingBox.bottom.toInt()
                    )
                    println("result I: " + i)

                    // 겹치는 boundingBoxList에 추가
                    overlapBoundingBoxList.add(arrayBounding)
                }

            }
        }

        // index가 0일 경우,
        // overlapBoundingBoxList의 크기가 2 이상일 경우,
        if (overlapBoundingBoxList.size >= 2) {
            // 사용자에게 어떤 사진 추출을 원하는지 질문
            imgDialogToCheckImg(bitmap, overlapBoundingBoxList, 0)
        }

        // overlapBoundingBoxList의 크기가 1일 경우
        else {
            val array = overlapBoundingBoxList[0]
            standardRect = Rect(array[1], array[2], array[3], array[4])
            boundingBox.add(array)
            getboundingBox(standardRect)
        }
    }

    /**
     * getboundingBox(standardRect: Rect):
     *      전달된 bitmap과 x,y 값을 통해 해당 bitmap에 감지된 객체
     *      bounding Box 속에 (x, y) 좌표가 존재하는지 확인한다.
     */
    private fun getboundingBox(standardRect: Rect) {

        for (j in 1 until bitmapArray.size) {
            var bitmap = bitmapArray[j]

            // Object Detection을 통한 결과 값을 얻을 수 있는 함수 호출
            val detectionResult = getObjectDetection(bitmap)

            // image[0]에서 클릭된 좌표에 존재하는 겹쳐진 바운딩 박스를 모은 박스 리스트
            val overlapBoundingBoxList = arrayListOf<List<Int>>()

            for (i in 0 until detectionResult.size) {
                val it = detectionResult[i]

                // 해당 객체가 구분이 되었을 때, 해당 객체의 구분자(category)와 비교해 같을 경우만 실행
                if (currentObject.equals("") || currentObject.equals(it.text)) {

                    println("[i] x: " + it.boundingBox.left + " ~ " + it.boundingBox.right)
                    println("[i] y: " + it.boundingBox.top + " ~ " + it.boundingBox.bottom)

                    // 포인트가 boundingBox 안에 존재할 경우에만 실행
                    if (checkPointInRect(touchPoint, it.boundingBox.toRect())) {
                        print("currentObject: " + it.text)

                        // index, boundingBox를 list로 담기
                        val arrayBounding = listOf(
                            j,
                            it.boundingBox.left.toInt(),
                            it.boundingBox.top.toInt(),
                            it.boundingBox.right.toInt(),
                            it.boundingBox.bottom.toInt()
                        )
                        println("result I: " + i)

                        if(compareRectAndRect(standardRect, it.boundingBox.toRect())) {
                            // boundingBox에 추가
                            boundingBox.add(arrayBounding)

                            break
                        }

                    }
                }
            }
        }
        cropImgListAddAndView()
    }

    /**
     * getFaceBoundingBox():
     *      전달된 bitmap과 x,y 값을 통해 해당 bitmap에 감지된 객체
     *      bounding Box 속에 (x, y) 좌표가 존재하는지 확인한다.
     */
    private fun getFaceBoundingBox() {

        for (j in 0 until bitmapArray.size) {
            var bitmap = bitmapArray[j]

            // Object Detection을 통한 결과 값을 얻을 수 있는 함수 호출
            val detectionResult = getFaceDetectionOneByOne(bitmap)

            // image[0]에서 클릭된 좌표에 존재하는 겹쳐진 바운딩 박스를 모은 박스 리스트
            val overlapBoundingBoxList = arrayListOf<List<Int>>()

            for (i in 0 until detectionResult!!.size) {
                val it = detectionResult[i]

                println("[i] x: " + it.boundingBox.left + " ~ " + it.boundingBox.right)
                println("[i] y: " + it.boundingBox.top + " ~ " + it.boundingBox.bottom)

                // 포인트가 boundingBox 안에 존재할 경우에만 실행
                if (checkPointInRect(touchPoint, it.boundingBox)) {

                    // index, boundingBox를 list로 담기
                    val arrayBounding = listOf(
                        j,
                        it.boundingBox.left.toInt(),
                        it.boundingBox.top.toInt(),
                        it.boundingBox.right.toInt(),
                        it.boundingBox.bottom.toInt()
                    )
                    println("result I: " + i)

                    // 포인트가 boundingBox 안에 존재할 경우에만 실행
                    if (checkPointInRect(touchPoint, it.boundingBox)) {
                        // boundingBox에 추가
                        boundingBox.add(arrayBounding)

                        break
                    }
                }
            }
        }
        cropImgListAddAndView()
    }


    /**
     * imgDialogToCheckImg(bitmap: Bitmap, overlapBoundingBoxList: ArrayList<List<Int>>, index: Int):
     *          사용자에게 다이얼로그를 띄어 사진을 확인하고,
     *              사용자가 OK버튼을 누르면 선택한 boundingBox를 기준으로 하고 남은 사진을 분석한다.
     *              사용자가 NO버튼을 누르면 다음 boundingBox를 보여준다.
     */
    private fun imgDialogToCheckImg(bitmap: Bitmap, overlapBoundingBoxList: ArrayList<List<Int>>, index: Int) {
        var standardRect: Rect = Rect(-1,0,-1,0)

        val copyBitmap: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val overlap = overlapBoundingBoxList[index]
        val rect = Rect(overlap[1],overlap[2],overlap[3],overlap[4])
        val newBitmap = cropBitmap(copyBitmap, rect)
        val dlg = checkSelectDialog(this, newBitmap!!)

        dlg.listener =
            object : checkSelectDialog.LessonDialogClickedListener {
                override fun onOkClicked() {
                    standardRect = rect
                    boundingBox.add(overlap)
                    getboundingBox(standardRect)
                }

                override fun onNoCliecked() {
                    if(overlapBoundingBoxList.size > index)
                         imgDialogToCheckImg(bitmap, overlapBoundingBoxList, index+1)

                    getboundingBox(standardRect)
                }
            }
        dlg.start()

    }

    /**
     * checkSelectDialog(context: Context, bitmap: Bitmap)
     *      사용자가 선택한 객체가 맞는지 확인하는 다이얼로그
     */
    class checkSelectDialog(context: Context, bitmap: Bitmap) {

        lateinit var listener: LessonDialogClickedListener
        lateinit var noBtn: Button
        lateinit var okBtn: Button
        lateinit var imageView: ImageView
        val bitmap: Bitmap = bitmap
        interface LessonDialogClickedListener {
            fun onOkClicked()
            fun onNoCliecked()
        }

        private val dlg = Dialog(context)

        fun start() {

            dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
            /*커스텀 다이얼로그 radius 적용*/
            dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            dlg.setContentView(R.layout.check_select_img_dialog)

            imageView = dlg.findViewById(R.id.selectImageView)
            imageView.setImageBitmap(bitmap)

            // ok 버튼을 눌렀을 때
            okBtn = dlg.findViewById(R.id.dialogOkBtn)
            okBtn.setOnClickListener {
                listener.onOkClicked()
                dlg.dismiss()
            }

            // no 버튼을 눌렀을 때
            noBtn = dlg.findViewById(R.id.dialogNoBtn)
            noBtn.setOnClickListener {
                listener.onNoCliecked()
                dlg.dismiss()
            }
            dlg.show()
        }
    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     *      사진 속 객체를 감지하고, 감지된 객체에 boundingBox와 category를 적고 화면에 띄운다.
     *      또한, category를 classification List에 추가해준다. (분류에 도움을 줌)
     */
    private fun runObjectDetection(bitmap: Bitmap) {

        // Object Detection
        resultToDisplay = getObjectDetection(bitmap)

        // ObjectDetection 결과(bindingBox) 그리기
        val objectDetectionResult =
            drawDetectionResult(bitmap, resultToDisplay!!)

        //추가할 커스텀 레이아웃 가져오기
        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // 사진에서 감지된 객체 개수만큼 반복 문
        for (i in 0 until resultToDisplay!!.size) {
            // category를 text로 설정
            val text = resultToDisplay!![i].text

            // objectClassification List에 text가 존재하지 않을 경우 실행
            if (!objectClassification.contains(text)) {
                // objectClassification List에 text 추가
                objectClassification.add(text)

                // 넣고자 하는 layout 불러오기
                val customLayout = layoutInflater.inflate(R.layout.object_classification, null)

                // 위 불러온 layout에서 변경을 할 view 가져오기
                val classificationBtn: Button =
                    customLayout.findViewById<Button>(R.id.classificationBtn)

                // text 설정 및 onclicklistener를 설정해준다
                classificationBtn.text = text
                classificationBtn.setOnClickListener {
                    // 현재 설정된 currentObject 값 설정
                    currentObject = text

                    // ObjectDetection 결과(bindingBox) 그리기
                    val objectDetectionResult =
                        drawDetectionResult(bitmapArray[0], resultToDisplay!!)

                    // 화면에 이미지 띄우기
                    runOnUiThread {
                        imageView.setImageBitmap(objectDetectionResult)
                    }
                }

                // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                binding.classificationLayout.addView(customLayout)
            }
        }

        // 화면에 이미지 띄우기
        runOnUiThread {
            imageView.setImageBitmap(objectDetectionResult)
        }
    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     *      사진 속 객체를 감지하고, 감지된 객체에 boundingBox와 category를 적고 화면에 띄운다.
     *      또한, category를 classification List에 추가해준다. (분류에 도움을 줌)
     */
//    private fun runFaceDetection(bitmap: Bitmap) {
//
//        // Object Detection
//        FaceresultToDisplay = getFaceDetectionOneByOne(bitmap)!!
//
//        // ObjectDetection 결과(bindingBox) 그리기
//        val objectDetectionResult =
//            drawDetectionResult(bitmap, FaceresultToDisplay)
//
//        //추가할 커스텀 레이아웃 가져오기
//        val layoutInflater =
//            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//
//        // 사진에서 감지된 객체 개수만큼 반복 문
//        for (i in 0 until FaceresultToDisplay.size) {
//
//                // 넣고자 하는 layout 불러오기
//                val customLayout = layoutInflater.inflate(R.layout.object_classification, null)
//
//                // 위 불러온 layout에서 변경을 할 view 가져오기
//                val classificationBtn: Button =
//                    customLayout.findViewById<Button>(R.id.classificationBtn)
//
//        }
//
//        // 화면에 이미지 띄우기
//        runOnUiThread {
//            imageView.setImageBitmap(objectDetectionResult)
//        }
//    }



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
     * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>
     *      Draw a box around each objects and show the object's name.
     */
    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
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

    /**
     * floatToDp(f : Float):
     *        float 값을 dp값으로 변화해서 반환
     */
    private fun floatToDp(f : Float):Int {
        return (f * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
    }

    /**
     * cropBitmap(original: Bitmap, cropRect: Rect):
     *       original 이미지를 cropRect에 맞게 잘르 이미지를 만들어 반환
     */
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

    /**
     * overlayBitmap(original: Bitmap, add: Bitmap, optimizationX:Int, optimizationY:Int):
     *       original 이미지에 add 이미지를 (optimizationX, optimizationY) 좌표에 붙여 넣은 이미지를 반환한다.
     */
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

    private fun setDetecter() {
        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
        faceDetector = FaceDetection.getClient(highAccuracyOpts)

        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(10)          // 최대 결과 (모델에서 감지해야 하는 최대 객체 수)
            .setScoreThreshold(0.3f)    // 점수 임계값 (감지된 객체를 반환하는 객체 감지기의 신뢰도)
            .build()
        customObjectDetector = ObjectDetector.createFromFileAndOptions(
            this,
            "model.tflite",
            options
        )

    }

    private fun getFaceDetectionOneByOne(bitmap: Bitmap): ArrayList<Face>? {
        var returnFaces : ArrayList<Face>? = null
        var returnState = false

        val image = InputImage.fromBitmap(bitmap, 0)

        faceDetector.process(image)
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
}




data class DetectionResult(val boundingBox: RectF, val text: String)
