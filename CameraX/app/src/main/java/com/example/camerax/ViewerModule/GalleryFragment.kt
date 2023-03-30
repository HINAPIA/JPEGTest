package com.example.camerax.ViewerModule

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.camerax.PictureModule.Picture
import com.example.camerax.databinding.FragmentViewerBinding
import com.example.camerax.jpegViewModel

class GalleryFragment : Fragment() {

    private lateinit var binding: FragmentViewerBinding
    private val jpegViewModel by activityViewModels<jpegViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentViewerBinding.inflate(inflater, container, false)
        Log.d("[GalleryFragment] onCreateView: ","fragment 전환됨")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("[GalleryFragment] onViewCreated: ","")
        Log.d("jpegContainer = ", jpegViewModel.jpegMCContainer.value?.toString()+"")

        var size = jpegViewModel.imageUriLiveData.value!!.size // activity viewmodel 데이터가 제대로 들어왔는지 확인
        Log.d("[GalleryFragment] imageUriLIst size : ",""+size)

    }
}