package com.faithdeveloper.giveaway.ui.adapters

import android.annotation.SuppressLint
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
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.databinding.UnadaptiveMediaLayoutBinding

class PostPicturesAdapter(
    val mediaUrl: List<String>,
    val dataSavingMode: Boolean,
    val mediaClick: (media: Array<String>, hasVideo: Boolean) -> Unit,
    val hasVideo: Boolean
) :
    RecyclerView.Adapter<PostPicturesAdapter.PostPicturesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostPicturesViewHolder {
        val binding = UnadaptiveMediaLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostPicturesViewHolder(binding)
    }

    inner class PostPicturesViewHolder(private val binding: UnadaptiveMediaLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {
            with(binding) {
//                if (!dataSavingMode){
//                    image.setImageResource(R.drawable.placeholder)
//                    touchToLoad.makeVisible()
//                }else {
                    Glide.with(itemView)
                        .load(mediaUrl[position])
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                // do nothing
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                binding.progressCircular.makeInVisible()
                                return false
                            }
                        })
                        .placeholder(R.drawable.placeholder)
                        .into(image)

                count.text = "${position + 1}/${mediaUrl.size}"
                if (hasVideo || mediaUrl.size <= 1) count.makeInVisible()
                if (hasVideo) play.makeVisible()
                image.setOnClickListener {
                    mediaClick.invoke(mediaUrl.toTypedArray(), hasVideo)
                }
            }
        }
  }
    override fun onBindViewHolder(holder: PostPicturesViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = mediaUrl.size
}