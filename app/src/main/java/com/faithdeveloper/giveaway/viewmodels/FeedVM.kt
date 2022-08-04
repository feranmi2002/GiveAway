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
/*
    this flag is used to indicate that the view model has just been initialized which also means that
   the fragment has just been created
*/
    private var fragmentCreatedFlag: Boolean = true

    private val _profilePicUpload = LiveEvent<Event>()
    private var loadFilter = MutableLiveData<String>()
    private var adapterIsSetUp = false
    private var _newPostAvailable:Boolean = false
    private val _feedResult  = loadFilter.distinctUntilChanged().switchMap {  filter ->
        loadFeed(filter)
    }
    val feedResult get() = _feedResult
    val profilePicUpload get() = this._profilePicUpload

    fun fragmentHasJustBeenCreated() = fragmentCreatedFlag

    fun setFragmentCreationFlag(state:Boolean){
        fragmentCreatedFlag = state
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

