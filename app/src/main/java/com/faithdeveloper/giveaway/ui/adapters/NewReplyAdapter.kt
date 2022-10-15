package com.faithdeveloper.giveaway.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.CommentData
import com.faithdeveloper.giveaway.data.models.ReplyData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.ReplyItemBinding
import com.faithdeveloper.giveaway.utils.Extensions
import com.faithdeveloper.giveaway.utils.interfaces.CommentsEditInterface

class NewReplyAdapter(
    private val replies: MutableList<ReplyData>,
    val profileNameClick: (author: UserProfile) -> Unit,
    private val reply: (userRepliedTo: UserProfile?) -> Unit,
    val userUid: String,
    val deleteReply: (replyId: String) -> Unit,
    val editReply: (reply: ReplyData?) -> Unit
) :
    RecyclerView.Adapter<NewReplyAdapter.ReplyViewHolder>() {

    //    this is used to note the position of the item user wants to delete
    var positionOfItemToDelete = -1

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        val currentItem = replies[position]
        holder.bind(currentItem)
    }

    inner class ReplyViewHolder(val binding: ReplyItemBinding) :
        RecyclerView.ViewHolder(binding.root){
        private var mItem: ReplyData? = null
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
                        editReply.invoke(
                            mItem
                        )
                    } else {
                        positionOfItemToDelete = bindingAdapterPosition
                        deleteReply.invoke(
                            mItem?.reply!!.id)
                    }
                    return@setOnMenuItemClickListener true
                }
                popup.show()
            }

            reply.setOnClickListener {
                this@NewReplyAdapter.reply.invoke(mItem?.author)
            }

            name.setOnClickListener {
                profileNameClick.invoke(mItem?.author!!)
            }
            profilePic.setOnClickListener{
                profileNameClick.invoke(mItem?.author!!)
            }
        }

        fun bind(item: ReplyData) {
            mItem = item
            val mReply = item.reply
            val author = item.author
            val userRepliedTo = item.userReplied

            with(binding) {
                mReply?.let { reply ->
                    author?.let { author ->
                        tag.isGone = userRepliedTo ==null
                        tag.text = "@${userRepliedTo?.name}"
                        name.text = author.name
                        binding.more.isVisible = author.id == userUid
                    }
                    with(reply) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        return ReplyViewHolder(
            ReplyItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    fun removeReply() {
        notifyItemRemoved(positionOfItemToDelete)
    }

    override fun getItemCount() = replies.size
}
