package com.example.camerax.ViewerModule

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

import com.example.camerax.databinding.FragmentViewerBinding

import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.camerax.PictureModule.Picture
import com.example.camerax.R

import com.example.camerax.jpegViewModel


class ViewerFragment : Fragment() {

    private lateinit var binding: FragmentViewerBinding
    private val jpegViewModel by activityViewModels<jpegViewModel>()

    // 후에 JpegViewer가 할 일
    private lateinit var mainPicture: Picture

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentViewerBinding.inflate(inflater, container, false)
        Log.d("[ViewerFragment] onCreateView: ","fragment 전환됨")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("[ViewerFragment] onViewCreated: ","")
        Log.d("jpegContainer = ", jpegViewModel.jpegMCContainer.value?.toString()+"")

        var size = jpegViewModel.imageUriLiveData.value!!.size // activity viewmodel 데이터가 제대로 들어왔는지 확인
        Log.d("[ViewerFragment] imageUriLIst size : ",""+size)

        init()
    }


    fun init() {

        /** TODO: 1) main uri, previous uri, next uri 로 메인 스크롤뷰 채우기(main 이미지)
                  2) jpegContainer 만들기 - main uri로
                  3) jpegContainer 분석해서 main 및 내부 이미지들로 하단 스크롤뷰 채우기
        */

//        Log.d("fragment test songsong", ""+jpegViewModel.jpegMCContainer.value?.imageContent?.pictureList?.size)
//        var size = jpegViewModel.jpegMCContainer.value?.imageContent?.pictureList?.size
//
//        val imageContent = jpegViewModel.jpegMCContainer.value?.imageContent
//
//        mainPicture = imageContent?.getPictureAtIndex(0)!!
//        binding.mainPictureView.setImageBitmap(mainPicture?.getBitmap())
//
//
//        for (i in 0 until size!!) {
//
//            val image = imageContent.getPictureAtIndex(i)!!.getBitmap()
//
//            // 넣고자 하는 layout 불러오기
//            val scollItemLayout = layoutInflater.inflate(R.layout.scroll_item_layout, null)
//
//            // 위 불러온 layout에서 변경을 할 view가져오기
//            val scrollImageView: ImageView =
//                scollItemLayout.findViewById(R.id.scrollImageView)
//
//            scrollImageView.setImageBitmap(image)
//            binding.linear.addView(scollItemLayout)
//        }



        val source = BitmapFactory.decodeResource(getResources(), R.drawable.santa2)
        binding.mainPictureView.setImageBitmap(source)
//
//        for (i in 1..5) {
//            val imageView = ImageView(requireContext())
//            imageView.setImageResource(R.drawable.santa)
//            binding.linear.addView(imageView)
//        }

    }
}