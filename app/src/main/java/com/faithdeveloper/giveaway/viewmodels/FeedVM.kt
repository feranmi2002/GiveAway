package com.faithdeveloper.giveaway.viewmodels

import android.os.CountDownTimer
import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.FeedData
import com.faithdeveloper.giveaway.data.models.PagerKey
import com.faithdeveloper.giveaway.data.models.PagerResponse
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.pagingsources.FeedPagingSource
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import com.faithdeveloper.giveaway.utils.interfaces.FeedVMAndPagingSourceInterface
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class FeedVM(private val repository: Repository) : ViewModel(), FeedVMAndPagingSourceInterface {

    private var mapOfCachedFeed: MutableMap<String, List<FeedData>> = mutableMapOf()
    private var mapOfNewUploadedPosts: MutableMap<String, MutableList<FeedData>> = mutableMapOf()
    private var mapOFLastDocumentSnapshots: MutableMap<String, DocumentSnapshot?> = mutableMapOf()
    private var mapOfLatestFeed: MutableMap<String, MutableList<FeedData>> = mutableMapOf()
    private var mapOfLatestFeedSnapshot: MutableMap<String, DocumentSnapshot?> = mutableMapOf()

    private var explicitRefresh = false

    //    this LiveData string is used to store the new feed filter and trigger a reload based on the new filter
    private var loadFilter = MutableLiveData<String>("All")

    /*   this latest time stamp is used to identify the latest feed fetched by the paging adapter,
    * it is consequently used as a basis to fetch latest posts that will be unknown to the paging adapter*/
    private var latestTimeStamp: Long? = null

    /*    this snapshot is sent to the paging adapter when the adapter is refreshed
    * it informs the adapter of the last post retrieved by the latest feed getter. This is used as the basis of the
    * adapter to continue from*/


    /*This flag is used to notify the fragment that latest feeds have been loaded and cached*/
    private val _newFeedAvailableFlag = LiveEvent<Boolean>()
    val newFeedAvailableFlag get() = _newFeedAvailableFlag

    /*This job is used to load the latest feed in background*/
    private var latestFeedJob: Deferred<Event>? = null

    /*This timer is used to periodically check for latest feed from the remote database*/
    private var countDownTimer: CountDownTimer? = null

    private val _feedResult = loadFilter.distinctUntilChanged().switchMap { filter ->
        clearViewModelPreloadedData()
        stopPreloadingLatestFeed()
        loadFeed(filter)
    }

    init {
        initMapOfCachedLoadedDataForEachFilter()
    }

    fun stopPreloadingLatestFeed() {
        countDownTimer?.cancel()
        countDownTimer = null
        latestFeedJob?.cancel()
    }

    val feedResult get() = _feedResult

    private fun getLatestFeed() {
        latestTimeStamp?.run {
            var result: Event? = null
            viewModelScope.launch(Dispatchers.IO) {
                latestFeedJob = async {
                    return@async repository.getLatestFeed(this@run, filter())
                }
                result = latestFeedJob!!.await()
                result?.let { result ->
                    if (result is Event.Success && result.data != null) {
                        val data = result.data as PagerResponse<FeedData>
                        addToLatestFeed(filter(), data.data)
                        addToLatestSnapshot(filter(), data.lastSnapshot)
                        latestTimeStamp = data.data.last().postData?.time!!.time
                        if (mapOfLatestFeed[filter()]?.size!! > 0) _newFeedAvailableFlag.postValue(
                            true
                        )
                        else _newFeedAvailableFlag.postValue(false)
                    }
                }
//                stop prefetching feed data when there is already above 30 prefetched feed items
                if (mapOfLatestFeed[filter()]!!.size < 30) countDownTimer?.start()
            }
        }
    }

    private fun addToLatestFeed(filter: String, data: List<FeedData>) {
        mapOfLatestFeed[filter]?.addAll(0, data)
    }

    fun clearLatestFeed(filter: String) {
        mapOfLatestFeed[filter] = mutableListOf()
    }

    fun getLatestFeed(filter: String) = mapOfLatestFeed[filter]

    private fun addToLatestSnapshot(filter: String, snapshot: DocumentSnapshot?) {
        mapOfLatestFeedSnapshot[filter] = snapshot
    }

    fun getLatestFeedSnapshot(filter: String) = mapOfLatestFeedSnapshot[filter]


    fun filter() = loadFilter.value ?: DEFAULT_FILTER

    fun setLoadFilter(filter: String) {
        loadFilter.value = filter
    }

    fun checkIfNewPostAvailable(): Boolean {
        //        if (state) {
//            preloadedLatestFeed.add(repository.getUploadedPost())
//            //       repository.makeNewUploadedPostNull()
//        }
        return repository.checkIfNewUploadedPostIsAvailable()
    }

    fun getUploadedPost() = repository.getUploadedPost()

    fun makeUploadedPostNull() {
        repository.makeNewUploadedPostNull()
    }

    private fun loadFeed(filter: String) =
        Pager(
            config = PagingConfig(
                pageSize = 10
            ),
            pagingSourceFactory = {
                FeedPagingSource(
                    repository,
                    true,
                    mapOfLatestFeedSnapshot[filter],
                    this,
                    mapOfLatestFeed[filter]!!
                )
            },
            initialKey = PagerKey(
                lastSnapshot = null,
                filter = filter,
                loadSize = 10,
            )
        ).liveData.cachedIn(viewModelScope)

    fun userUid() = repository.userUid()!!

    fun setProfileForProfileView(poster: UserProfile) {
        repository.setAuthorProfileForProfileView(poster)
    }


    override fun updateLatestFeedTimeStamp(timeStamp: Long) {
        latestTimeStamp = timeStamp

        if (countDownTimer == null) {
            countDownTimer = initCountDownTimer()
        }
        countDownTimer?.start()
    }

    override fun clearViewModelPreloadedData() {
        mapOfLatestFeed[filter()] = mutableListOf()
        mapOfLatestFeedSnapshot[filter()] = null
        _newFeedAvailableFlag.postValue(false)
    }

    private fun initCountDownTimer() = object : CountDownTimer(60000, 1000) {
        override fun onTick(p0: Long) {
//                do nothing
        }

        override fun onFinish() {
            //getLatestFeed()
        }
    }

    private fun initMapOfCachedLoadedDataForEachFilter() {
        repository.getTimelineOptions().onEach {
            mapOfCachedFeed[it] = listOf()
            mapOfNewUploadedPosts[it] = mutableListOf()
            mapOFLastDocumentSnapshots[it] = null
            mapOfLatestFeed[it] = mutableListOf()
            mapOfLatestFeedSnapshot[it] = null
        }
    }

    fun cacheLoadedData(filter: String, snapshot: List<FeedData>) {
        mapOfCachedFeed[filter] = snapshot
    }

    fun clearCachedLoadedData(filter: String) {
        mapOfCachedFeed[filter] = mutableListOf()
    }

    fun cacheNewUploadedPost(filter: List<String>, uploadedPost: FeedData) {
        mapOfNewUploadedPosts[DEFAULT_FILTER]!!.add(0, uploadedPost)
        filter.onEach {
            mapOfNewUploadedPosts[it]!!.add(0, uploadedPost)
        }
    }

    fun clearCachedNewUploadedPosts(filter: String) {
        mapOfNewUploadedPosts[filter] = mutableListOf()
    }

    fun getCachedUploadedNewPosts(filter: String) = mapOfNewUploadedPosts[filter]

    override fun requestCachedData(filter: String): List<FeedData>? = mapOfCachedFeed[filter]

    override fun storeLastSnapshot(filter: String, snapshot: DocumentSnapshot?) {
        mapOFLastDocumentSnapshots[filter] = snapshot
    }

    override fun getLastSnapshot(filter: String): DocumentSnapshot? =
        mapOFLastDocumentSnapshots[filter]

    fun setExplicitRefresh(value: Boolean) {
        explicitRefresh = value
    }

//    override fun getExplicitRefresh() = explicitRefresh
//
//    override fun mSetExplicitRefresh(value: Boolean) {
//        explicitRefresh = value
//    }

    override fun clearCachedData(filter: String) {
        clearCachedLoadedData(filter)
    }

    override fun getCurrentFilter() = loadFilter.value ?: DEFAULT_FILTER

    companion object {
        const val DEFAULT_FILTER = "All"
    }
}

