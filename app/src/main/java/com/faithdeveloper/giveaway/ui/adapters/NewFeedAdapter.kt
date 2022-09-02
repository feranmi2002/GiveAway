package com.faithdeveloper.giveaway.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.FeedData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.*
import com.faithdeveloper.giveaway.utils.Extensions
import com.faithdeveloper.giveaway.utils.Extensions.getDataSavingMode
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible

class NewFeedAdapter(
    val reactions: (reactionType: String, postID: String, posterID: String) -> Unit,
    private val profileNameClick: (userUid: UserProfile) -> Unit,
    private val imagesClick: (images: Array<String>, hasVideo: Boolean, position: Int) -> Unit,
    private val menuAction: (action: String) -> Unit,
    val userUid: String,
    val data: MutableList<FeedData>,
    val dataSavingMode: Boolean
) : RecyclerView.Adapter<NewFeedAdapter.FeedViewHolder>() {
    inner class FeedViewHolder(val binding: FeedItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FeedData) {
            val post = item.postData
            val poster = item.authorData
            with(binding) {
                description.setBackgroundColor(binding.root.context.resources.getColor(R.color.purple_200))
                // load poster's profile picture
                Glide.with(itemView)
                    .load(poster?.profilePicUrl)
                    .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
                    .into(profiePic)

                // setup poster's data and reactions
                poster?.let { posterData ->
                    profileName.text = posterData.name
                    reaction.email.setOnClickListener {
                        reactions.invoke("email", posterData.email, posterData.id)
                    }
                    reaction.phone.setOnClickListener {
                        reactions.invoke("phone", posterData.phoneNumber, posterData.id)
                    }
                    reaction.whatsapp.setOnClickListener {
                        reactions.invoke("whatsapp", posterData.phoneNumber, posterData.id)
                    }
                    // setup post's data
                    post?.let {
                        with(it) {
                            if (post.authorId != userUid) {
                                profileName.setOnClickListener {
                                    profileNameClick.invoke(poster!!)
                                }
                            }
//                            description.text = text
                            // setup reaction views
                            if (hasComments) {
                                reaction.comments.makeVisible()
                                reaction.comments.setOnClickListener {
                                    reactions.invoke("comments", post.postID, posterData.id)
                                }
                            }
                            if (link != "") {
                                reaction.launchLink.makeVisible()
                                reaction.launchLink.setOnClickListener {
                                    reactions.invoke("launchLink", post.link, posterData.id)
                                }
                            }

//                             show time
                            timeView.text = Extensions.convertTime(time)

//                            //setup media
//                            if (mediaUrls.isNotEmpty()) {
//                                media.makeVisible()
//                                val dataSavingMode = itemView.context.getDataSavingMode()
//                                val adapter = PostPicturesAdapter(
//                                    post.mediaUrls,
//                                    dataSavingMode,
//                                    imagesClick,
//                                    hasVideo
//                                )
//                                media.layoutManager = LinearLayoutManager(
//                                    itemView.context,
//                                    LinearLayoutManager.HORIZONTAL,
//                                    false
//                                )
//                                media.adapter = adapter
//                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val currentItem = data[position]
        holder.bind(item = currentItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding =
            FeedItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding)
    }

    override fun getItemCount() = data.size
}
