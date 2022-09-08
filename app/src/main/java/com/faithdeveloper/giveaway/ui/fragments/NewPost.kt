package com.faithdeveloper.giveaway.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.databinding.LayoutPostBinding
import com.faithdeveloper.giveaway.databinding.LinkLayoutBinding
import com.faithdeveloper.giveaway.ui.adapters.NewPostMediaAdapter
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.checkTypeOfMedia
import com.faithdeveloper.giveaway.utils.Extensions.disable
import com.faithdeveloper.giveaway.utils.Extensions.enable
import com.faithdeveloper.giveaway.utils.Extensions.hideKeyboard
import com.faithdeveloper.giveaway.utils.Extensions.mediaSize
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.viewmodels.NewPostVM
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import kotlin.properties.Delegates

@SuppressLint("NotifyDataSetChanged")
class NewPost : Fragment() {
    // init properties
    private var _binding: LayoutPostBinding? = null
    private val binding get() = _binding!!
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var dialog: AlertDialog? = null
    private var videoChosen by Delegates.notNull<Boolean>()
    private var imageChosen by Delegates.notNull<Boolean>()

    //    'imageMax' is used to know that max number of images for a post have been selected
    private var imageMax by Delegates.notNull<Boolean>()

    private lateinit var viewModel: NewPostVM
    private lateinit var activityObserver: ActivityObserver
    private lateinit var getImageFromGallery: ActivityResultLauncher<Intent>
    private lateinit var getImageFromCamera: ActivityResultLauncher<CropImageContractOptions>
    private lateinit var getVideoFromGallery: ActivityResultLauncher<Intent>
    private lateinit var getVideoFromCamera: ActivityResultLauncher<Intent>
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>

    private lateinit var mediaRecycler: NewPostMediaAdapter
    private lateinit var mediaAction: String

    private var shouldInterceptOnBackPressed = false


