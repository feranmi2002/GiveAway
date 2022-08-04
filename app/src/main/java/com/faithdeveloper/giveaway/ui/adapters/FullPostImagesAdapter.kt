package com.faithdeveloper.giveaway.ui.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.databinding.AdaptiveMediaLayoutBinding

class FullPostImagesAdapter(
    val mediaUrl: Array<String>
) :
    RecyclerView.Adapter<FullPostImagesAdapter.PostPicturesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostPicturesViewHolder {
        val binding =
            AdaptiveMediaLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostPicturesViewHolder(binding)
    }

    inner class PostPicturesViewHolder(private val binding: AdaptiveMediaLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            with(binding) {
                remove.makeInVisible()
                play.makeInVisible()
                Glide.with(itemView)
                    .load(mediaUrl[position])
                    .placeholder(R.drawable.placeholder)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }
                    }).into(media)
            }
        }
    }

    override fun onBindViewHolder(holder: PostPicturesViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = mediaUrl.size
}