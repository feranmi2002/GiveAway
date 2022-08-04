package com.faithdeveloper.giveaway.pagingsources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.faithdeveloper.giveaway.data.models.FeedPagerKey
import com.faithdeveloper.giveaway.data.*
import com.faithdeveloper.giveaway.data.models.PagerResponse
import com.faithdeveloper.giveaway.data.models.Post

class ProfilePagingSource(val repository: Repository, private var firstTimeLoad: Boolean, val userId: String) :
    PagingSource<FeedPagerKey, Post>() {
    override val jumpingSupported = false
    override val keyReuseSupported = false

    override fun getRefreshKey(state: PagingState<FeedPagerKey, Post>) = null

    override suspend fun load(params: LoadParams<FeedPagerKey>): LoadResult<FeedPagerKey, Post> {
        return try {
            val response = repository.getProfileFeed(params.key!!, userId).data as PagerResponse<Post>

            val nextKey = if (response.data.isNotEmpty()) {
                // data was loaded
                if (response.data.size < 10) {
                    // last set of data loaded. No new ones available
                    null
                }
                // new data still available in the database
                else FeedPagerKey(lastSnapshot = response.lastSnapshot, filter = params.key!!.filter , loadSize = params.key!!.loadSize)
            } else {
                // no new data found
                null
            }

            val prevKey = if (firstTimeLoad) {
                firstTimeLoad = false
                null
            } else FeedPagerKey(lastSnapshot = params.key?.lastSnapshot , filter = params.key!!.filter , loadSize = params.key!!.loadSize)
            LoadResult.Page(
                data = response.data,
                nextKey = nextKey,
                prevKey = prevKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}