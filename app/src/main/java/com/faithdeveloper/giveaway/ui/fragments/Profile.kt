package com.faithdeveloper.giveaway.ui.fragments

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.models.UserProfile
import com.faithdeveloper.giveaway.databinding.LayoutProfileBinding
import com.faithdeveloper.giveaway.ui.adapters.FeedLoadStateAdapter
import com.faithdeveloper.giveaway.ui.adapters.ProfilePagerAdapter
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.checkTypeOfMedia
import com.faithdeveloper.giveaway.utils.Extensions.getSignInStatus
import com.faithdeveloper.giveaway.utils.Extensions.getUserProfilePicUrl
import com.faithdeveloper.giveaway.utils.Extensions.launchLink
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
import com.faithdeveloper.giveaway.viewmodels.ProfileVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.properties.Delegates

class Profile : Fragment() {
    //    init properties
    private lateinit var viewModel: ProfileVM
    private lateinit var activityObserver: ActivityObserver
    private lateinit var adapter: ProfilePagerAdapter
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var alertDialog: AlertDialog? = null
    private var _binding: LayoutProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var profile: UserProfile
    private var newSignIn by Delegates.notNull<Boolean>()
    private lateinit var cropImage: ActivityResultLauncher<CropImageContractOptions>
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>

    //    this is a flag used to know whether to load the profile of the user or of the author of a post.
    private var userProfile = true

    override fun onCreate(savedInstanceState: Bundle?) {

        activityObserver = object : ActivityObserver() {
            override fun onResumeAction() {
                (activity as MainActivity).getRepository().setAppState(Repository.APP_STARTED)
            }

            override fun onPauseAction() {
                (activity as MainActivity).getRepository().setAppState(Repository.APP_PAUSED)
            }

            override fun onCreateAction() {
                viewModel = ViewModelProvider(
                    this@Profile,
                    VMFactory(
                        repository = (requireActivity() as MainActivity).getRepository(),
                        getUserProfile = requireArguments().getBoolean("userProfile")
                    )
                ).get(ProfileVM::class.java)
            }
        }

        setUpAdapter()
        userProfile = requireArguments().getBoolean("userProfile")

        activity?.lifecycle?.addObserver(activityObserver)

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
        super.onCreate(savedInstanceState)
}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setUpLoadState()
        handleCreateProfilePicDialog()
        handleObserver()
        handleViewPresentation()
        onClickSettings()
        onClickEdit()
        onClickNewPost()
        onClickBack()
        onClickProfileImage()
        onClickRetry()
        binding.recycler.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.recycler.adapter = adapter.withLoadStateFooter(
            FeedLoadStateAdapter {
                adapter.retry()
            }
        )
        binding.recycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        super.onViewCreated(view, savedInstanceState)
    }

    private fun onClickRetry() {
        binding.errorLayout.retryButton.setOnClickListener {
            adapter.retry()
        }
    }

    private fun onClickBack() {
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun onClickNewPost() {
        binding.fab.setOnClickListener {
            findNavController().navigate(ProfileDirections.actionProfileToNewpost())
        }
    }

    private fun onClickProfileImage() {
        binding.profilePic.setOnClickListener {
            showImages(listOf(profile.profilePicUrl), false, 0)
        }
    }

    private fun handleViewPresentation() {
        binding.edit.isGone = !userProfile
        binding.settings.isGone = !userProfile
        if (userProfile) {
//            user wants to check his/her profile
            loadView(viewModel.getUserProfile())
        } else {
//            user is checking the profile of another user (author of a post)
            loadView(viewModel.getAuthorProfile())
        }
    }

    private fun loadView(profile: UserProfile) {
        this.profile = profile
//        load the views with their data
        binding.apply {
            Glide.with(this@Profile)
                .load(profile.profilePicUrl)
                .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
                .into(profilePic)
            profileName.text = profile.name
        }
        binding.email.setOnClickListener {
            if (sendEmail(profile.email) is Event.Failure) noAppToHandleRequest()
        }
        binding.phone.setOnClickListener {
            if (sendPhone(profile.phoneNumber) is Event.Failure) noAppToHandleRequest()
        }
        binding.whatsapp.setOnClickListener {
            if (sendWhatsapp(profile.phoneNumber) is Event.Failure) noAppToHandleRequest()
        }
    }

    //    informs user of no app on the device to handle intended action
    private fun noAppToHandleRequest() {
        requireContext().showSnackbarShort(
            binding.root,
            "There is no app to handle this request on this device"
        )
    }

    private fun setUpAdapter() {
        adapter = ProfilePagerAdapter(
            { reaction, data, commentCount ->
                if (reaction == "comments") {
                    showComments(data, commentCount, null)
                    return@ProfilePagerAdapter
                }
                if (reaction == "launchLink") {
                    launchLink(data)
                    return@ProfilePagerAdapter
                }

            }, { images, hasVideo, position ->
                showImages(images.toList(), hasVideo, position)
            }, { menuAction -> }, (activity as MainActivity).getRepository().getUserProfile()
        )
    }

    private fun showImages(images: List<String>, hasVideo: Boolean, position: Int) {
        var array = arrayOf<String>()
        images.onEachIndexed { index, item ->
            array = array.plus(item)
        }
        val mediaType: String = if (hasVideo) {
            Feed.VIDEO
        } else {
            FullPostMediaBottomSheet.IMAGE
        }
        this.showMedia(array, mediaType, position)
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


    private fun handleObserver() {
        viewModel.feedResult.observe(viewLifecycleOwner, Observer {
            adapter.submitData(viewLifecycleOwner.lifecycle, it)
        })
        //    observe result of profile picture upload
        viewModel.profilePicUpload.observe(viewLifecycleOwner) {
            when (it) {
                is Event.Success -> {
                    requireContext().showSnackbarShort(binding.root, "Your profile picture has been uploaded")
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


    private fun onClickSettings() {
        binding.settings.setOnClickListener {
            findNavController().navigate(ProfileDirections.actionProfileToSettingsFragment())
        }
    }

    private fun onClickEdit() {
        binding.edit.setOnClickListener {
            findNavController().navigate(ProfileDirections.actionProfileToProfileEdit())
        }
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

    private fun makeEmptyResultLayoutVisible() {
        makeErrorLayoutInvisible()
        binding.errorLayout.progressCircular.makeInVisible()
        binding.emptyResultLayout.emptyText.makeVisible()
        binding.emptyResultLayout.emptyImg.makeVisible()
    }

    private fun makeEmptyResultLayoutInvisible() {
        binding.emptyResultLayout.emptyText.makeInVisible()
        binding.emptyResultLayout.emptyImg.makeInVisible()
    }

    private fun setUpLoadState() {
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                if (loadStates.source.refresh is LoadState.NotLoading && loadStates.append.endOfPaginationReached && adapter.itemCount == 0) {
                    makeEmptyResultLayoutVisible()
                } else {
                    makeEmptyResultLayoutInvisible()
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
                            binding.errorLayout.progressCircular.makeInVisible()
                            makeErrorLayoutInvisible()
                            Log.i(
                                getString(com.faithdeveloper.giveaway.R.string.app_name),
                                "Not loading feed"
                            )
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
                setOutputCompressQuality(80)
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
}