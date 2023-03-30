package com.example.camerax.ViewerModule

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.camerax.R

class ViewPagerAdapter (uriList: List<Drawable>) : RecyclerView.Adapter<ViewPagerAdapter.PagerViewHolder>() {

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

        fun bind(image: Drawable) {
            imageView.setImageDrawable(image)
        }
    }

}