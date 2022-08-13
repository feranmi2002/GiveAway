package com.faithdeveloper.giveaway.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.LiveEvent
import com.faithdeveloper.giveaway.data.Repository
import kotlinx.coroutines.launch

class NewPostVM(private val repository: Repository) : ViewModel() {
    private var _link: String? = null
    private var _hasComments = true
    private val _newPostResult = LiveEvent<Event>()
    private val _mediaUri = mutableListOf<Uri>()
    private val _tags: HashMap<Int, Boolean> = HashMap()

    val link get() = _link
    val hasComments get() = _hasComments
    val newPostResult get()  = _newPostResult
    val mediaUri get() = _mediaUri
    val tags get() = _tags

    init {
        for (index in 0 until 8) _tags[index] = false
    }

    fun tagsSize()=
      tags.filterValues {
            it
        }.size

    fun addMedia(imageUri: Uri) {
        _mediaUri.add(imageUri)
    }

    fun setComment(boolean: Boolean) {
        _hasComments = boolean
    }

    fun getNoOfMedia() = mediaUri.size

    fun addTag(position: Int, state: Boolean) {
        _tags[position] = state
    }

    fun removeMedia(uri: Uri) {
        _mediaUri.remove(uri)
    }

    fun createNewPost(postText: String) {
        _newPostResult.postValue(Event.InProgress(null))
        viewModelScope.launch {
            _newPostResult.postValue(
                repository.uploadNewPost(postText, mediaUri, tags, hasComments, link)
            )
        }
    }

    fun addLink(newLink: String?) {
        _link = newLink
    }

    fun removeTag(index: Int) {
        _tags[index] = false
    }

    fun generatePreviouslyAddedTags() = BooleanArray(7) {
        _tags[it]!!
    }
}

