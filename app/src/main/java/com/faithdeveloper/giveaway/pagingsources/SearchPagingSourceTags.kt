package com.faithdeveloper.giveaway.pagingsources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.faithdeveloper.giveaway.data.models.FirebaseModelFeed
import com.faithdeveloper.giveaway.data.models.FeedData
import com.faithdeveloper.giveaway.data.Repository
import com.google.firebase.firestore.DocumentSnapshot

class SearchPagingSourceTags(val repository: Repository, var firstTimeLoad: Boolean, val searchKeyword:String, val tag:String) :
    PagingSource<DocumentSnapshot, FeedData>() {
    override val jumpingSupported = false
    override val keyReuseSupported = false

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, FeedData>) = null

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, FeedData> {
        return try {
            val response = repository.searchByTags(params.key, searchKeyword, tag).data as List<FirebaseModelFeed>
            val nextKey = if (response.isNotEmpty()) {
                if (response.size < 30) null
                else response.last().postData
            } else null

            val prevKey = if (firstTimeLoad) {
                firstTimeLoad = false
                null
            }
            else response.first().postData
            val formattedResponse = repository.formatFeedResponse(response)
            LoadResult.Page(
                data = formattedResponse,
                nextKey = nextKey,
                prevKey = prevKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}