package com.faithdeveloper.giveaway.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.databinding.UnadaptiveMediaLayoutBinding
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible

class PostPicturesAdapter(
    val mediaUrl: List<String>,
    val dataSavingMode: Boolean,
    val mediaClick: (media: Array<String>, hasVideo: Boolean, position: Int) -> Unit,
    val hasVideo: Boolean
) :
    RecyclerView.Adapter<PostPicturesAdapter.PostPicturesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostPicturesViewHolder {
        val binding =
            UnadaptiveMediaLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostPicturesViewHolder(binding)
    }

    inner class PostPicturesViewHolder(private val binding: UnadaptiveMediaLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {
            with(binding) {
                if (dataSavingMode) {
                    image.setImageResource(R.drawable.placeholder)
                    touchToLoad.makeVisible()
                } else {
                    Glide.with(itemView)
                        .load(mediaUrl[position])
                        .placeholder(R.drawable.placeholder)
                        .into(image)
                }

                count.text = "${position + 1}/${mediaUrl.size}"
                if (hasVideo || mediaUrl.size <= 1) count.makeInVisible()
                if (hasVideo) play.makeVisible()
                image.setOnClickListener {
                    mediaClick.invoke(mediaUrl.toTypedArray(), hasVideo, absoluteAdapterPosition)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: PostPicturesViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = mediaUrl.size
}