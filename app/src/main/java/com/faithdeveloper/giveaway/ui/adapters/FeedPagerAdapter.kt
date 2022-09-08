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
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.models.FeedData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.FeedItemLayoutBinding
import com.faithdeveloper.giveaway.databinding.FeedItemMediaLayoutBinding
import com.faithdeveloper.giveaway.ui.adapters.comparators.FEED_ITEM_COMPARATOR
import com.faithdeveloper.giveaway.utils.BaseViewHolder
import com.faithdeveloper.giveaway.utils.Extensions
import com.google.android.material.badge.BadgeDrawable

class FeedPagerAdapter(
    val reactions: (reactionType: String, data: String,  commentCount:Int) -> Unit,
    private val profileNameClick: (userUid: UserProfile) -> Unit,
    private val imagesClick: (images: Array<String>, hasVideo: Boolean, position: Int) -> Unit,
    private val menuAction: (action: String) -> Unit,
    val userUid: String,
    val dataSavingMode: Boolean
) :
    PagingDataAdapter<FeedData, BaseViewHolder>
        (FEED_ITEM_COMPARATOR) {


    inner class FeedViewHolderMedia(val binding: FeedItemMediaLayoutBinding) :
        BaseViewHolder(binding.root) {
        private val sharedPool = RecyclerView.RecycledViewPool()
        private var mItem: FeedData? = null
        val email = binding.reaction.email
        val phone = binding.reaction.phone
        val whatsapp = binding.reaction.whatsapp
        val profileName = binding.profileName
        val comments = binding.reaction.comments
        val launchLink = binding.reaction.launchLink
        val media = binding.media
        val readMore = binding.readMore
        val description = binding.description

        init {
            email.setOnClickListener {
                reactions.invoke("email", mItem?.authorData!!.email,  mItem?.postData!!.commentCount)
            }
            phone.setOnClickListener {
                reactions.invoke("phone", mItem?.authorData!!.phoneNumber,mItem?.postData!!.commentCount)
            }
            whatsapp.setOnClickListener {
                reactions.invoke(
                    "whatsapp",
                    mItem?.authorData!!.phoneNumber,
                    mItem?.postData!!.commentCount
                )
            }
            profileName.setOnClickListener {
                profileNameClick.invoke(mItem?.authorData!!)
            }
            comments.setOnClickListener {
                    reactions.invoke("comments", mItem?.postData!!.postID,  mItem?.postData!!.commentCount)

            }
            launchLink.setOnClickListener {
                    reactions.invoke("launchLink", mItem?.postData!!.link,mItem?.postData!!.commentCount)
            }

            media.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            media.setRecycledViewPool(sharedPool)

            description.doOnLayout {
                readMore.isVisible = description.layout.text.toString()
                    .equals(mItem?.postData!!.text, true)
            }
        }

        override fun bind(item: FeedData) {
            mItem = item
            val post = item.postData!!
            val author = item.authorData!!
            with(binding) {
                // setup poster's data and reactions
                author.let { authorData ->
                    profileName.text = authorData.name
                    // setup post's data
                    with(post) {
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
                                badgeDrawable.number = post.commentCount
                            }
                        }
                        reaction.launchLink.isGone = link == ""
                        // show time
                        timeView.text = Extensions.convertTime(time)
                        val adapter = PostPicturesAdapter(
                            mediaUrls,
                            dataSavingMode,
                            imagesClick,
                            hasVideo
                        )
                        media.adapter = adapter
                    }
                }
                Glide.with(itemView)
                    .load(author.profilePicUrl)
                    .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
                    .into(profiePic)
            }
        }
    }

    inner class FeedViewHolder(val binding: FeedItemLayoutBinding) :
        BaseViewHolder(binding.root) {
        private var mItem: FeedData? = null
        val email = binding.reaction.email
        val phone = binding.reaction.phone
        val whatsapp = binding.reaction.whatsapp
        val profileName = binding.profileName
        val comments = binding.reaction.comments
        val launchLink = binding.reaction.launchLink
        val media = binding.media
        val description = binding.description

        init {
            email.setOnClickListener {
                reactions.invoke("email", mItem?.authorData!!.email,  mItem?.postData!!.commentCount)
            }
            phone.setOnClickListener {
                reactions.invoke("phone", mItem?.authorData!!.phoneNumber,    mItem?.postData!!.commentCount)
            }
            whatsapp.setOnClickListener {
                reactions.invoke(
                    "whatsapp",
                    mItem?.authorData!!.phoneNumber,

                    mItem?.postData!!.commentCount
                )
            }
            profileName.setOnClickListener {
                profileNameClick.invoke(mItem?.authorData!!)
            }
            comments.setOnClickListener {
                    reactions.invoke("comments", mItem?.postData!!.postID,   mItem?.postData!!.commentCount)

            }
            launchLink.setOnClickListener {
                    reactions.invoke("launchLink", mItem?.postData!!.link,     mItem?.postData!!.commentCount)

            }
            description.doOnLayout {
                media.isGone = description.layout.text.toString()
                    .equals(mItem?.postData!!.text, true)
            }
        }

        override fun bind(item: FeedData) {
            mItem = item
            val post = item.postData!!
            val author = item.authorData!!
            with(binding) {
                // setup poster's data and reactions
                author.let { authorData ->
                    profileName.text = authorData.name
                    // setup post's data
                    with(post) {
                        description.isGone = text.isEmpty()
                        description.text = text
                        // setup reaction views
                        reaction.comments.isGone = !hasComments
                        reaction.launchLink.isGone = link == ""

                        // show time
                        timeView.text = Extensions.convertTime(time)
                    }
                }
                // load poster's profile picture
                Glide.with(itemView)
                    .load(author.profilePicUrl)
                    .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
                    .into(profiePic)
            }

        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null
        ) {
            holder.bind(item = currentItem)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
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
            if (it.postData!!.mediaUrls.isEmpty()) {
                R.layout.feed_item_layout
            } else {
                R.layout.feed_item_media_layout
            }
        }
        return super.getItemViewType(position)
    }
}
