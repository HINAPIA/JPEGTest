package com.example.camerax.ViewerModule

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.camerax.R
import kotlinx.coroutines.NonDisposableHandle.parent
import java.io.File

class ViewPagerAdapter (val context: Context, uriList: List<Drawable>) : RecyclerView.Adapter<ViewPagerAdapter.PagerViewHolder>() {

    var images = uriList // gallery에 있는 이미지 리스트

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PagerViewHolder(parent)

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        holder.bind(images[position]) // binding
    }

    override fun getItemCount(): Int = images.size

    /* View Holder 정의 */
    inner class PagerViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder
        (LayoutInflater.from(parent.context).inflate(R.layout.main_image_list_item, parent, false)){
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(image: Drawable?) {
            imageView.setImageDrawable(image)
            //imageView.setImageURI(Uri.parse(image))
            //Glide.with(context).load(File(image)).into(imageView)
        }
    }

}