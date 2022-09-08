package com.faithdeveloper.giveaway.pagingsources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.PagerKey
import com.faithdeveloper.giveaway.data.models.PagerResponse
import com.faithdeveloper.giveaway.data.models.ReplyData

class RepliesPagingSource(
    val repository: Repository,
    private val parentID: String,
    private val commentID: String
) :
    PagingSource<PagerKey, ReplyData>() {
    override val jumpingSupported = false
    override val keyReuseSupported = false

    override fun getRefreshKey(state: PagingState<PagerKey, ReplyData>) = null

    override suspend fun load(params: LoadParams<PagerKey>): LoadResult<PagerKey, ReplyData> {
        return try {
            val response = repository.getReplies(
                params.key!!,
                parentID,
                commentID
            ).data as PagerResponse<ReplyData>
            val nextKey = if (response.data.isNotEmpty()) {
                // data was loaded
                if (response.data.size < 10) {
                    // last set of data loaded. No new ones available
                    null
                }
                // new data still available in the database
                else PagerKey(
                    lastSnapshot = response.lastSnapshot,
                    filter = params.key!!.filter,
                    loadSize = params.key!!.loadSize
                )
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