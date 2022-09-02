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
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.databinding.FullPostMediaItemLayoutBinding
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.interfaces.FullImageAdapterInterface

class FullPostImagesAdapter(
    val mediaUrl: Array<String>,
    val fullImageAdapterInterface: FullImageAdapterInterface
) :
    RecyclerView.Adapter<FullPostImagesAdapter.ImagesViewHolder>() {

    private var viewHolder:ImagesViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagesViewHolder {
        val binding =
            FullPostMediaItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ImagesViewHolder(binding)
    }

    inner class ImagesViewHolder(private val binding: FullPostMediaItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            with(binding) {
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
                            fullImageAdapterInterface.imageIsReady(false)
                            return false
                        }
                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            progressCircular.makeInVisible()
                            fullImageAdapterInterface.imageIsReady(true)
                            return false
                        }
                    }).into(media)

            }
        }
    }

    override fun onBindViewHolder(holder: ImagesViewHolder, position: Int) {
        holder.bind(position)
        viewHolder = holder
    }

    override fun getItemCount() = mediaUrl.size

    fun getViewHolder()  = viewHolder
}