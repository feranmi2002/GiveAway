package com.faithdeveloper.giveaway.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.CommentData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.CommentsItemBinding
import com.faithdeveloper.giveaway.ui.adapters.comparators.COMMENTS_ITEM_COMPARATOR
import com.faithdeveloper.giveaway.ui.fragments.CommentsBottomSheet.Companion.DELETE
import com.faithdeveloper.giveaway.ui.fragments.CommentsBottomSheet.Companion.UPDATE
import com.faithdeveloper.giveaway.utils.Extensions
import com.faithdeveloper.giveaway.utils.interfaces.CommentsEditInterface

class CommentsPagerAdapter(
    val profileNameClick: (author: UserProfile) -> Unit,
    private val reply: (commentId:String, text:String, count:Int) -> Unit,
    val userUid: String,
    val moreClick: (action: String,commentId:String, postText: String,replies:Int) -> Unit
) :
    PagingDataAdapter<CommentData, CommentsPagerAdapter.CommentsViewHolder>
        (COMMENTS_ITEM_COMPARATOR) {

    //    this is used to note the position of the item user wants to delete
    var positionOfItemToDelete = -1

    //    this interface is used for updating the comment
    private var commentsEditInterface: CommentsEditInterface? = null

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null) {
            commentsEditInterface = holder.commentsInterface
            holder.bind(currentItem)
        }
    }

    inner class CommentsViewHolder(val binding: CommentsItemBinding) :
        RecyclerView.ViewHolder(binding.root), CommentsEditInterface {
        private var mItem: CommentData? = null
        val more = binding.more
        val reply = binding.reply
        val name = binding.name

        init {
            more.setOnClickListener {
                val popup = PopupMenu(itemView.context, binding.commentsText)
                if (mItem?.author!!.id == userUid ) popup.menuInflater.inflate(R.menu.comment_menu_edit_delete, popup.menu)
                else popup.menuInflater.inflate(R.menu.comment_menu_edit, popup.menu)
                popup.setOnMenuItemClickListener {
                    if (it.itemId == R.id.edit) {
                        moreClick.invoke(
                            UPDATE,
                            mItem?.comment!!.id,
                            mItem?.comment!!.commentText,
                            mItem?.comment!!.replies
                        )
                    } else {
                        positionOfItemToDelete = bindingAdapterPosition
                        moreClick.invoke(
                            DELETE,
                            mItem?.comment!!.id,
                            mItem?.comment!!.commentText,
                            mItem?.comment!!.replies
                        )
                    }
                    return@setOnMenuItemClickListener true
                }
                popup.show()
            }

            reply.setOnClickListener {
                this@CommentsPagerAdapter.reply.invoke(
                    mItem?.comment!!.id, mItem?.comment!!.commentText, mItem?.comment!!.replies)
            }

            name.setOnClickListener {
                profileNameClick.invoke(mItem?.author!!)
            }
        }

        val commentsInterface = this
        override fun updateComment(comment: String) {
            with(binding) {
                commentsText.text = comment
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
                            if (this > 0)  reply.text = "$replies replies"
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

    fun updateComment(newComment: String) {
        commentsEditInterface?.updateComment(newComment)
    }

    fun removeComment() {
        notifyItemRemoved(positionOfItemToDelete - 1)
    }


}
