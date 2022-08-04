package com.faithdeveloper.giveaway.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.faithdeveloper.giveaway.ui.adapters.FullPostImagesAdapter
import com.faithdeveloper.giveaway.databinding.FullPostMediaLayoutBinding
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FullPostMediaBottomSheet : BottomSheetDialogFragment() {
    private var _binding: FullPostMediaLayoutBinding? = null
    private val binding get() = _binding!!
    private lateinit var mediaType: String
    private lateinit var media: Array<String>
    override fun onCreate(savedInstanceState: Bundle?) {
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

    override fun onStart() {
//        (dialog as? BottomSheetDialog)?.behavior?.isFitToContents = false
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        mediaType = arguments?.getString(TYPE)!!
        media = arguments?.getStringArray(MEDIA)!!
        handleMediaType()
        handleClose()
        super.onStart()
    }

    private fun handleMediaType() {
        if (mediaType == IMAGES) {
            showImages()
            return
        }

        if (mediaType == VIDEO) {
            showVideo()
            return
        }


    }

    private fun showVideo() {
        // do nothing
    }

    private fun showImages() {
        binding.apply {
            recycler.makeVisible()
            recycler.adapter = FullPostImagesAdapter(media)
            recycler.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
        }
        super.onSaveInstanceState(outState)
    }

    private fun handleClose() {
        binding.dismiss.setOnClickListener {
            dialog?.dismiss()
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