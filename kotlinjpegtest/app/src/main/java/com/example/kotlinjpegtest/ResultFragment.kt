package com.example.kotlinjpegtest

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinjpegtest.data.Marker
import com.example.kotlinjpegtest.databinding.ActivityMainBinding
import com.example.kotlinjpegtest.databinding.FragmentMainBinding
import com.example.kotlinjpegtest.databinding.FragmentResultBinding
import com.example.kotlinjpegtest.databinding.ItemMarkerBinding
import java.io.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResultFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ResultFragment : Fragment() {
    var jpegConstant : JpegConstant = JpegConstant()
    var resultBitMap: Bitmap? = null
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap
    private lateinit var binding: FragmentResultBinding
    var markerDataList : ArrayList<Marker> =arrayListOf()
    lateinit var mainActivity: MainActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // 2. Context를 액티비티로 형변환해서 할당
        mainActivity = context as MainActivity
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentResultBinding.inflate(inflater, container, false)
        binding!!.resultImageView.setOnClickListener {
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
//        val resultByteArray = this.arguments?.getByteArray("image")
//        resultBitMap = byteArrayToBitmap(resultByteArray!!)

//        binding.resultImageView.setImageBitmap(resultBitMap)

        //save 버튼 클릭
        binding!!.btnSave.setOnClickListener{
            val bitmap = drawBitmap()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
                if (bitmap != null) {
                    saveImageOnAboveAndroidQ(bitmap)
                }
                Toast.makeText(mainActivity, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
                val writePermission = ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

                if(writePermission == PackageManager.PERMISSION_GRANTED) {
                    if (bitmap != null) {
                        saveImageOnUnderAndroidQ(bitmap)
                    }
                    Toast.makeText(mainActivity, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val requestExternalStorageCode = 1

                    val permissionStorage = arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    ActivityCompat.requestPermissions(mainActivity, permissionStorage, requestExternalStorageCode)
                }
            }
        }
        val linearLayoutManager = LinearLayoutManager(mainActivity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        binding!!.recycleView.setLayoutManager(linearLayoutManager)
        binding!!.recycleView.adapter = MarkerAdapter()
        return binding.root
    }
    //갤러리에서 돌아올 때
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        Log.d("결과", " 프래그먼트 onActivityResult 호출 ${requestCode}")
        // 1번째 image view 클릭 (source image)
        if(requestCode == 0){
            if(resultCode == Activity.RESULT_OK){
                var sourcePhotoUri = data?.data
                // ImageView에 image set
                binding.resultImageView.setImageURI(sourcePhotoUri)
                val iStream: InputStream? = mainActivity.contentResolver.openInputStream(sourcePhotoUri!!)
                var sourceByteArray = getBytes(iStream!!)
                getMarker(sourceByteArray)
//                Log.d("이미지", "sourceByteArray ${sourceByteArray}")
            }else{
                mainActivity.finish()
            }
            // 2번째 image view 클릭 (dest image)
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
    inner class MarkerViewHolder(val binding: ItemMarkerBinding) : RecyclerView.ViewHolder(binding.root)
    @SuppressLint("SuspiciousIndentation")
    inner class MarkerAdapter : RecyclerView.Adapter<MarkerViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding: ItemMarkerBinding = ItemMarkerBinding.inflate(inflater, parent, false)
            return MarkerViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return markerDataList!!.size
        }

        override fun onBindViewHolder(holder: MarkerViewHolder, position: Int) {
            val item = markerDataList!![position]
            Log.d("blutooth scan","onBindViewHolder. (position = ${position}, item = ${item})")
            holder.binding.textView.text=  item.name
            holder.binding.textView2.text= item.index
        }

    }
    fun getMarker(byteArray: ByteArray){
        var n1: Int
        var n2: Int

        for (i in 0 until byteArray.size - 1) {

            n1 = Integer.valueOf(byteArray[i].toInt())
            if (n1 < 0) {
                n1 += 256
            }
            n2 = Integer.valueOf(byteArray[i+1].toInt())
            if (n2 < 0) {
                n2 += 256
            }

            val twoByteToNum = n1 + n2
            if (markerHashMap.containsKey(twoByteToNum) && n1 == 255) {
                //println("마커 찾음 : ${i}: ${twoByteToNum}")
                //println("n1 : ${n1}, n2 : ${n2}")
                var curMarker = Marker()
                curMarker.index = i.toString()
                curMarker.name = jpegConstant.nameHashMap.get(twoByteToNum);
                markerDataList!!.add(curMarker)
                Log.d("Marker", "마커 찾음 : ${i}: ${twoByteToNum}")
            }
        }
        binding!!.recycleView.adapter?.notifyDataSetChanged()
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
    // Byte를 Bitmap으로 변환
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
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
        val uri = mainActivity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if(uri != null) {
                val image = mainActivity.contentResolver.openFileDescriptor(uri, "w", null)
                // write 모드로 file을 open한다.

                if(image != null) {
                    val fos = FileOutputStream(image.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    //비트맵을 FileOutputStream를 통해 compress한다.
                    fos.close()

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // 저장소 독점을 해제한다.
                    mainActivity.contentResolver.update(uri, contentValues, null, null)
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

            mainActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))
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