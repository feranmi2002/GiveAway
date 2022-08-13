package com.faithdeveloper.giveaway.viewmodels

import android.net.Uri
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class FeedVM(private val repository: Repository) : ViewModel(), FeedVMAndPagingSourceInterface {

    private val _profilePicUpload = LiveEvent<Event>()
    val profilePicUpload get() = this._profilePicUpload

    //    this LiveData string is used to store the new feed filter and trigger a reload based on the new filter
    private var loadFilter = MutableLiveData<String>()

    /*   this latest time stamp is used to identify the latest feed fetched by the paging adapter,
    * it is consequently used as a basis to fetch latest posts that will be unknown to the paging adapter*/
    private var latestTimeStamp: Long? = null

    /*    this snapshot is sent to the paging adapter when the adapter is refreshed
    * it informs the adapter of the last post retrieved by the latest feed getter. This is used as the basis of the
    * adapter to continue from*/
    private var preLoadedFeedLastSnapshot: DocumentSnapshot? = null

    /*This is the cached list of latest feed loaded by the latest feed getter*/
    private var preloadedLatestFeed = mutableListOf<FeedData>()

    /*This flag is used to notify the fragment that latest feeds have been loaded and cached*/
    private val _newFeedAvailableFlag = LiveEvent<Boolean>()
    val newFeedAvailableFlag get() = _newFeedAvailableFlag

    /*This job is used to load the latest feed in background*/
    private var latestFeedJob: Deferred<Event>? = null

    /*This timer is used to periodically check for latest feed from the remote database*/
    private var countDownTimer: CountDownTimer? = null

    private val _feedResult = loadFilter.distinctUntilChanged().switchMap { filter ->
        clearViewModelPreloadedData()
        countDownTimer?.cancel()
        countDownTimer = null
        latestFeedJob?.cancel()
        loadFeed(filter)
    }
    val feedResult get() = _feedResult

    private fun getLatestFeed() {
        latestTimeStamp?.run {
            var result: Event? = null
            viewModelScope.launch {
                latestFeedJob = async {
                    return@async repository.getLatestFeed(this@run, filter())
                }
                result = latestFeedJob!!.await()
                result?.let { result ->
                    if (result is Event.Success && result.data != null) {
                        val data = result.data as PagerResponse<FeedData>
                        preloadedLatestFeed.addAll(data.data)
                        preLoadedFeedLastSnapshot = data.lastSnapshot
                        latestTimeStamp = data.data.last().postData?.time!!.time
                        if (preloadedLatestFeed.size > 0) _newFeedAvailableFlag.postValue(true)
                        else _newFeedAvailableFlag.postValue(false)
                    }
                }
                countDownTimer?.start()
            }
        }
    }

    //    this is the post that was just uploaded by the user
    fun getUploadedPost() = repository.getUploadedPost()

    fun uploadProfilePicture(profilePicPath: Uri) {
        viewModelScope.launch {
            profilePicPath.let {
                this@FeedVM._profilePicUpload.postValue(repository.createProfilePicture(it))
            }
        }
    }

    fun filter() = loadFilter.value ?: DEFAULT_FILTER
    fun setLoadFilter(filter: String) {
        loadFilter.value = filter
    }

    fun checkIfNewPostAvailable() = repository.checkIfNewUploadedPostIsAvailable()
    fun makeNewUploadedPostNull() {
        repository.makeNewUploadedPostNull()
    }

    private fun loadFeed(filter: String) =
        Pager(
            config = PagingConfig(
                pageSize = 10,
                maxSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = {
                FeedPagingSource(
                    repository,
                    true,
                    preLoadedFeedLastSnapshot,
                    this,
                    preloadedLatestFeed
                )
            },
            initialKey = PagerKey(
                lastSnapshot = preLoadedFeedLastSnapshot,
                filter = filter,
                loadSize = 10,
            )
        ).liveData.cachedIn(viewModelScope)


    fun userUid() = repository.userUid()!!

    fun setProfileForProfileView(poster: UserProfile) {
        repository.setAuthorProfileForProfileView(poster)
    }

    override fun latestFeedTimestamp(timeStamp: Long) {
        latestTimeStamp = timeStamp

        if (countDownTimer == null) {
            countDownTimer = initCountDownTimer()
        }
        countDownTimer?.start()
    }

    override fun clearViewModelPreloadedData() {
        preloadedLatestFeed = mutableListOf()
        preLoadedFeedLastSnapshot = null
        _newFeedAvailableFlag.postValue(false)
    }

    private fun initCountDownTimer() = object : CountDownTimer(60000, 1000) {
        override fun onTick(p0: Long) {
//                do nothing
        }

        override fun onFinish() {
            getLatestFeed()
        }
    }

    companion object {
        const val DEFAULT_FILTER = "All"
    }


}