    override fun onCreate(savedInstanceState: Bundle?) {
        /* activityObserver observes the lifecycle of the activity and performs corresponding actions */
        activityObserver = object : ActivityObserver() {
            override fun onResumeAction() {
                (activity as MainActivity).getRepository().setAppState(Repository.APP_STARTED)
            }

            override fun onPauseAction() {
                (activity as MainActivity).getRepository().setAppState(Repository.APP_PAUSED)
            }


            override fun onCreateAction() {
                // get view model
                viewModel = ViewModelProvider(
                    this@NewPost,
                    VMFactory((activity as MainActivity).getRepository())
                ).get(NewPostVM::class.java)
                savedInstanceState.let {
//                    get saved key values to setup view states
                    if (it == null) {
                        videoChosen = false
                        imageChosen = false
                    } else {
                        videoChosen = it.getBoolean(VIDEO_CHOSEN, false)
                        imageChosen = it.getBoolean(IMAGE_CHOSEN, false)
                    }
                }


//                set up callback  for choosing image from gallery
                getImageFromGallery =
                    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
//                            inform adapter that the file chosen is an image so as to set up the view correctly
                            mediaRecycler.changeMediaType(IMAGE)
                            val data = result.data
                            if (data!!.clipData != null) {
                                // picked multiple images
                                val count = data.clipData!!.itemCount
                                for (index in 0 until count) {
                                    if (viewModel.getNoOfMedia() <= MAX_NUMBER_OF_IMAGES) {
//                                        ensure each file chosen is an image else give error
                                        if (requireContext().checkTypeOfMedia(
                                                (data.clipData!!.getItemAt(
                                                    index
                                                ).uri)
                                            ) != "image"
                                        ) {
                                            wrongFileChosen("image")
                                            break
                                        } else {
//                                            ensure each image chosen is not above the max file size
                                            if (mediaSize(data.clipData!!.getItemAt(index).uri) > MAX_IMAGE_SIZE) {
                                                fileSizeToBig(IMAGE)
                                                break
                                            } else {
                                                viewModel.addMedia(data.clipData!!.getItemAt(index).uri)
                                                imageChosen = true
                                                videoChosen = false
                                            }
                                        }
                                    } else {
                                        imageMax = true
                                        requireContext().showSnackbarShort(
                                            binding.root,
                                            "Max of 4 images cam be uploaded"
                                        )
                                    }
                                }

                                mediaRecycler.notifyDataSetChanged()
//                                update views
                                updateMediaIcons()
                            } else {
                                //picked single image
                                val imageUri = data.data
                                imageUri?.let {
                                    if (requireContext().checkTypeOfMedia((imageUri)) == "video") {
                                        wrongFileChosen("image")
                                    } else {
                                        if (mediaSize(imageUri) > MAX_IMAGE_SIZE
                                        ) {
                                            fileSizeToBig(IMAGE)
                                        } else {
                                            viewModel.addMedia(it)
                                            mediaRecycler.notifyDataSetChanged()
                                            if (viewModel.getNoOfMedia() > MAX_NUMBER_OF_IMAGES) imageMax =
                                                true
                                            imageChosen = true
                                            videoChosen = false
                                            updateMediaIcons()
                                        }
                                    }
                                }
                            }
                        } else requireContext().showSnackbarShort(
                            binding.root,
                            "Failed to get image"
                        )
                        updateDoneButton()
                    }

//                set up callback to get image from camera
                getImageFromCamera = registerForActivityResult(CropImageContract()) { result ->
                    if (result.isSuccessful) {
                        mediaRecycler.changeMediaType(IMAGE)
                        result.getUriFilePath(requireContext(), true)?.let {
                            viewModel.addMedia(convertFilePathToUri(it))

                            mediaRecycler.notifyDataSetChanged()
                            if (viewModel.getNoOfMedia() > MAX_NUMBER_OF_IMAGES) imageMax = true
                            imageChosen = true
                            videoChosen = false
                            updateMediaIcons()
                        }
                    } else {
                        // an error occurred
                        requireContext().showSnackbarShort(
                            binding.root,
                            "Failed to get Image."
                        )
                    }
                    updateDoneButton()
                }

//                set up callback to make video from camera
                getVideoFromCamera =
                    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            mediaRecycler.changeMediaType(VIDEO)
                            val videoUri = result.data?.data
                            videoUri?.let {
//                                check video size
                                if (mediaSize(it) > MAX_VIDEO_SIZE) {
                                    fileSizeToBig(VIDEO)
                                } else {
                                    viewModel.addMedia(videoUri)
                                    videoChosen = true
                                    imageChosen = false
                                    updateMediaIcons()
                                    //     launchVideoCompressionDialog()
                                    mediaRecycler.notifyDataSetChanged()
                                }
                            }
                        } else requireContext().showSnackbarShort(
                            binding.root,
                            "Failed to get video"
                        )
                        updateDoneButton()
                    }

//                set up callback to get video from gallery
                getVideoFromGallery =
                    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            mediaRecycler.changeMediaType(VIDEO)
                            val videoUri = result.data?.data
                            videoUri?.let {
//                                check type of file chosen
                                if (requireContext().checkTypeOfMedia(
                                        (it)
                                    ) == "image"
                                ) {
                                    wrongFileChosen("video")
                                }
//                                check file size
                                else {
                                    if (mediaSize(videoUri) > MAX_VIDEO_SIZE) {
                                        fileSizeToBig(VIDEO)
                                    } else {
                                        viewModel.addMedia(videoUri)
                                        videoChosen = true
                                        imageChosen = false

                                        mediaRecycler.notifyDataSetChanged()
                                        updateMediaIcons()
                                    }
                                }
                            }
                        } else requireContext().showSnackbarShort(
                            binding.root,
                            "Failed to get video"
                        )
                        updateDoneButton()
                    }

//                init media recycler
                mediaRecycler =
                    NewPostMediaAdapter(viewModel.mediaUri, IMAGE) { position, uri ->
                        viewModel.removeMedia(uri)
                        removeMedia()
                        updateMediaIcons()
                        updateDoneButton()
                    }

//                set up permissions contract
                requestMultiplePermissions = registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    permissions.entries.forEach {
                        if (!it.value) {
                            permissionDenied()
                            return@registerForActivityResult
                        }
                    }

