package com.faithdeveloper.giveaway.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.CommentData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.CommentsItemBinding
import com.faithdeveloper.giveaway.ui.adapters.comparators.COMMENTS_ITEM_COMPARATOR
import com.faithdeveloper.giveaway.utils.Extensions
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.interfaces.CommentsEditInterface

class CommentsPagerAdapter(
    val profileNameClick: (author: UserProfile) -> Unit,
    private val reply: (profileOfTheAuthorBeingReplied: UserProfile) -> Unit,
    val userUid: String,
    val moreClick: (action: String, postID: String, postText: String) -> Unit
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
            holder.bind(currentItem, position)
        }
    }

    inner class CommentsViewHolder(val binding: CommentsItemBinding) :
        RecyclerView.ViewHolder(binding.root), CommentsEditInterface {

        val commentsInterface = this
        override fun updateComment(comment: String) {
            with(binding) {
                commentsText.text = comment
            }
        }

        fun bind(item: CommentData, position: Int) {
            val comment = item.comment
            val author = item.author
            val userRepliedTo = item.userRepliedTo

            with(binding) {
                comment?.let { comment ->

                    author?.let { author ->

                        name.text = author.name
                        Glide.with(itemView)
                            .load(author.profilePicUrl)
                            .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
                            .into(profilePic)

                        if (author.id == userUid) {
//                         user is the author of the comment, make menu that contains the delete option available
                            binding.more.makeVisible()
                            more.setOnClickListener {
                                val popup = PopupMenu(itemView.context, commentsText)
                                popup.menuInflater.inflate(R.menu.comment_menu, popup.menu)
                                popup.setOnMenuItemClickListener {
                                    if (it.itemId == R.id.edit) {
                                        moreClick.invoke(
                                            "edit",
                                            comment.parentID,
                                            comment.commentText
                                        )
                                    } else {
                                        positionOfItemToDelete = position
                                        moreClick.invoke(
                                            "delete",
                                            comment.parentID,
                                            comment.commentText
                                        )
                                    }
                                    return@setOnMenuItemClickListener true
                                }
                                popup.show()
                            }

                            if (author.id != userUid) {
                                this.reply.makeVisible()
                                name.setOnClickListener {
                                    profileNameClick.invoke(author)
                                }
                            }
                        }
                    }
                    with(comment) {
                        commentsText.text = commentText
                        binding.time.text = Extensions.convertTime(time!!)
                        if (idOfTheUserThisCommentIsAReplyTo != "" && idOfTheUserThisCommentIsAReplyTo != userUid) {
                            tag.makeVisible()
                            tag.text = userRepliedTo?.name
                        }
                    }
                }
                reply.setOnClickListener {
                    this@CommentsPagerAdapter.reply.invoke(author!!)
                }
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
