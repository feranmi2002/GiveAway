package com.faithdeveloper.giveaway.fragments

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.faithdeveloper.giveaway.*
import com.faithdeveloper.giveaway.Extensions.getSignInStatus
import com.faithdeveloper.giveaway.Extensions.setSignInStatus
import com.faithdeveloper.giveaway.Extensions.showDialog
import com.faithdeveloper.giveaway.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.databinding.LayoutFeedBinding
import com.faithdeveloper.giveaway.viewmodels.FeedVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import permissions.dispatcher.NeedsPermission
import kotlin.properties.Delegates

class Home : Fragment() {
    private var _binding: LayoutFeedBinding? = null
    private lateinit var viewModel: FeedVM
    private val binding get() = _binding!!
    private var newSignIn by Delegates.notNull<Boolean>()
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var dialog: AlertDialog? = null

    //    private lateinit var tabSetup: FeedTabsSetup
//    private lateinit var viewPager:ViewPager2
//    private lateinit var tabLayout: TabLayout
    private lateinit var activityObserver: ActivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        activityObserver = object : ActivityObserver() {
            override fun onCreateAction() {
                newSignIn = requireContext().getSignInStatus()
                viewModel = ViewModelProvider(
                    this@Home,
                    VMFactory((activity as MainActivity).getRepository())
                ).get(FeedVM::class.java)
            }
        }
        activity?.lifecycle?.addObserver(activityObserver)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        _binding = LayoutFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarConfig = AppBarConfiguration(findNavController().graph)
        binding.toolbar.setupWithNavController(findNavController(), appBarConfig)


//        tabSetup = FeedTabsSetup(this)
//        viewPager = binding.viewPager
//        viewPager.adapter = tabSetup
//        tabLayout = binding.tabLayout
//        TabLayoutMediator(tabLayout, viewPager){tab, position ->
//            tab.text = tabNames[position]
//            tab.setIcon(tabIcons[position])
//        }.attach()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setUpToolbar() {
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.giveaway -> handleNewPost()
            }
            true
        }

    }

    private fun handleNewPost() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            title = "New Post",
            cancelable = true,
            itemsId = R.array.new_post_options,
            itemsAction = {
                if (it == 0) {
                    findNavController().navigate(HomeDirections.actionHomeToNeed2())
                } else findNavController().navigate(HomeDirections.actionHomeToExchange())
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun handleObservers() {
        viewModel.homeResult.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Event.Success -> {
                    // do nothing
                }
                is Event.Failure -> {
                    if (it.msg == "Profile pic update failed") {
                        requireContext().showSnackbarShort(
                            binding.root,
                            "Failed to upload profile picture."
                        )
                    }
                }
                is Event.InProgress -> {
                    // do nothing}
                }
            }
        })
    }

    private fun handleCreateProfilePicDialog() {
        if (newSignIn) {
            dialog?.dismiss()
            dialogBuilder = requireContext().showDialog(
                cancelable = true,
                title = "Add a profile picture",
                message = "Adding a profile picture helps people recognize you easily. Would you like to? ",
                positiveButtonText = "YES",
                positiveAction = {
                    choosePicture()
                },
                negativeButtonText = "LATER",
                negativeAction = {
                    //do nothing
                }
            )
            dialog = dialogBuilder?.create()
            dialog?.show()
            requireContext().setSignInStatus(false)
        }
    }

    @NeedsPermission(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private fun choosePicture() {
        val cropImage = registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                // use the returned uri
                requireContext().showSnackbarShort(binding.root, "Uploading profile picture")
                viewModel.uploadProfilePicture(result.uriContent)
            } else {
                // an error occurred
                requireContext().showSnackbarShort(
                    binding.root,
                    "Profile picture upload failed. Try again."
                )
            }
        }
        cropImage.launch(
            options {
                setGuidelines(CropImageView.Guidelines.ON)
                setCropShape(CropImageView.CropShape.OVAL)
                setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                setImageSource(includeGallery = true, includeCamera = true)

            }
        )
    }

    private fun checkIfUserDetailsIsStored() = viewModel.checkIfUserDetailIsStored()

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        const val NEW_POST = 0
//        val tabNames = arrayOf("Need", "Gift", "Exchange")
//        val tabIcons = arrayOf(
//            R.drawable.ic_baseline_need_24,
//            R.drawable.ic_baseline_shopping_bag_24,
//            R.drawable.ic_baseline_icon_swap_horiz_24
//        )
    }

}