//                    callback to intended action that warranted for permission to be granted
                    when (mediaAction) {
                        IMAGE_FROM_GALLERY -> imageFromGallery()
                        IMAGE_FROM_CAMERA -> snapImage()
                        VIDEO_FROM_GALLERY -> videoFromGallery()
                        else -> snapVideo()
                    }

                }
            }
        }
        activity?.lifecycle?.addObserver(activityObserver)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (shouldInterceptOnBackPressed) {
                        abortPostCreationDialog()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                        shouldInterceptOnBackPressed = true
                        isEnabled = true
                    }
                }
            })
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setUpMediaRecycler()
        updateMediaIcons()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setUpMediaRecycler() {
        binding.recyclerImage.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerImage.adapter = mediaRecycler
    }


    //    makes sure user cannot push empty post to database
    private fun updateDoneButton() {
//        check if text and media are both not empty
        if (binding.needInputLayout.editText?.text.toString()
                .isEmpty() && viewModel.getNoOfMedia() == 0
        ) binding.done.disable()
        else binding.done.enable()
    }

    private fun snapImage() {
        getImageFromCamera.launch(
            options {
                setGuidelines(CropImageView.Guidelines.ON)
                setCropShape(CropImageView.CropShape.RECTANGLE)
                setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                setImageSource(includeGallery = false, includeCamera = true)
            }
        )

    }

    //    informs user that file chosen is above size limit
    private fun fileSizeToBig(mediaType: String) {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            title = "Image size too large",
            message = when (mediaType) {
                VIDEO -> {
                    "Max video size allowed is 10 MB"
                }
                else -> "Max image size allowed is 2 MB"
            },
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    //    update test input view based on user input
    private fun watchTextLayout() {
        binding.needInputLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //do nothing
            }

            override fun afterTextChanged(editable: Editable?) {
                editable?.let {
//                    control hiding of the hint of the input layout
                    binding.needInputLayout.isHintEnabled = it.isEmpty()
                    if (it.isEmpty() && viewModel.getNoOfMedia() == 0) binding.done.disable()
                    else binding.done.enable()
                }

            }
        })
    }


    //    informs user that wrong file type chosen
    private fun wrongFileChosen(msg: String) {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            title = "Wrong file chosen",
            message = "Please choose $msg file",
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun snapVideo() {
        getVideoFromCamera.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 16000)
        })
    }

    //     informs user that permission ws not granted
    private fun permissionDenied() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = getString(R.string.permission_denied),
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    //    ask for required permissions
    private fun askPermissions() {
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }


    // this is done because seems there is a problem with the output uri of the crop library
    private fun convertFilePathToUri(path: String) = File(path).toUri()

    private fun chooseVideos() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            title = "Choose from",
            itemsId = R.array.picture_options,
            itemsAction = { position ->
                when (position) {
                    0 -> {
                        mediaAction = VIDEO_FROM_GALLERY
                        askPermissions()
                    }
                    1 -> {
                        mediaAction = VIDEO_FROM_CAMERA
                        askPermissions()
                    }
                }
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun chooseImages() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            title = "Choose from",
            itemsId = R.array.picture_options,
            itemsAction = { position ->
                when (position) {
                    0 -> {
                        mediaAction = IMAGE_FROM_GALLERY
                        askPermissions()
                    }
                    1 -> {
                        mediaAction = IMAGE_FROM_CAMERA
                        askPermissions()
                    }
                }
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun imageFromGallery() {
        getImageFromGallery.launch(Intent().apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            action = Intent.ACTION_GET_CONTENT
        })
    }

    private fun videoFromGallery() {
        getVideoFromGallery.launch(Intent().apply {
            type = "video/*"
            action = Intent.ACTION_GET_CONTENT
        })
    }

    override fun onStart() {
        clickListeners()
        handleObservers()
        showTags()
        watchTextLayout()
        super.onStart()
    }

    private fun handleObservers() {
//        observe result of new post request
        viewModel.newPostResult.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Event.Success -> {
//                    new post successfully created
                    dialog?.dismiss()
                    requireContext().hideKeyboard(binding.root)
//                        navigate to feed page
                    findNavController().popBackStack()
                }
                is Event.Failure -> {
//                    failed to create  new post
                    showPostFailureDialog()
                }
                is Event.InProgress -> {
//                    inform user of creation of new post
                    postingNewPost()
                }
            }
        })
    }

    //    informs user of failed attempt to create a new post
    private fun showPostFailureDialog() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            message = "We couldn't upload your post. Please try again",
            positiveButtonText = "Try again",
            positiveAction = {
                viewModel.createNewPost(binding.needInputLayout.editText?.text.toString())
            },
            negativeButtonText = "Cancel",
            negativeAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun postingNewPost() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = getString(R.string.create_new_post_msg)
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun clickListeners() {
        binding.apply {
            mediaControls.newPicture.setOnClickListener {
//                add images to post
                chooseImages()
            }
            mediaControls.newVideo.setOnClickListener {
//                add videos to post
                chooseVideos()
            }

            mediaControls.newTag.setOnClickListener {
//                add tags to post
                dialog?.dismiss()
                if (viewModel.tagsSize() != MAX_NUMBER_OF_TAGS) {
//                    number tags added not yet up to max allowed
                    dialogBuilder = requireContext().showDialog(
                        cancelable = true,
                        title = "Add tags",
                        multiChoiceItemsId = R.array.tags,
                        checkedItems = viewModel.generatePreviouslyAddedTags(),
                        positiveButtonText = "DONE",
                        positiveAction = {
                            // do nothing
                        },
                        multiChoiceItemsAction = { position, state ->
                            updateTags(position, state)
                        }
                    )
                    dialog = dialogBuilder?.create()
                    dialog?.show()
                } else {
//                    number of tags added already up to max
                    requireContext().showSnackbarShort(binding.root, "Max of 3 tags allowed.")
                }
            }
            cancel.setOnClickListener {
//                abort post creation
                abortPostCreationDialog()
            }
            done.setOnClickListener {
//                push new post to repository
                viewModel.createNewPost(binding.needInputLayout.editText?.text.toString().trim())
            }
            mediaControls.addComment.setOnClickListener {
//                update comment option and its view
                if (viewModel.hasComments) {
                    viewModel.setComment(false)
                    mediaControls.addComment.setImageResource(R.drawable.ic_outline_insert_comment_diasbled_24)
                    requireContext().showSnackbarShort(
                        binding.root,
                        "Comments on this Post is disabled"
                    )
                } else {
                    viewModel.setComment(true)
                    mediaControls.addComment.setImageResource(R.drawable.ic_baseline_insert_comment_24)
                    requireContext().showSnackbarShort(
                        binding.root,
                        "Comments on this Post is enabled"
                    )
                }
            }

            mediaControls.addLink.setOnClickListener {
//                gives user ability to direct add a link to their post
                dialog?.dismiss()
                dialogBuilder = requireContext().showDialog(
                    cancelable = true,
                    positiveButtonText = "Done",
                    negativeButtonText = "Cancel",
                    linkBinding = LinkLayoutBinding.inflate(LayoutInflater.from(requireContext())),
                    linkAction = { newLink ->
//                        ensure empty link is not added to the post
                        if (newLink != null) {
                            viewModel.addLink(newLink)
                            mediaControls.addLink.setImageResource(R.drawable.ic_baseline_insert_link_24)
                        } else {
                            viewModel.addLink(null)
                            mediaControls.addLink.setImageResource(R.drawable.ic_baseline_link_off_24)
                            requireContext().showSnackbarShort(binding.root, "Link was not added.")
                        }
                    },
                    oldLink = viewModel.link
                )
                dialog = dialogBuilder?.create()
                dialog?.show()
            }
        }
    }

    private fun abortPostCreationDialog() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            title = getString(R.string.discard_post),
            message = getString(R.string.discard_post_msg),
            positiveButtonText = "OK",
            positiveAction = {
                findNavController().popBackStack()
            },
            negativeButtonText = getString(R.string.cancel),
            negativeAction = {
                shouldInterceptOnBackPressed = false
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun updateTags(position: Int, state: Boolean) {
        if (state) {
            viewModel.addTag(position, state)
//                    check if max allowed number of tags have been reached
            if (viewModel.tagsSize() == MAX_NUMBER_OF_TAGS) {
                dialog?.dismiss()
                binding.mediaControls.newTag.setImageResource(R.drawable.ic_baseline_add_box_disabled_24)
                requireContext().showSnackbarShort(binding.root, "Max of 3 tags allowed")
            }
        } else {
            viewModel.removeTag(position)
        }
//        update the tag view
        showTags()
    }


    private fun removeMedia() {
        mediaRecycler.notifyDataSetChanged()
    }

    private fun showTags() {
        binding.chipGroup.removeAllViews()
        val chipsData = resources.getStringArray(R.array.tags)
        viewModel.tags.forEach { item ->
            if (item.value) {
                val chip = layoutInflater.inflate(
                    R.layout.chip,
                    binding.chipGroup,
                    false
                ) as Chip
                chip.text = chipsData[item.key]
                chip.isCloseIconVisible = true
                chip.setOnCloseIconClickListener {
                    viewModel.removeTag(item.key)
                    binding.chipGroup.removeView(chip)
                    binding.mediaControls.newTag.setImageResource(R.drawable.ic_baseline_add_box_24)
                }
                binding.chipGroup.addView(chip)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            this.putBoolean(VIDEO_CHOSEN, videoChosen)
            this.putBoolean(IMAGE_CHOSEN, imageChosen)
        }
        super.onSaveInstanceState(outState)
    }

    private fun updateMediaIcons() {
        if (viewModel.getNoOfMedia() > 0) {
            if (imageChosen && imageMax) {
                binding.mediaControls.newPicture.setImageResource(R.drawable.ic_baseline_add_a_photo_disabled_24)
                binding.mediaControls.newVideo.setImageResource(R.drawable.ic_baseline_video_call_disabled_24)
                binding.mediaControls.newVideo.disable()
                binding.mediaControls.newPicture.disable()
                return
            }
            if (imageChosen) {
                binding.mediaControls.newPicture.setImageResource(R.drawable.ic_baseline_add_a_photo_24)
                binding.mediaControls.newVideo.setImageResource(R.drawable.ic_baseline_video_call_disabled_24)
                binding.mediaControls.newVideo.disable()
                binding.mediaControls.newPicture.enable()
                return
            }
            if (videoChosen) {
                binding.mediaControls.newPicture.setImageResource(R.drawable.ic_baseline_add_a_photo_disabled_24)
                binding.mediaControls.newVideo.setImageResource(R.drawable.ic_baseline_video_call_disabled_24)
                binding.mediaControls.newVideo.disable()
                binding.mediaControls.newPicture.disable()
            }
        } else {
            imageChosen = false
            videoChosen = false
            binding.mediaControls.newPicture.setImageResource(R.drawable.ic_baseline_add_a_photo_24)
            binding.mediaControls.newVideo.setImageResource(R.drawable.ic_baseline_video_call_24)
            binding.mediaControls.newVideo.enable()
            binding.mediaControls.newPicture.enable()
        }
    }


    override fun onDestroy() {
        _binding = null
        activity?.lifecycle?.removeObserver(activityObserver)
        super.onDestroy()
    }

    companion object {
        const val IMAGE_FROM_GALLERY = "gallery_image"
        const val IMAGE_FROM_CAMERA = "camera_image"
        const val VIDEO_FROM_GALLERY = "gallery_video"
        const val VIDEO_FROM_CAMERA = "camera_video"
        const val IMAGE = "image"
        const val VIDEO = "video"
        const val VIDEO_CHOSEN = "video_chosen"
        const val IMAGE_CHOSEN = "image_chosen"
        const val MAX_IMAGE_SIZE = 2097152
        const val MAX_VIDEO_SIZE = 10485760
        const val MAX_NUMBER_OF_IMAGES = 3
        const val MAX_NUMBER_OF_TAGS = 3
        const val VIDEO_FILE_SIZE_IN_MB = 8
    }
}