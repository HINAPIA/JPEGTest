package com.example.camerax.ViewerModule

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.camerax.PictureModule.PictureContainer
import com.example.camerax.databinding.FragmentViewerBinding
import androidx.fragment.app.activityViewModels
import com.example.camerax.jpegViewModel

class ViewerFragment : Fragment() {

    private lateinit var binding: FragmentViewerBinding
    private val jpegViewModel by activityViewModels<jpegViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentViewerBinding.inflate(inflater, container, false)
        Log.d("viewerFragment onCreateView: ","여기로 들어옴!!")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("viewrFragment onViewCreated: ","여기로 들어옴!!")
        Log.d("jpegContainer: ",""+ (jpegViewModel.jpegContainer.value?.getCount() ?: -99))


    //        articleAdapter = ArticleAdapter(requireContext())
//
//        viewModel.articleLiveData.observe(viewLifecycleOwner) {
//            if (it != null) {
//                articleAdapter.submitList(it)
//            }
//        }
//
//        binding.articleRecyclerView.apply {
//            setHasFixedSize(true)
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = articleAdapter
//        }
    }
}