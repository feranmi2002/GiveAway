package com.faithdeveloper.giveaway.pagingsources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.faithdeveloper.giveaway.data.models.PagerKey
import com.faithdeveloper.giveaway.data.*
import com.faithdeveloper.giveaway.data.models.PagerResponse
import com.faithdeveloper.giveaway.data.models.Post
import com.google.firebase.firestore.DocumentSnapshot
import java.util.*

class ProfilePagingSource(val repository: Repository, private var firstTimeLoad: Boolean, private val userId: String) :
    PagingSource<PagerKey, Post>() {

    override val jumpingSupported = false
    override val keyReuseSupported = false

    override fun getRefreshKey(state: PagingState<PagerKey, Post>) = null

    override suspend fun load(params: LoadParams<PagerKey>): LoadResult<PagerKey, Post> {
        return try {
            val response = repository.getProfileFeed(params.key!!, userId).data as PagerResponse<Post>

            val nextKey = if (response.data.isNotEmpty()) {
                // data was loaded
                if (response.data.size < 10) {
                    // last set of data loaded. No new ones available
                    null
                }
                // new data still available in the database
                else PagerKey(lastSnapshot = response.lastSnapshot, filter = params.key!!.filter , loadSize = params.key!!.loadSize)
            } else {
                // no new data found
                null
            }
            LoadResult.Page(
                data = response.data,
                nextKey = nextKey,
                prevKey = null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}