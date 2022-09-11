package com.faithdeveloper.giveaway.ui.fragments

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.Repository.Companion.APP_PAUSED
import com.faithdeveloper.giveaway.data.Repository.Companion.APP_STARTED
import com.faithdeveloper.giveaway.data.models.FeedData
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.LayoutFeedBinding
import com.faithdeveloper.giveaway.ui.adapters.FeedLoadStateAdapter
import com.faithdeveloper.giveaway.ui.adapters.FeedPagerAdapter
import com.faithdeveloper.giveaway.ui.adapters.NewFeedAdapter
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.checkTypeOfMedia
import com.faithdeveloper.giveaway.utils.Extensions.getDataSavingMode
import com.faithdeveloper.giveaway.utils.Extensions.getSignInStatus
import com.faithdeveloper.giveaway.utils.Extensions.getUserProfilePicUrl
import com.faithdeveloper.giveaway.utils.Extensions.launchLink
import com.faithdeveloper.giveaway.utils.Extensions.makeGone
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.Extensions.sendEmail
import com.faithdeveloper.giveaway.utils.Extensions.sendPhone
import com.faithdeveloper.giveaway.utils.Extensions.sendWhatsapp
import com.faithdeveloper.giveaway.utils.Extensions.setSignInStatus
import com.faithdeveloper.giveaway.utils.Extensions.showComments
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showMedia
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.utils.interfaces.FragmentCommentsInterface
import com.faithdeveloper.giveaway.viewmodels.FeedVM
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.properties.Delegates

class Feed : Fragment(), FragmentCommentsInterface {
    // init properties
    private lateinit var cropImage: ActivityResultLauncher<CropImageContractOptions>
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>
    private lateinit var adapter: FeedPagerAdapter
    private lateinit var mAdapter: NewFeedAdapter
    private lateinit var viewModel: FeedVM
    private lateinit var activityObserver: ActivityObserver
    private var _binding: LayoutFeedBinding? = null
    private val binding get() = _binding!!
    private var newSignIn by Delegates.notNull<Boolean>()
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var alertDialog: AlertDialog? = null
    private var newFeed = mutableListOf<FeedData>()


