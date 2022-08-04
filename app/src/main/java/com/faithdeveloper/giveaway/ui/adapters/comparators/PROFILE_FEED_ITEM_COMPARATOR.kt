package com.faithdeveloper.giveaway.ui.adapters.comparators

import androidx.recyclerview.widget.DiffUtil
import com.faithdeveloper.giveaway.data.models.Post

object PROFILE_FEED_ITEM_COMPARATOR : DiffUtil.ItemCallback<Post>() {

    override fun areItemsTheSame(oldItem: Post, newItem: Post) = false

    override fun areContentsTheSame(
        oldItem: Post,
        newItem: Post
    ) =
    oldItem == newItem
}