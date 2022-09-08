package com.faithdeveloper.giveaway.utils

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.faithdeveloper.giveaway.data.models.FeedData
import com.faithdeveloper.giveaway.data.models.Post

open class ProfileBaseViewHolder(binding: View):RecyclerView.ViewHolder(binding) {
    val context: Context = itemView.context


    open fun bind(item: Post){

    }
}