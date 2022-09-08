package com.faithdeveloper.giveaway.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faithdeveloper.giveaway.ui.adapters.comparators.PROFILE_FEED_ITEM_COMPARATOR
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.Post
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.*
import com.faithdeveloper.giveaway.utils.Extensions
import com.faithdeveloper.giveaway.utils.ProfileBaseViewHolder
import com.google.android.material.badge.BadgeDrawable

class ProfilePagerAdapter(
    val reactions: (reactionType: String, data: String, commentCount:Int) -> Unit,
    private val imagesClick: (images: Array<String>, hasVideo: Boolean, positon:Int ) -> Unit,
    private val menuAction: (action: String) -> Unit,
    val userProfile: UserProfile
) : PagingDataAdapter<Post, ProfileBaseViewHolder>
    (PROFILE_FEED_ITEM_COMPARATOR) {

    inner class FeedViewHolderMedia(val binding: FeedItemMediaLayoutBinding) :
        ProfileBaseViewHolder(binding.root) {
        private val sharedPool = RecyclerView.RecycledViewPool()
        private var mItem:Post? = null
        val email = binding.reaction.email
        val phone = binding.reaction.phone
        val whatsapp = binding.reaction.whatsapp
        val comments = binding.reaction.comments
        val launchLink = binding.reaction.launchLink
        val media = binding.media
        val readMore = binding.readMore
        val description = binding.description

        init {
            email.setOnClickListener {
                reactions.invoke("email", userProfile.email,  mItem?.commentCount!!)
            }
            phone.setOnClickListener {
                reactions.invoke("phone", userProfile.phoneNumber,mItem?.commentCount!!)
            }
            whatsapp.setOnClickListener {
                reactions.invoke(
                    "whatsapp",
                    userProfile.phoneNumber,
                    mItem?.commentCount!!
                )
            }
            comments.setOnClickListener {
                    reactions.invoke("comments", mItem?.postID!!,  mItem?.commentCount!!)

            }
            launchLink.setOnClickListener {
                    reactions.invoke("launchLink", mItem?.link!!,mItem?.commentCount!!)
            }
            media.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            media.setRecycledViewPool(sharedPool)

            description.doOnLayout {
                readMore.isVisible = description.layout.text.toString()
                    .equals(mItem?.text, true)
            }
        }

        override fun bind(item: Post) {
            mItem = item
            with(binding) {
                // setup poster's data and reactions
                userProfile.let { authorData ->
                    profileName.text = authorData.name
                    // setup post's data
                    with(item) {
                        description.isGone = text.isEmpty()
                        description.text = text
                        // setup reaction views
                        reaction.comments.isGone = !hasComments
                        reaction.comments.isGone.run {
                            if (this){
                                val badgeDrawable = BadgeDrawable.create(itemView.context)
                                badgeDrawable.badgeGravity = BadgeDrawable.TOP_END
                                badgeDrawable.backgroundColor = itemView.resources.getColor(R.color.teal_200)
                                badgeDrawable.badgeTextColor = itemView.resources.getColor(R.color.white)
                                badgeDrawable.number = item.commentCount
                            }
                        }
                        reaction.launchLink.isGone = link == ""
                        // show time
                        timeView.text = Extensions.convertTime(time)
                        val adapter = PostPicturesAdapter(
                            mediaUrls,
                            false,
                            imagesClick,
                            hasVideo
                        )
                        media.adapter = adapter
                    }
                }
                Glide.with(itemView)
                    .load(userProfile.profilePicUrl)
                    .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
                    .into(profiePic)
            }
        }
    }

    inner class FeedViewHolder(val binding: FeedItemLayoutBinding) :
        ProfileBaseViewHolder(binding.root) {
        private var mItem: Post? = null
        val email = binding.reaction.email
        val phone = binding.reaction.phone
        val whatsapp = binding.reaction.whatsapp
        val comments = binding.reaction.comments
        val launchLink = binding.reaction.launchLink
        val media = binding.media
        val description = binding.description
        val readMore =binding.media

        init {
            email.setOnClickListener {
                reactions.invoke("email", userProfile.email,  mItem?.commentCount!!)
            }
            phone.setOnClickListener {
                reactions.invoke("phone", userProfile.phoneNumber,mItem?.commentCount!!)
            }
            whatsapp.setOnClickListener {
                reactions.invoke(
                    "whatsapp",
                    userProfile.phoneNumber,
                    mItem?.commentCount!!
                )
            }
            comments.setOnClickListener {
                    reactions.invoke("comments", mItem?.postID!!,  mItem?.commentCount!!)
            }
            launchLink.setOnClickListener {
                    reactions.invoke("launchLink", mItem?.link!!,mItem?.commentCount!!)
            }
            description.doOnLayout {
                readMore.isVisible = description.layout.text.toString()
                    .equals(mItem?.text, true)
            }
        }

        override fun bind(post: Post) {
            mItem = post
            with(binding) {
                // setup poster's data and reactions
                userProfile.let { authorData ->
                    profileName.text = authorData.name
                    // setup post's data
                    with(post) {
                        description.isGone = text.isEmpty()
                        description.text = text
                        // setup reaction views
                        reaction.comments.isGone = hasComments
                        reaction.launchLink.isGone = link == ""

                        // show time
                        timeView.text = Extensions.convertTime(time)
                    }
                }
                // load poster's profile picture
                Glide.with(itemView)
                    .load(userProfile.profilePicUrl)
                    .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
                    .into(profiePic)
            }

        }
    }

    override fun onBindViewHolder(holder: ProfileBaseViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null
        ) {
            holder.bind(item = currentItem)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileBaseViewHolder {
        return when (viewType) {
            R.layout.feed_item_layout -> FeedViewHolder(
                FeedItemLayoutBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
            else -> {
                FeedViewHolderMedia(
                    FeedItemMediaLayoutBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        getItem(position)?.let {
            if (it.mediaUrls.isEmpty()) {
                R.layout.feed_item_layout
            } else {
                R.layout.feed_item_media_layout
            }
        }
        return super.getItemViewType(position)
    }
}
