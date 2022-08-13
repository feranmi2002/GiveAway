package com.faithdeveloper.giveaway.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faithdeveloper.giveaway.utils.Extensions.getDataSavingMode
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.ui.adapters.comparators.PROFILE_FEED_ITEM_COMPARATOR
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.Post
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.*
import com.faithdeveloper.giveaway.utils.Extensions

class ProfilePagerAdapter(
    val reactions: (reactionType: String, data: String, posterID:String) -> Unit,
    private val imagesClick: (images: Array<String>, hasVideo: Boolean ) -> Unit,
    private val menuAction: (action: String) -> Unit,
    val userProfile: UserProfile
) :
    PagingDataAdapter<Post, ProfilePagerAdapter.FeedViewHolder>
        (PROFILE_FEED_ITEM_COMPARATOR) {

    inner  class FeedViewHolder(val binding:FeedItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Post) {
            with(binding) {
                // load poster's profile picture
                Glide.with(itemView)
                    .load(userProfile.profilePicUrl)
                    .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
                    .into(profiePic)

                // setup poster's data and reactions
                profileName.text = userProfile.name

                    // setup post's data
                with(item) {
                    description.text = text

                    // setup reaction views
                    if (hasComments) {
                        reaction.comments.text = "Comments"
                        reaction.comments.makeVisible()
                        reaction.comments.setOnClickListener {
                            reactions.invoke("comments", item.postID, userProfile.id)
                        }
                    }
                    if (link != "") {
                        reaction.launchLink.makeVisible()
                        reaction.launchLink.setOnClickListener {
                            reactions.invoke("launchLink", item.link, userProfile.id)
                        }
                    }

                    // show time
                    timeView.text = Extensions.convertTime(time!!)

                    //setup media
                    if (mediaUrls.isNotEmpty()) {
                        media.makeVisible()
                        val dataSavingMode = itemView.context.getDataSavingMode()
                        val adapter = PostPicturesAdapter(
                            item.mediaUrls,
                            dataSavingMode,
                            imagesClick,
                            hasVideo
                        )
                        media.layoutManager = LinearLayoutManager(
                            itemView.context,
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        media.adapter = adapter
                    }
                }
                }
            }
        }
    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null) {
            holder.bind(item = currentItem)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
     val binding = FeedItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding)
    }
}
