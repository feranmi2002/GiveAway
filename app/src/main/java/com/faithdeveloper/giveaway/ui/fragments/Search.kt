package com.faithdeveloper.giveaway.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.faithdeveloper.giveaway.*
import com.faithdeveloper.giveaway.utils.Extensions.disable
import com.faithdeveloper.giveaway.utils.Extensions.enable
import com.faithdeveloper.giveaway.utils.Extensions.launchLink
import com.faithdeveloper.giveaway.utils.Extensions.sendEmail
import com.faithdeveloper.giveaway.utils.Extensions.sendPhone
import com.faithdeveloper.giveaway.utils.Extensions.sendWhatsapp
import com.faithdeveloper.giveaway.utils.Extensions.showComments
import com.faithdeveloper.giveaway.utils.Extensions.showMedia
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.ui.adapters.FeedLoadStateAdapter
import com.faithdeveloper.giveaway.ui.adapters.FeedPagerAdapter
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.LayoutSearchBinding
import com.faithdeveloper.giveaway.ui.fragments.NewPost.Companion.VIDEO
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.getDataSavingMode
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.utils.interfaces.FragmentCommentsInterface
import com.faithdeveloper.giveaway.viewmodels.SearchVM
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Search : Fragment(), FragmentCommentsInterface {

//    init properties
    private var _binding: LayoutSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SearchVM
    private lateinit var activityObserver: ActivityObserver
    private lateinit var adapter: FeedPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        activityObserver = object : ActivityObserver() {
            override fun onResumeAction() {
                (activity as MainActivity).getRepository().setAppState(Repository.APP_STARTED)
            }

            override fun onPauseAction() {
                (activity as MainActivity).getRepository().setAppState(Repository.APP_PAUSED)
            }
            override fun onCreateAction() {

                activity?.lifecycle?.addObserver(activityObserver)
            }
        }
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        viewModel = ViewModelProvider(
            this@Search,
            VMFactory((activity as MainActivity).getRepository())
        ).get(SearchVM::class.java)
        watchSearchText()
        showTags()
        setUpAdapter()
        setUpLoadState()
        search()
        searchTextListener()
        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun showTags() {
        val chipsData = resources.getStringArray(R.array.search_options)
        binding.chipGroup.apply {
            isSelectionRequired = true
            isSingleSelection = true
            isSingleLine = true
            setOnCheckedChangeListener { group, checkedId ->
                val chip = group.findViewById<Chip>(checkedId)
                viewModel.changeFilter(chip.text.toString())
            }
            for (index in chipsData) {
                val chip = layoutInflater.inflate(
                    R.layout.checkable_chip,
                    binding.chipGroup,
                    false
                ) as Chip
                chip.text = index
                if (chip.text == viewModel.filter) chip.isChecked = true
                addView(chip)
            }
        }
    }

    private fun searchTextListener(){
        binding.search.setOnClickListener {
            val id = binding.chipGroup.get(0).id
            binding.chipGroup.check(id)
        }
    }

    private fun search() {
        binding.searchImg.setOnClickListener {
            viewModel.search(binding.search.text?.toString()!!)
        }
    }

    private fun watchSearchText() {
        disableSearchIcon()
        if (binding.search.text?.isNotEmpty() == true && binding.search.text?.isNotBlank()!!) enableSearchIcon()
        binding.search.doAfterTextChanged {
            it?.let {
                if (it.isBlank()) disableSearchIcon()
                else enableSearchIcon()
            }
        }
    }

    private fun enableSearchIcon() {
        binding.searchImg.enable()
        binding.searchImg.setImageResource(R.drawable.ic_baseline_search_24)
    }

    private fun disableSearchIcon() {
        binding.searchImg.disable()
        binding.searchImg.setImageResource(R.drawable.ic_baseline_search_disabled_24)
    }

    override fun onDestroyView() {
        _binding = null
        activity?.lifecycle?.removeObserver(activityObserver)
        super.onDestroyView()
    }

    private fun setUpAdapter() {
        adapter = FeedPagerAdapter(
            { reaction, data, posterID ->
                if (reaction == "email") {
                    if (sendEmail(data) is Event.Failure) noAppToHandleRequest()
                    return@FeedPagerAdapter
                }
                if (reaction == "whatsapp") {
                    if (sendWhatsapp(data) is Event.Failure) noAppToHandleRequest()
                    return@FeedPagerAdapter
                }
                if (reaction == "phone") {
                    if (sendPhone(data) is Event.Failure) noAppToHandleRequest()
                    return@FeedPagerAdapter
                }
                if (reaction == "comments") {
                    showComments(data, posterID, fragmentCommentsInterface = this)
                    return@FeedPagerAdapter
                }
                if (reaction == "launchLink") {
                    launchLink(data)
                    return@FeedPagerAdapter
                }

            },
            { profileName ->
                navigateToProfilePage(profileName)
            },
            { images, hasVideo, position ->
                showImages(images.toList(), hasVideo, position)
            },
            { menuAction -> }, viewModel.userUid(), requireContext().getDataSavingMode()
        )
        binding.recycler.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.recycler.adapter = adapter.withLoadStateFooter(
            FeedLoadStateAdapter() {
                adapter.retry()
            }
        )
        binding.recycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    private fun showImages(images: List<String>, hasVideo: Boolean, position:Int) {
        var array = arrayOf<String>()
        images.onEachIndexed { index, item ->
            array = array.plus(item)
        }
        val mediaType: String = if (hasVideo) {
            Feed.VIDEO
        } else {
            FullPostMediaBottomSheet.IMAGES
        }
        this.showMedia(array, mediaType, position)
    }

    private fun setUpLoadState() {
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                when (loadStates.refresh) {
                    is LoadState.Error -> {
                        // initial load failed
                        makeErrorLayoutVisible()
                        binding.errorLayout.progressCircular.makeInVisible()
                    }
                    is LoadState.Loading -> {
                        // initial load has begun
                        binding.errorLayout.progressCircular.makeVisible()
                        makeErrorLayoutInvisible()
                    }
                    is LoadState.NotLoading -> {
                        binding.refresh.isRefreshing = false
                        binding.errorLayout.progressCircular.makeInVisible()
                        makeErrorLayoutInvisible()
                        Log.i(getString(R.string.app_name), "Not loading feed")
                    }
                }

                when (loadStates.append) {
                    is LoadState.Error -> {
                        requireContext().showSnackbarShort(
                            binding.root,
                            "Failed to retrieve more feed"
                        )
                    }
                    is LoadState.Loading -> {
                        // additional load has begun
                    }
                    is LoadState.NotLoading -> {
                        if (loadStates.append.endOfPaginationReached) {
                            // all data has been loaded
                        }
                        if (loadStates.refresh is LoadState.NotLoading) {
                            // the previous load either initial or additional completed
                        }
                    }
                }
            }
        }
    }

    private fun navigateToProfilePage(poster: UserProfile) {
        findNavController().navigate(
            FeedDirections.actionHomeToProfile2()
        )
    }

    private fun makeErrorLayoutInvisible() {
        binding.errorLayout.errorText.makeInVisible()
        binding.errorLayout.retryButton.makeInVisible()
    }

    private fun makeErrorLayoutVisible() {
        binding.errorLayout.errorText.makeVisible()
        binding.errorLayout.retryButton.makeVisible()
    }

    private fun noAppToHandleRequest() {
        requireContext().showSnackbarShort(
            binding.root,
            "There is no app to handle this request on this device"
        )
    }


    companion object {
        const val PEOPLE = "people"
    }

    override fun onClick(poster: UserProfile) {
        navigateToProfilePage(poster)
    }
}