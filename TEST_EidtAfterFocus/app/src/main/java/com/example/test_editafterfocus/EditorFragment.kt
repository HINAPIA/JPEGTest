package com.example.test_editafterfocus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test_editafterfocus.databinding.FragmentCameraBinding
import com.example.test_editafterfocus.databinding.FragmentEditorBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class EditorFragment : Fragment() {

    private lateinit var editImageBitmap : Bitmap
    private lateinit var viewBinding : FragmentEditorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentEditorBinding.inflate(inflater, container, false)

        val bundle = arguments
        if (bundle != null) {
            lifecycleScope.launch(Dispatchers.Main) {
                val imageList = async(Dispatchers.IO) {
                    bundle.getSerializable("image") as ArrayList<ByteArray>
                }
                val test = imageList.await()
                editImageBitmap = byteArrayToBitmap(test[0])

                val matrix : Matrix = Matrix()
                matrix.setRotate(90F)
                editImageBitmap = Bitmap.createBitmap(editImageBitmap, 0, 0, editImageBitmap.width, editImageBitmap.height, matrix, true)

                viewBinding.editImageView.setImageBitmap(editImageBitmap)
            }
        }

        // Inflate the layout for this fragment
        return viewBinding.root
    }

    /**
     * ByteArray ===> Bitmap
     */
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        Log.v("Editor Check", "byteArrayToBitmap 인자 확인 : $byteArray")
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

}