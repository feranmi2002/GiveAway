package com.faithdeveloper.giveaway.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.ui.fragments.Search.Companion.PEOPLE
import com.faithdeveloper.giveaway.pagingsources.SearchPagingSourceTags

class SearchVM(val repository: Repository) : ViewModel() {
    private var _filter: String = PEOPLE
    val filter get() = _filter
    private val searchString = MutableLiveData<String>()
    val result = searchString.switchMap {
        loadSearchTag(it, filter)
    }

    fun changeFilter(filter: String) {
        _filter = filter
    }

    fun search(keyword: String) {
        searchString.value = keyword
    }

    private fun loadSearchTag(keyword: String, filter: String) = Pager(
        config = PagingConfig(
            pageSize = 15,
            maxSize = 30,
            enablePlaceholders = false,
            prefetchDistance = 5
        ), pagingSourceFactory = {
            SearchPagingSourceTags(repository, true, keyword, filter)
        }, initialKey = null
    ).liveData.cachedIn(viewModelScope)

    fun userUid() = repository.userUid()!!
}