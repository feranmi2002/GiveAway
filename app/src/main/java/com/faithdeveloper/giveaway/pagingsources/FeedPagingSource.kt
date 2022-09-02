package com.faithdeveloper.giveaway.pagingsources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.FeedData
import com.faithdeveloper.giveaway.data.models.PagerKey
import com.faithdeveloper.giveaway.data.models.PagerResponse
import com.faithdeveloper.giveaway.utils.interfaces.FeedVMAndPagingSourceInterface
import com.google.firebase.firestore.DocumentSnapshot

class FeedPagingSource(
    val repository: Repository,
    var firstTimeLoad: Boolean,
    private val preloadedFeedLastSnapshot: DocumentSnapshot?,
    private val viewmodelCallbackInterface: FeedVMAndPagingSourceInterface,
    private val preloadedLatestFeed: MutableList<FeedData>
) :
    PagingSource<PagerKey, FeedData>() {
    private var responseFromCachedLatestFeed = false
    override val jumpingSupported = false
    override val keyReuseSupported = false

    override fun getRefreshKey(state: PagingState<PagerKey, FeedData>) = null

    override suspend fun load(params: LoadParams<PagerKey>): LoadResult<PagerKey, FeedData> {
        return try {
            val response: PagerResponse<FeedData>
            if (firstTimeLoad && preloadedLatestFeed.size > 0) {
//                paging source is refreshed but there is already a preloaded data by the feed view model
                response = PagerResponse(preloadedLatestFeed, preloadedFeedLastSnapshot)
                viewmodelCallbackInterface.clearViewModelPreloadedData()
                responseFromCachedLatestFeed = true
            } else {
                response = repository.getFeed(params.key!!).data as PagerResponse<FeedData>
                responseFromCachedLatestFeed = false
            }

            val nextKey = if (response.data.isNotEmpty()) {
                // data was loaded

                if (firstTimeLoad && !responseFromCachedLatestFeed) viewmodelCallbackInterface.latestFeedTimestamp(
                    response.data.first().postData?.time!!.time
                )
                var result: PagerKey? = null
                if (response.data.size < params.key!!.loadSize) {
                    if (responseFromCachedLatestFeed) {
                        result = PagerKey(
                            lastSnapshot = preloadedFeedLastSnapshot,
                            filter = params.key!!.filter,
                            loadSize = params.key!!.loadSize
                        )
                    }
                }else{
                    if (responseFromCachedLatestFeed) {
                        result = PagerKey(
                            lastSnapshot = preloadedFeedLastSnapshot,
                            filter = params.key!!.filter,
                            loadSize = params.key!!.loadSize
                        )
                    }else{
                        result =  PagerKey(
                            lastSnapshot = response.lastSnapshot,
                            filter = params.key!!.filter,
                            loadSize = params.key!!.loadSize
                        )
                    }
                }
                result
            } else {
                // no new data found
                null
            }
            firstTimeLoad = false
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