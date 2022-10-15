package com.faithdeveloper.giveaway.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.CommentData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.CommentsItemBinding
import com.faithdeveloper.giveaway.utils.Extensions
import com.faithdeveloper.giveaway.utils.interfaces.CommentsEditInterface

class NewCommentsAdapter(
    val comments: MutableList<CommentData>,
    val profileNameClick: (author: UserProfile) -> Unit,
    private val reply: (commentId: String, text: String, count: Int) -> Unit,
    val userUid: String,
    val deleteComment: (commentId: String, replies: Int) -> Unit,
    val editComment: (comment: CommentData?) -> Unit
) :
    RecyclerView.Adapter<NewCommentsAdapter.CommentsViewHolder>() {

    //    this is used to note the position of the item user wants to delete
    var positionOfItemToDelete = -1

    inner class CommentsViewHolder(val binding: CommentsItemBinding) :
        RecyclerView.ViewHolder(binding.root){
        private var mItem: CommentData? = null
        val more = binding.more
        val reply = binding.reply
        val name = binding.name
        val profilePic = binding.profilePic

        init {
            more.setOnClickListener {
                val popup = PopupMenu(itemView.context, binding.commentsText)
                if (mItem?.author!!.id == userUid) popup.menuInflater.inflate(
                    R.menu.comment_menu_edit_delete,
                    popup.menu
                )
                else popup.menuInflater.inflate(R.menu.comment_menu_edit, popup.menu)
                popup.setOnMenuItemClickListener {
                    if (it.itemId == R.id.edit) {
                        positionOfItemToDelete = bindingAdapterPosition
                        editComment.invoke(
                            mItem
                        )
                    } else {
                        positionOfItemToDelete = bindingAdapterPosition
                        deleteComment.invoke(
                            mItem?.comment!!.id,
                            mItem?.comment!!.replies
                        )
                    }
                    return@setOnMenuItemClickListener true
                }
                popup.show()
            }

            reply.setOnClickListener {
                this@NewCommentsAdapter.reply.invoke(
                    mItem?.comment!!.id, mItem?.comment!!.commentText, mItem?.comment!!.replies
                )
            }

            name.setOnClickListener {
                profileNameClick.invoke(mItem?.author!!)
            }
            profilePic.setOnClickListener {
                profileNameClick.invoke(mItem?.author!!)
            }
        }

        fun bind(item: CommentData) {
            mItem = item
            val comment = item.comment
            val author = item.author

            with(binding) {
                comment?.let { comment ->
                    author?.let { author ->
                        name.text = author.name
                        binding.more.isVisible = author.id == userUid
                    }
                    with(comment) {
                        replies.run {
                            if (this > 0) reply.text = "$replies replies"
                        }
                        commentsText.text = commentText
                        binding.time.text = Extensions.convertTime(time)
                    }
                }
                Glide.with(itemView)
                    .load(author?.profilePicUrl)
                    .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
                    .into(profilePic)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
        return CommentsViewHolder(
            CommentsItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount() = comments.size

    fun removeComment() {
        notifyItemRemoved(positionOfItemToDelete)
    }
}