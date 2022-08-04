package com.faithdeveloper.giveaway.ui.adapters.comparators

import androidx.recyclerview.widget.DiffUtil
import com.faithdeveloper.giveaway.data.models.CommentData

object COMMENTS_ITEM_COMPARATOR : DiffUtil.ItemCallback<CommentData>() {
    override fun areItemsTheSame(
        oldItem: CommentData,
        newItem: CommentData
    ) = false

    override fun areContentsTheSame(
        oldItem: CommentData,
        newItem: CommentData
    ) = oldItem == newItem
}