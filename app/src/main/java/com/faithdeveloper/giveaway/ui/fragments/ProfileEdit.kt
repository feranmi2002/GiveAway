package com.faithdeveloper.giveaway.ui.fragments

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.Repository.Companion.PHONE_NUMBER_INDEX
import com.faithdeveloper.giveaway.data.Repository.Companion.USERNAME_INDEX
import com.faithdeveloper.giveaway.databinding.LayoutProfileEditBinding
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.viewmodels.ProfileEditVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class ProfileEdit : Fragment() {
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var dialog: AlertDialog? = null
    private var _binding: LayoutProfileEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileEditVM
    private lateinit var activityObserver: ActivityObserver
    private lateinit var userDetails: Array<String?>
    private lateinit var cropImage: ActivityResultLauncher<CropImageContractOptions>
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>
    private var shouldInterceptOnBackPressed = true

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
                    this@ProfileEdit,
                    VMFactory(
                        (requireActivity() as MainActivity).getRepository()
                    )
                ).get(ProfileEditVM::class.java)
            }
        }
        activity?.lifecycle?.addObserver(activityObserver)
        cropImage = registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                // use the returned uri
                result.getUriFilePath(requireContext(), true)?.let {
                    viewModel.newPicture(
                        convertFilePathToUri(it)
                    )
                    loadProfilePicture()
                }
            } else {
                // an error occurred
                requireContext().showSnackbarShort(
                    binding.root,
                    "Profile picture upload failed. Try again."
                )
            }
        }
        requestMultiplePermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                if (!it.value) {
                    permissionDenied()
                    return@registerForActivityResult
                }
            }
            choosePicture()
        }
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
        _binding = LayoutProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        watchNameBox()
        watchPhoneBox()
        watchBioBox()
        watchOccupationBox()
        fillUserDetails()
        updateProfile()
        chooseNewPicture()
        navigateBack()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        handleObservers()
        super.onStart()
    }

    // this is done because seems there is a problem with the output uri of the crop library
    private fun convertFilePathToUri(path: String) = File(path).toUri()

    private fun choosePicture() {
        dialog?.dismiss()
        cropImage.launch(
            options {
                setGuidelines(CropImageView.Guidelines.ON)
                setCropShape(CropImageView.CropShape.OVAL)
                setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                setImageSource(includeGallery = true, includeCamera = true)
            }
        )
    }

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


    private fun chooseNewPicture() {
        binding.setPicture.setOnClickListener {
            askPermissions()
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

    private fun fillUserDetails() {
        userDetails = viewModel.getUserDetails()
        binding.nameLayout.editText?.setText(userDetails[USERNAME_INDEX])
        binding.phoneLayout.editText?.setText(userDetails[PHONE_NUMBER_INDEX])
        loadProfilePicture()
    }

    private fun loadProfilePicture() {
        Glide.with(this)
            .load(viewModel.newPicture)
            .placeholder(R.drawable.ic_baseline_account_circle_grey_24)
            .error(viewModel.getUserProfilePic())
            .into(binding.profiePic)
    }

    private fun watchNameBox() {
        binding.nameLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun afterTextChanged(p0: Editable?) {
                p0?.let {
                    if (it.isNotEmpty()) {
                        binding.nameLayout.error = null
                    } else {
                        binding.nameLayout.error = getString(R.string.correct_name)
                    }
                    userDetails[USERNAME_INDEX] = it.toString()
                }
            }
        })
    }

    private fun watchPhoneBox() {
        binding.phoneLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(phoneText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun afterTextChanged(phoneText: Editable?) {
                phoneText?.let {

                    if (it.isNotEmpty() && android.util.Patterns.PHONE.matcher(it).matches()) {
                        binding.phoneLayout.error = null
                    } else {
                        binding.phoneLayout.error = getString(R.string.phone_num_error)
                    }
                    userDetails[PHONE_NUMBER_INDEX] = it.toString()
                }
            }
        })
    }

    private fun watchOccupationBox() {
        binding.jobLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(phoneText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun afterTextChanged(phoneText: Editable?) {
                phoneText?.let {

                    if (it.isNotEmpty() && android.util.Patterns.PHONE.matcher(it).matches()) {
                        userDetails[PHONE_NUMBER_INDEX] = it.toString()
                    } else {
                    }
                }
            }
        })
    }

    private fun watchBioBox() {
        binding.bioLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(phoneText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun afterTextChanged(phoneText: Editable?) {
                phoneText?.let {
                    if (it.isNotEmpty() && android.util.Patterns.PHONE.matcher(it).matches()) {
                    } else {
                    }
                    userDetails[PHONE_NUMBER_INDEX] = it.toString()
                }
            }
        })
    }

    override fun onDestroyView() {
        _binding = null
        activity?.lifecycle?.removeObserver(activityObserver)
        super.onDestroyView()
    }

    private fun handleObservers() {
        viewModel.result.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Event.Success -> {
                    dialog?.dismiss()
                    requireContext().showSnackbarShort(requireView(), "Update Successful")
                    findNavController().popBackStack()
                }
                is Event.Failure -> {
                    incompleteUpdate()
                }
                else -> {
                    // do nothing
                }
            }
        })
    }

    private fun incompleteUpdate() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "We couldn't complete profile upload. Try again",
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun updateProfile() {
        binding.save.setOnClickListener {
            if (userDetails[USERNAME_INDEX]?.isNotBlank() == true && userDetails[PHONE_NUMBER_INDEX]?.isNotBlank() == true) {
                dialog?.dismiss()
                dialogBuilder = requireContext().showDialog(
                    cancelable = false,
                    message = "Updating your profile"
                )
                dialog = dialogBuilder?.create()
                dialog?.show()
                viewModel.updateProfile(
                    binding.nameLayout.editText!!.text.toString().trim(),
                    binding.phoneLayout.editText!!.text.toString().trim()
                )
            } else {
                requireContext().showSnackbarShort(binding.root, "Fill your details")
            }
        }
    }

    fun navigateBack() {
        binding.back.setOnClickListener {
            abortPostCreationDialog()
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
                shouldInterceptOnBackPressed = true
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

}