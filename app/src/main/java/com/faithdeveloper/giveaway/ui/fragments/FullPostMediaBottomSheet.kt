package com.faithdeveloper.giveaway.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.faithdeveloper.giveaway.databinding.FullPostMediaLayoutBinding
import com.faithdeveloper.giveaway.ui.adapters.FullPostImagesAdapter
import com.faithdeveloper.giveaway.utils.Extensions.disable
import com.faithdeveloper.giveaway.utils.Extensions.enable
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.utils.interfaces.FullImageAdapterInterface
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.Util
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

class FullPostMediaBottomSheet : BottomSheetDialogFragment(), FullImageAdapterInterface {

    //    init properties
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var mDialog: AlertDialog? = null
    private lateinit var mediaType: String
    private lateinit var media: Array<String>
    private lateinit var adapter: FullPostImagesAdapter
    private var position by Delegates.notNull<Int>()
    private var FRAGMENT_JUST_CREATED by Delegates.notNull<Boolean>()
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>

    private var _binding: FullPostMediaLayoutBinding? = null
    private val binding get() = _binding!!
    private var currentMediaUrl: String? = null
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:mm:ss z",
        Locale.getDefault()
    )


    override fun onCreate(savedInstanceState: Bundle?) {
//        get all arguments
        media = requireArguments().getStringArray(MEDIA)!!
        mediaType = requireArguments().getString(TYPE)!!
        position = requireArguments().getInt(POSITION)
        FRAGMENT_JUST_CREATED = true
        if (mediaType == IMAGES) {
            adapter = FullPostImagesAdapter(media, this)
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
//            permission given by user
            downloadMedia()
        }

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FullPostMediaLayoutBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
       (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        binding.back.setOnClickListener {
            dismiss()
        }

        if (mediaType == IMAGES) {
            with(binding) {
                recycler.makeVisible()
                recycler.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                recycler.adapter = adapter
                if (FRAGMENT_JUST_CREATED) recycler.scrollToPosition(position)
            }
        } else {
            binding.videoView.makeVisible()
        }
        downloadClick()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        if (mediaType == VIDEO) {
            hideSystemUi()
            if (Util.SDK_INT > 23) {
                initializePlayer()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    override fun onDestroyView() {
        binding.recycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun askPermissions() {
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }

    //     informs user that permission was not granted
    private fun permissionDenied() {
        mDialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "Failed to save image due to lack of permission",
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        mDialog = dialogBuilder?.create()
        dialog?.show()
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowInsetsControllerCompat(
            requireActivity().window,
            binding.videoView
        ).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }

    private fun downloadMedia() {
        Glide.with(requireContext())
            .load(currentMediaUrl)
            .onlyRetrieveFromCache(true)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, p1: Transition<in Drawable>?) {
                    saveImageToStorage((resource as BitmapDrawable).bitmap)
                }

                override fun onLoadCleared(p0: Drawable?) {
//                            do nothing
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    requireContext().showSnackbarShort(binding.root, "Failed to save image")
                    super.onLoadFailed(errorDrawable)
                }
            })

    }

    fun saveImageToStorage(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            val resolver = requireContext().applicationContext.contentResolver
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Find all image files on the primary external storage device.
                    val imageCollection =
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                    val newImageDetails = ContentValues().apply {
                        put(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            "IMG-${System.currentTimeMillis()}.jpg"
                        )
                        put(MediaStore.MediaColumns.DATE_ADDED, dateFormatter.format(Date()))
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/Giveaway"
                        )
                    }
                    with(resolver) {
                        val imageUri = insert(imageCollection, newImageDetails)
                        resolver.openOutputStream(imageUri!!)?.use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                            stream.close()
                        }
                    }
                } else {
                    val directory =
                        requireContext().getExternalFilesDir("${Environment.DIRECTORY_PICTURES}/Giveaway")
                    if (!directory?.exists()!!) directory.mkdir()
                    val newImageDetails = ContentValues().apply {
                        put(
                            MediaStore.Images.Media.DISPLAY_NAME,
                            "IMG-${System.currentTimeMillis()}.jpg"
                        )
                        put(MediaStore.Images.Media.DATE_ADDED, dateFormatter.format(Date()))
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                    }
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newImageDetails)
                    val file = File(directory, "IMG-${System.currentTimeMillis()}.jpg")
                    val fileOutputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                    fileOutputStream.close()
                }
                requireContext().showSnackbarShort(binding.root, "Image saved")
            } catch (e: Exception) {
                requireContext().showSnackbarShort(binding.root, "Failed to save image")
            }
        }

    }

    private fun downloadClick() {
        if (mediaType == IMAGES) {
            binding.download.setOnClickListener {
                currentMediaUrl = media[adapter.getViewHolder()!!.absoluteAdapterPosition]
                askPermissions()
            }
        }
    }

    override fun imageIsReady(imageReady: Boolean) {
        if (imageReady) binding.download.enable()
        else binding.download.disable()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(requireContext())
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(media[0])
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentItem, playbackPosition)
                exoPlayer.prepare()
            }
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onIsLoadingChanged(isLoading: Boolean) {
            super.onIsLoadingChanged(isLoading)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
        }
    }

    companion object {
        const val TAG = "MediaBottomSheet"
        private const val MEDIA = "media"
        private const val TYPE = "type"
        const val IMAGES = "images"
        private const val VIDEO = "video"
        private const val POSITION = "position"
        fun instance(media: Array<String>, type: String, position: Int): FullPostMediaBottomSheet {
            return FullPostMediaBottomSheet().apply {
                arguments = Bundle().apply {
                    putStringArray(MEDIA, media)
                    putString(TYPE, type)
                    putInt(POSITION, position)
                }
            }
        }
    }


}