    override fun onCreate(savedInstanceState: Bundle?) {
        /*
      * activityObserver observes the lifecycle of the activity and performs corresponding actions
      * */

        activityObserver = object : ActivityObserver() {
            override fun onResumeAction() {
                (activity as MainActivity).getRepository().setAppState(APP_STARTED)
            }

            override fun onPauseAction() {
                (activity as MainActivity).getRepository().setAppState(APP_PAUSED)
            }

            override fun onCreateAction() {
//                initialize view model
                viewModel = ViewModelProvider(
                    this@Feed,
                    VMFactory((activity as MainActivity).getRepository())
                ).get(FeedVM::class.java)

                setUpAdapter()

                /* 'newSignIn' is true only when user has signed in to a new device
                * for the first time and has not set a previous profile picture
     * */
                newSignIn =
                    requireContext().getSignInStatus() && requireContext().getUserProfilePicUrl() == null

//                initialize third party library used for taking and cropping user profile picture
                cropImage = registerForActivityResult(CropImageContract()) { result ->
                    if (result.isSuccessful) {
                        // use the returned uri
                        result?.let {
                            /*Check if file chosen is of type image*/
                            if (requireContext().checkTypeOfMedia(
                                    convertFilePathToUri(
                                        result.getUriFilePath(
                                            requireContext(),
                                            true
                                        )!!
                                    )
                                ) == "video"
                            ) {
//                                wrong file type chosen
                                wrongFileChosen()
                            } else {
//                                correct file type chosen, upload profile picture in background
                                requireContext().showSnackbarShort(
                                    binding.root,
                                    "Uploading profile picture"
                                )
                                result.getUriFilePath(requireContext(), true)?.let {
                                    viewModel.uploadProfilePicture(
                                        convertFilePathToUri(it)
                                    )
                                }
                            }
                        }
                    } else {
//                        failed to upload profile picture
                        requireContext().showSnackbarShort(
                            binding.root,
                            "Profile picture upload failed. Try again."
                        )
                    }
                }
//                setup permissions for taking picture
                requestMultiplePermissions = registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    permissions.entries.forEach {
                        if (!it.value) {
//                           permissions not granted
                            permissionDenied()
                            return@registerForActivityResult
                        }
                    }
//                    permissions granted
                    choosePicture()
                }
            }
        }
//        add lifecycle observer
        activity?.lifecycle?.addObserver(activityObserver)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadProfilePicture()
        handleObservers()
        clickListeners()

//        this is used to load the latest that been pre loaded in the background
        handleLatestFeed()

//        this is used to ask user to upload a profile picture if no profile picture is detected
        handleCreateProfilePicDialog()

//        set up recycler
        binding.recycler.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.recycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val concatAdapter = ConcatAdapter()
        concatAdapter.addAdapter(mAdapter)
        concatAdapter.addAdapter(adapter.withLoadStateFooter(
            FeedLoadStateAdapter() { adapter.retry() }
        ))
        binding.recycler.adapter = concatAdapter
        setUpLoadState()
        handleRefresh()
        setUpTags()
        super.onViewCreated(view, savedInstanceState)
    }


    override fun onStart() {
        //        check if a new post has just been uploaded by the user, then display it if true
        viewModel.checkIfNewPostAvailable().run {
            //   binding.latestFeed.isVisible = this
            if (this) {
                requireContext().showSnackbarShort(binding.root, "Your Post has been sent.")
                newFeed.add(viewModel.getUploadedPost())
                viewModel.storeSizeOfNewFeed(newFeed.size)
                mAdapter.notifyItemInserted(0)
            }
        }
        super.onStart()
    }


    private fun loadProfilePicture() {
        Glide.with(this)
            .load(requireContext().getUserProfilePicUrl())
            .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
            .into(binding.profile)
    }

    /*Tags can be added to Posts to help segment the posts.
         * User can view feed of posts under a particular tag*/
    private fun setUpTags() {
        val chipsData = resources.getStringArray(R.array.feedTags)
        binding.chipGroup.apply {
            isSelectionRequired = true
            isSingleSelection = true
            isSingleLine = true
            setOnCheckedStateChangeListener { group, checkedIds ->
//              reload feed on tag selected changed

//                first clear existing data
                binding.refresh.isRefreshing = false
                adapter.submitData(viewLifecycleOwner.lifecycle, PagingData.empty())
                binding.recycler.removeAllViewsInLayout()

                val chip: Chip? = group.findViewById<Chip>(checkedIds[0])
                newFeed = mutableListOf()
                mAdapter.notifyItemRangeRemoved(0, viewModel.newFeed)
//                this triggers a reload of data from remote database with the filter
                viewModel.setLoadFilter((chip?.text.toString()) ?: viewModel.filter())
            }
//            init chips
            for (text in chipsData) {
                val chip = layoutInflater.inflate(
                    R.layout.checkable_chip,
                    binding.chipGroup,
                    false
                ) as Chip
                chip.text = text
                if (chip.text == viewModel.filter()) chip.isChecked = true
                addView(chip)
            }
        }
    }

    // this is done because seems there is a problem (bug) with the output uri of the crop library
    private fun convertFilePathToUri(path: String) = File(path).toUri()

    //    handles permissions not granted by user
    private fun permissionDenied() {
        alertDialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = getString(R.string.permission_denied),
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        alertDialog = dialogBuilder?.create()
        alertDialog?.show()
    }

    private fun clickListeners() {
        binding.apply {
            newpost.setOnClickListener {
//                navigate to new post fragment
                findNavController().navigate(FeedDirections.actionHomeToNeed2())
            }
            profile.setOnClickListener {
//                navigate to user profile fragment
                findNavController().navigate(FeedDirections.actionHomeToProfile2(userProfile = true))
            }
            search.setOnClickListener {
//                navigate to search fragment
                findNavController().navigate(FeedDirections.actionHome2ToSearch2())
            }
            errorLayout.retryButton.setOnClickListener {
//                retry load of feed
                adapter.retry()
            }
        }
    }

    //    refreshes the feed
    private fun handleRefresh() {
        binding.refresh.setOnRefreshListener {
            binding.latestFeed.makeGone()
            viewModel.clearViewModelPreloadedData()
            viewModel.stopPreloadingLatestFeed()
            adapter.refresh()
        }
    }

    //    observes livedata
    private fun handleObservers() {
//    observe the feed result
        viewModel.feedResult.observe(viewLifecycleOwner) {
            if (binding.errorLayout.progressCircular.isVisible && newFeed.size > 0) {
                newFeed = mutableListOf()
                mAdapter.notifyItemRangeRemoved(0, viewModel.newFeed)
            }
            adapter.submitData(viewLifecycleOwner.lifecycle, it)
        }

//        observe latest feed result
        viewModel.newFeedAvailableFlag.observe(viewLifecycleOwner) {
            if (it) {
                binding.latestFeed.makeVisible()
            } else {
                binding.latestFeed.makeGone()
            }
        }
//    observe result of profile picture upload
        viewModel.profilePicUpload.observe(viewLifecycleOwner) {
            when (it) {
                is Event.Success -> {
                    // do nothing
                }
                is Event.Failure -> {
                    handleProfilePicUpdateFailure()
                }
                is Event.InProgress -> {
                    // do nothing
                }
            }
        }
    }

    private fun handleLatestFeed() {
        binding.latestFeed.setOnClickListener {
            binding.latestFeed.makeGone()
            adapter.refresh()
        }
    }

    private fun handleProfilePicUpdateFailure() {
        alertDialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            title = "Profile Picture Update",
            message = "Profile picture update failed. Try again later",
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        alertDialog = dialogBuilder?.create()
        alertDialog?.show()
    }

    private fun handleCreateProfilePicDialog() {
        if (newSignIn) {
            if (requireContext().getUserProfilePicUrl() == null) {
                alertDialog?.dismiss()
                dialogBuilder = requireContext().showDialog(
                    cancelable = false,
                    title = "Add a profile picture",
                    message = "Adding a profile picture helps people recognize you easily. Would you like to? ",
                    positiveButtonText = "YES",
                    positiveAction = {
                        askPermissions()
                    },
                    negativeButtonText = "LATER",
                    negativeAction = {
                        //do nothing
                    }
                )
                alertDialog = dialogBuilder?.create()
                alertDialog?.show()
                requireContext().setSignInStatus(false)
                newSignIn = false
            }
        }
    }

    private fun askPermissions() {
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }

    private fun choosePicture() {
        alertDialog?.dismiss()
        cropImage.launch(
            options {
                setGuidelines(CropImageView.Guidelines.ON)
                setCropShape(CropImageView.CropShape.OVAL)
                setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                setImageSource(includeGallery = true, includeCamera = true)
            }
        )
    }

    private fun wrongFileChosen() {
        alertDialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            title = "Wrong file chosen",
            message = "Please choose image file",
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        alertDialog = dialogBuilder?.create()
        alertDialog?.show()
    }

    override fun onDestroyView() {
        binding.recycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        activity?.lifecycle?.removeObserver(activityObserver)
        super.onDestroy()
    }

    //    informs user of no app on the device to handle intended action
    private fun noAppToHandleRequest() {
        requireContext().showSnackbarShort(
            binding.root,
            "There is no app to handle this request on this device"
        )
    }

    private fun setUpAdapter() {
        adapter = FeedPagerAdapter(
            { action, data,  commentCount ->
                if (action == "email") {
                    if (sendEmail(data) is Event.Failure) noAppToHandleRequest()
                    return@FeedPagerAdapter
                }
                if (action == "whatsapp") {
                    if (sendWhatsapp(data) is Event.Failure) noAppToHandleRequest()
                    return@FeedPagerAdapter
                }
                if (action == "phone") {
                    if (sendPhone(data) is Event.Failure) noAppToHandleRequest()
                    return@FeedPagerAdapter
                }
                if (action == "comments") {
                    showComments(data, commentCount, fragmentCommentsInterface = this)
                    return@FeedPagerAdapter
                }
                if (action == "launchLink") {
                    launchLink(data)
                    return@FeedPagerAdapter
                }
            },
            { profile ->
                navigateToProfilePage(profile)
            },
            { images, hasVideo, position ->
                showImages(images.toList(), hasVideo, position)
            },
            { menuAction -> }, viewModel.userUid(),
            requireContext().getDataSavingMode()
        )
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        mAdapter = NewFeedAdapter(
            { action, data, commentCount ->
                if (action == "email") {
                    if (sendEmail(data) is Event.Failure) noAppToHandleRequest()
                    return@NewFeedAdapter
                }
                if (action == "whatsapp") {
                    if (sendWhatsapp(data) is Event.Failure) noAppToHandleRequest()
                    return@NewFeedAdapter
                }
                if (action == "phone") {
                    if (sendPhone(data) is Event.Failure) noAppToHandleRequest()
                    return@NewFeedAdapter
                }
                if (action == "comments") {
                    showComments(data, commentCount, fragmentCommentsInterface = this)
                    return@NewFeedAdapter
                }
                if (action == "launchLink") {
                    launchLink(data)
                    return@NewFeedAdapter
                }
            }, { profileName ->
                navigateToProfilePage(profileName)
            },
            { images, hasVideo, position ->
                showImages(images.toList(), hasVideo, position)
            },
            { menuAction -> }, viewModel.userUid(),
            newFeed,
            requireContext().getDataSavingMode()
        )
    }

    private fun showImages(images: List<String>, hasVideo: Boolean, position: Int) {
        var array = arrayOf<String>()
        images.onEachIndexed { index, item ->
            array = array.plus(item)
        }
        val mediaType: String = if (hasVideo) {
            VIDEO
        } else {
            FullPostMediaBottomSheet.IMAGES
        }
        this.showMedia(array, mediaType, position)
    }

    private fun setUpLoadState() {
        lifecycleScope.launch {
            //                        check any result is returned and show appropriate view
            adapter.loadStateFlow.collectLatest { loadStates ->
                if (loadStates.source.refresh is LoadState.NotLoading && loadStates.append.endOfPaginationReached && adapter.itemCount == 0) {
                    makeEmptyResultLayoutVisible()
                } else {
                    makeEmptyResultLayoutInvisible()
                    when (loadStates.refresh) {
                        is LoadState.Error -> {
                            // initial load failed
                            binding.refresh.isRefreshing = false
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
    }

    private fun navigateToProfilePage(author: UserProfile) {
        viewModel.setProfileForProfileView(author)
        findNavController().navigate(
            FeedDirections.actionHomeToProfile2(userProfile = author.id == viewModel.userUid())
        )
    }

    private fun makeErrorLayoutInvisible() {
        binding.errorLayout.errorText.makeInVisible()
        binding.errorLayout.retryButton.makeInVisible()
        binding.errorLayout.errorImage.makeInVisible()
    }

    private fun makeErrorLayoutVisible() {
        makeEmptyResultLayoutInvisible()
        binding.errorLayout.errorText.makeVisible()
        binding.errorLayout.retryButton.makeVisible()
        binding.errorLayout.errorImage.makeVisible()
    }

    companion object {
        const val ADAPTER_STATE = "adapterState"
        const val VIDEO = "video"
    }

    override fun onClick(poster: UserProfile) {
        navigateToProfilePage(poster)
    }

    private fun makeEmptyResultLayoutVisible() {
        makeErrorLayoutInvisible()
        binding.refresh.isRefreshing = false
        binding.errorLayout.progressCircular.makeInVisible()
        binding.emptyResultLayout.emptyText.makeVisible()
        binding.emptyResultLayout.emptyImg.makeVisible()
    }

    private fun makeEmptyResultLayoutInvisible() {
        binding.emptyResultLayout.emptyText.makeInVisible()
        binding.emptyResultLayout.emptyImg.makeInVisible()
    }
}