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
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.interfaces.FullImageAdapterInterface

class FullPostImagesAdapter(
    val mediaUrl: Array<String>,
    val adapterInterface:FullImageAdapterInterface
) :
    RecyclerView.Adapter<FullPostImagesAdapter.ImagesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagesViewHolder {
        val binding =
            AdaptiveMediaLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImagesViewHolder(binding)
    }

    inner class ImagesViewHolder(private val binding: AdaptiveMediaLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            with(binding) {
                remove.makeInVisible()
                play.makeInVisible()
                progressCircular.makeVisible()
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
                            adapterInterface.mediaAvailabilityState(false, null)
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            adapterInterface.mediaAvailabilityState(true, mediaUrl[position])
                            progressCircular.makeInVisible()
                            return false
                        }
                    }).into(media)

//                update count in the bottom sheet
                adapterInterface.updateCount(position  + 1, itemCount)
            }
        }
    }

    override fun onBindViewHolder(holder: ImagesViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = mediaUrl.size
}