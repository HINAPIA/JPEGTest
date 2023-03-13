package com.example.kotlinjpegtest

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinjpegtest.data.Marker
import com.example.kotlinjpegtest.databinding.ActivityMainBinding
import com.example.kotlinjpegtest.databinding.FragmentMainBinding
import com.example.kotlinjpegtest.databinding.ItemMarkerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragment : Fragment() {
    var jpegConstant : JpegConstant = JpegConstant()
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap

    var sourcePhotoUri : Uri? = null
    var destPhotoUri : Uri? = null
    var resultBitMap: Bitmap? = null
    var sourceByteArray : ByteArray? = null
    var destByteArray : ByteArray? = null
    private lateinit var binding: FragmentMainBinding
    lateinit var mainActivity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 2. Context를 액티비티로 형변환해서 할당
        mainActivity = context as MainActivity
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        //source 이미지 클릭
        binding!!.imageView1.setOnClickListener {
            Log.d("이미지", "1 클릭")
            // 갤러리로 이동
            var photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            mainActivity.startActivityForResult(photoIntent, 0)
            // 권한 요청
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MainActivity.REQUEST_CODE
            )
        }

        //dest 이미지 클릭
        binding!!.imageView2.setOnClickListener {
            Log.d("이미지", "2 클릭")
            // 갤러리로 이동
            var photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            mainActivity.startActivityForResult(photoIntent, 1)
            // 권한 요청
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MainActivity.REQUEST_CODE
            )
        }

        //add 버튼 클릭
        binding!!.btnAdd.setOnClickListener{
            if(sourceByteArray != null && destByteArray != null){
                insertFrameToJpeg(sourceByteArray!!,destByteArray!!)
            }
        }

        // custom insert 버튼 클릭
        binding!!.btnNew.setOnClickListener{
            val intent = Intent(getActivity(), CustomMainActivity::class.java)
            startActivity(intent)
        }
        return binding.root
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
            //

            var bundle = Bundle()
            bundle.putByteArray("image",resultBytes)
            var fragment = ResultFragment()
            fragment.arguments = bundle
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment,fragment)
                .commit()
        }

    }
    //갤러리에서 돌아올 때
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        Log.d("결과", " 프래그먼트 onActivityResult 호출 ${requestCode}")
        // 1번째 image view 클릭 (source image)
        if(requestCode == 0){
            if(resultCode == Activity.RESULT_OK){
                sourcePhotoUri = data?.data
                // ImageView에 image set
                binding.imageView1.setImageURI(sourcePhotoUri)
                val iStream: InputStream? = mainActivity.contentResolver.openInputStream(sourcePhotoUri!!)
                sourceByteArray = getBytes(iStream!!)
                Log.d("이미지", "sourceByteArray ${sourceByteArray}")
            }else{
                mainActivity.finish()
            }
            // 2번째 image view 클릭 (dest image)
        }else if(requestCode ==1){
            if(resultCode == Activity.RESULT_OK){
                destPhotoUri = data?.data
                // ImageView에 image set
                binding.imageView2.setImageURI(destPhotoUri)
                val iStream: InputStream? = mainActivity.contentResolver.openInputStream(destPhotoUri!!)
                destByteArray = getBytes(iStream!!)
                Log.d("이미지", "destByteArray ${destByteArray}")
            }else{
                mainActivity.finish()
            }
        }
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

}