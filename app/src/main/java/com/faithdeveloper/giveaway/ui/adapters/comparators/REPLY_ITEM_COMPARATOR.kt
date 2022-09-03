package com.faithdeveloper.giveaway.ui.adapters.comparators

import androidx.recyclerview.widget.DiffUtil
import com.faithdeveloper.giveaway.data.models.CommentData
import com.faithdeveloper.giveaway.data.models.ReplyData

object REPLY_ITEM_COMPARATOR : DiffUtil.ItemCallback<ReplyData>() {
    override fun areItemsTheSame(oldItem: ReplyData, newItem: ReplyData) = oldItem.reply?.id == newItem.reply?.id

    override fun areContentsTheSame(oldItem: ReplyData, newItem: ReplyData)= oldItem == newItem

}