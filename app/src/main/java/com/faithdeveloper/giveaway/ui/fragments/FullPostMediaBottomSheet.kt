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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.faithdeveloper.giveaway.databinding.FullPostMediaLayoutBinding
import com.faithdeveloper.giveaway.ui.adapters.FullPostImagesAdapter
import com.faithdeveloper.giveaway.utils.Extensions.disable
import com.faithdeveloper.giveaway.utils.Extensions.enable
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.utils.interfaces.FullImageAdapterInterface
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FullPostMediaBottomSheet : BottomSheetDialogFragment(), FullImageAdapterInterface {

    //    init properties
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var mDialog: AlertDialog? = null
    private lateinit var mediaType: String
    private lateinit var media: Array<String>
    private lateinit var adapter: FullPostImagesAdapter
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>

    private var _binding: FullPostMediaLayoutBinding? = null
    private val binding get() = _binding!!
    private var currentMediaUrl: String? = null

    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:mm:ss z",
        Locale.getDefault()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
//        get all arguments

        media = requireArguments().getStringArray(MEDIA)!!
        mediaType = requireArguments().getString(TYPE)!!
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
        binding.dismiss.setOnClickListener {
            dismiss()
        }

//        handle appropriate view
        if (mediaType == IMAGES) {
            with(binding) {
                recycler.makeVisible()
                recycler.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                recycler.adapter = adapter
            }
        }

        if (currentMediaUrl == null) binding.download.disable()
        else binding.download.enable()

        saveMedia()
        super.onViewCreated(view, savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
//    updates the count
    override fun updateCount(position: Int, size: Int) {
        if (size > 1) {
            binding.count.makeVisible()
            binding.count.text = "$position / $size"
        }
    }

    //    this flag checks if media has been loaded by glide and sets the download icon accordingly
    override fun mediaAvailabilityState(state: Boolean, mediaUrl: String?) {
        currentMediaUrl = mediaUrl
        if (state) binding.download.enable()
        else binding.download.disable()
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

    private fun saveMedia() {
        binding.download.setOnClickListener {
            askPermissions()
        }
    }

    private fun downloadMedia() {
        Glide.with(requireContext())
            .load(currentMediaUrl)
            .onlyRetrieveFromCache(true)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, p1: Transition<in Drawable>?) {
                   saveMediaToStorage ((resource as BitmapDrawable).bitmap)
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

    fun saveMediaToStorage(bitmap:Bitmap) {
        val resolver = requireContext().applicationContext.contentResolver
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                // Find all image files on the primary external storage device.
                val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val newImageDetails = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG-${System.currentTimeMillis()}.jpg")
                    put(MediaStore.MediaColumns.DATE_ADDED, dateFormatter.format(Date()))
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Giveaway")
                }
                with(resolver) {
                    val imageUri = insert(imageCollection, newImageDetails)
                    resolver.openOutputStream(imageUri!!)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        stream.close()
                    }
                }
            }else{
                val directory = requireContext().getExternalFilesDir("${Environment.DIRECTORY_PICTURES}/Giveaway")
                if (!directory?.exists()!!) directory.mkdir()
                val newImageDetails = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "IMG-${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.DATE_ADDED, dateFormatter.format(Date()))
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                }
                 resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newImageDetails)
                val file = File(directory, "IMG-${System.currentTimeMillis()}.jpg" )
                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                fileOutputStream.close()
                }
            requireContext().showSnackbarShort(binding.dismiss, "Image saved")
        }catch (e:Exception){
            requireContext().showSnackbarShort(binding.dismiss, "Failed to save image")
        }
    }

    companion object {
        const val TAG = "PictureDialog"
        private const val MEDIA = "media"
        private const val TYPE = "type"
        const val IMAGES = "images"
        private const val VIDEO = "video"
        fun instance(media: Array<String>, type: String): FullPostMediaBottomSheet {
            return FullPostMediaBottomSheet().apply {
                arguments = Bundle().apply {
                    putStringArray(MEDIA, media)
                    putString(TYPE, type)
                }
            }
        }
    }
}