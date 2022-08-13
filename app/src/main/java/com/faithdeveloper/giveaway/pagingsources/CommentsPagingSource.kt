package com.faithdeveloper.giveaway.pagingsources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.faithdeveloper.giveaway.data.models.PagerKey
import com.faithdeveloper.giveaway.data.models.CommentData
import com.faithdeveloper.giveaway.data.models.PagerResponse
import com.faithdeveloper.giveaway.data.Repository

class CommentsPagingSource(
    val repository: Repository,
    var firstTimeLoad: Boolean,
    private val postID: String
) :
    PagingSource<PagerKey, CommentData>() {
    override val jumpingSupported = false
    override val keyReuseSupported = false

    override fun getRefreshKey(state: PagingState<PagerKey, CommentData>) = null

    override suspend fun load(params: LoadParams<PagerKey>): LoadResult<PagerKey, CommentData> {
        return try {
            val response = repository.getComments(params.key!!, postID).data as PagerResponse<CommentData>
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

            val prevKey = if (firstTimeLoad) {
                firstTimeLoad = false
                null
            } else PagerKey(lastSnapshot = params.key?.lastSnapshot , filter = params.key!!.filter, loadSize = params.key!!.loadSize)
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