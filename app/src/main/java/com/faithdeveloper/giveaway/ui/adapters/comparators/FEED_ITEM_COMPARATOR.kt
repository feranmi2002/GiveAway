package com.faithdeveloper.giveaway.ui.adapters.comparators

import androidx.recyclerview.widget.DiffUtil
import com.faithdeveloper.giveaway.data.models.FeedData

object FEED_ITEM_COMPARATOR : DiffUtil.ItemCallback<FeedData>() {

    override fun areItemsTheSame(oldItem: FeedData, newItem: FeedData) = false

    override fun areContentsTheSame(
        oldItem: FeedData,
        newItem: FeedData
    ) =
    oldItem == newItem
}