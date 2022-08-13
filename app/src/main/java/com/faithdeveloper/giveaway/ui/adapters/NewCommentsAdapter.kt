package com.faithdeveloper.giveaway.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.utils.Extensions
import com.faithdeveloper.giveaway.utils.Extensions.getUserProfilePicUrl
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.data.models.Comment
import com.faithdeveloper.giveaway.data.models.CommentData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.CommentsItemBinding
import com.faithdeveloper.giveaway.utils.interfaces.CommentsEditInterface

class NewCommentsAdapter(val comments: MutableList<CommentData>,
                         val profileNameClick: (poster: UserProfile) -> Unit,
                         private val reply: (profileOfTheAuthorBeingReplied: UserProfile) -> Unit,
                         val userUid: String,
                         val moreClick: (action: String, postID: String, postText: String) -> Unit) :
    RecyclerView.Adapter<NewCommentsAdapter.NewCommentsViewHolder>() {

    //    this is used to note the position of the item user wants to delete
    var positionOfItemToDelete = -1

    //    this interface is used for updating the comment
    private var commentsEditInterface: CommentsEditInterface? = null

    inner class NewCommentsViewHolder(val binding: CommentsItemBinding) :
        RecyclerView.ViewHolder(binding.root), CommentsEditInterface {

        val commentsInterface = this
        override fun updateComment(comment: String) {
            with(binding) {
                commentsText.text = comment
            }
        }

        fun bind(position: Int) {

            val comment = comments[position].comment
            val author = comments[position].author
            val userRepliedTo = comments[position].userRepliedTo

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
                                            comment.idOfPostThatIsCommented,
                                            comment.commentText
                                        )
                                    } else {
                                        positionOfItemToDelete = position
                                        moreClick.invoke(
                                            "delete",
                                            comment.idOfPostThatIsCommented,
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
                                    profileNameClick.invoke(author!!)
                                }
                            }
                        }
                    }
                    with(comment) {
                        commentsText.text = commentText
                        binding.time.text = Extensions.convertTime(time!!)
                        if (idOfTheUserThisCommentIsAReplyTo != "") {
                            tag.makeVisible()
                            tag.text = userRepliedTo?.name
                        }
                    }
                }
                reply.setOnClickListener {
                    this@NewCommentsAdapter.reply.invoke(author!!)
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewCommentsViewHolder {
        return NewCommentsViewHolder(
            CommentsItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }
    override fun onBindViewHolder(holder: NewCommentsViewHolder, position: Int) {
        commentsEditInterface = holder.commentsInterface
        holder.bind(position)
    }
    override fun getItemCount() = comments.size

    fun updateComment(newComment: String) {
        commentsEditInterface?.updateComment(newComment)
    }

    fun removeComment() {
        notifyItemRemoved(positionOfItemToDelete - 1)
    }
}