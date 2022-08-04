package com.faithdeveloper.giveaway.viewmodels

import android.net.Uri
import androidx.lifecycle.*
import androidx.paging.*
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.data.models.FeedPagerKey
import com.faithdeveloper.giveaway.utils.LiveEvent
import com.faithdeveloper.giveaway.data.models.FeedData
import com.faithdeveloper.giveaway.pagingsources.FeedPagingSource
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.UserProfile
import kotlinx.coroutines.launch

class FeedVM(private val repository: Repository) : ViewModel() {
    private val _profilePicUpload = LiveEvent<Event>()
    private var loadFilter = MutableLiveData(DEFAULT_FILTER)
    private var adapterIsSetUp = false
    private var _newPostAvailable:Boolean = false
    private val _feedResult  = loadFilter.switchMap {  filter ->
        loadFeed(filter)
    }
    val feedResult get() = _feedResult
    val profilePicUpload get() = this._profilePicUpload


    //    this is the post that was just uploaded by the user
    fun getUploadedPost() = repository.getUploadedPost()

    fun uploadProfilePicture(profilePicPath: Uri) {
        viewModelScope.launch {
            profilePicPath.let {
                this@FeedVM._profilePicUpload.postValue(repository.createProfilePicture(it))
            }
        }
    }

    fun filter() = loadFilter
    fun setLoadFilter(filter:String){
        loadFilter.value = filter
    }

    fun newPostAvailable() = _newPostAvailable
    fun setNewPostAvailable(state:Boolean){
        _newPostAvailable = state
    }

    private fun loadFeed(filter: String) =
        Pager(
            config = PagingConfig(
                pageSize = 10,
                maxSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ), pagingSourceFactory = {
                FeedPagingSource(repository, true)
            }, initialKey = FeedPagerKey(lastSnapshot = null, filter = filter, loadSize = 10)
        ).liveData.cachedIn(viewModelScope)


    fun userUid() = repository.userUid()!!

    fun adapterIsSetUp() = adapterIsSetUp
    fun updateAdapterState(isSetUp:Boolean) {
        adapterIsSetUp = isSetUp
    }

    fun setProfileForProfileView(poster: UserProfile) {
        repository.setAuthorProfileForProfileView(poster)
    }

    companion object{
        const val DEFAULT_FILTER = "All"
    }
}

