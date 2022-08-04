package com.faithdeveloper.giveaway.ui.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
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
import com.faithdeveloper.giveaway.databinding.AdaptiveMediaLayoutBinding
import com.faithdeveloper.giveaway.ui.fragments.NewPost.Companion.IMAGE

class NewPostMediaAdapter(
    val mediaUrl: MutableList<Uri>,
    var mediaType: String,
    val clickListener: (position: Int, uri: Uri) -> Unit
) :
    RecyclerView.Adapter<NewPostMediaAdapter.NeedMediaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NeedMediaViewHolder {
        val binding =AdaptiveMediaLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NeedMediaViewHolder(binding)
    }

    inner class NeedMediaViewHolder(private val binding: AdaptiveMediaLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {
            with(binding) {
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
                            //
                            return false
                        }
                    })
                    .placeholder(
                        R.drawable.placeholder
                    )
                    .into(media)
                binding.count.text = "${position + 1}/${mediaUrl.size}"
                if (mediaUrl.size <= 1) count.makeInVisible()
                else count.makeVisible()
                if (mediaType == IMAGE) play.makeInVisible()
                else play.makeVisible()

                remove.setOnClickListener {
                    clickListener.invoke(position, mediaUrl[position])
                }
            }
        }
    }

    override fun onBindViewHolder(holder: NeedMediaViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = mediaUrl.size

    fun changeMediaType(mediaType: String) {
        this.mediaType = mediaType
    }
}