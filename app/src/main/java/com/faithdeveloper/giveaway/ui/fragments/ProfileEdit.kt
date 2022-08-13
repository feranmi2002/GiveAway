package com.faithdeveloper.giveaway.ui.fragments

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.faithdeveloper.giveaway.*
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.Repository.Companion.PHONE_NUMBER_INDEX
import com.faithdeveloper.giveaway.data.Repository.Companion.USERNAME_INDEX
import com.faithdeveloper.giveaway.databinding.LayoutProfileEditBinding
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.disable
import com.faithdeveloper.giveaway.utils.Extensions.enable
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.viewmodels.ProfileEditVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import kotlin.properties.Delegates

class ProfileEdit : Fragment() {
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var dialog: AlertDialog? = null
    private var _binding: LayoutProfileEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileEditVM
    private lateinit var activityObserver: ActivityObserver
    private var nameSame by Delegates.notNull<Boolean>()
    private var phoneSame by Delegates.notNull<Boolean>()
    private var pictureSame by Delegates.notNull<Boolean>()
    private lateinit var userDetails: Array<String?>
    private lateinit var profileEditType: String
    private lateinit var cropImage: ActivityResultLauncher<CropImageContractOptions>
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>

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
        if (savedInstanceState == null) {
            nameSame = true
            phoneSame = true
            pictureSame = true
            profileEditType = "none"
        } else {
            nameSame = savedInstanceState.getBoolean(NAME)
            phoneSame = savedInstanceState.getBoolean(PHONE)
            pictureSame = savedInstanceState.getBoolean(PICTURE)
            profileEditType = savedInstanceState.getString(PROFILE_EDIT_TYPE)!!

        }

        cropImage = registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                // use the returned uri
                result.getUriFilePath(requireContext(), true)?.let {
                    viewModel.newPicture(
                        convertFilePathToUri(it)
                    )
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
        super.onCreate(savedInstanceState)
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

    override fun onStart() {
        userDetails = viewModel.getUserDetails()
        fillUserDetails()
        watchNameBox()
        watchPhoneBox()
        updateProfile()
        handleObservers()
        chooseNewPicture()
        super.onStart()
    }

    private fun fillUserDetails() {
        binding.nameLayout.editText?.setText(userDetails[USERNAME_INDEX])
        binding.phoneLayout.editText?.setText(userDetails[PHONE_NUMBER_INDEX])
        if (viewModel.newPicture == null) {
            Glide.with(this)
                .load(viewModel.getUserProfilePic())
                .into(binding.profiePic)
            return
        }
        Glide.with(this)
            .load(viewModel.newPicture)
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
                        nameSame = it.toString() == userDetails[USERNAME_INDEX]
                        handleContinueBox()
                    } else {
                        binding.nameLayout.error = getString(R.string.correct_name)
                        nameSame = true
                        binding.continueBtn.disable()
                    }


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
                        phoneSame = it.toString() == userDetails[PHONE_NUMBER_INDEX]
                        handleContinueBox()
                    } else {
                        binding.phoneLayout.error = getString(R.string.phone_num_error)
                        phoneSame = true
                        binding.continueBtn.disable()
                    }
                }
            }
        })
    }

    private fun handleContinueBox() {
        if (nameSame && phoneSame && pictureSame) {
            binding.continueBtn.disable()
        } else if (nameSame && !phoneSame && pictureSame) {
            profileEditType = PHONE_TYPE
            binding.continueBtn.enable()
        } else if (!nameSame && phoneSame && pictureSame) {
            profileEditType = NAME_TYPE
            binding.continueBtn.enable()
        } else if (nameSame && phoneSame && !pictureSame) {
            profileEditType = PICTURE_TYPE
            binding.continueBtn.enable()
        } else if (nameSame && !phoneSame && !pictureSame) {
            profileEditType = PHONE_PICTURE_TYPE
            binding.continueBtn.enable()
        } else if (!nameSame && phoneSame && !pictureSame) {
            profileEditType = NAME_PICTURE_TYPE
            binding.continueBtn.enable()
        } else if (!nameSame && !phoneSame && pictureSame) {
            profileEditType = NAME_PHONE_TYPE
            binding.continueBtn.enable()
        } else {
            profileEditType = ALL
            binding.continueBtn.enable()
        }
    }

    override fun onDestroyView() {
        _binding = null
        activity?.lifecycle?.removeObserver(activityObserver)
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun handleObservers() {
        viewModel.result.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Event.Success -> {
                    if (it.data == "New picture") {
                        loadNewPicture()
                    } else {
                        dialog?.dismiss()
                        findNavController().popBackStack()
                    }
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
            cancelable = true,
            message = "We couldn't complete profile upload. Try again",
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun loadNewPicture() {
        pictureSame = false
        Glide.with(this)
            .load(viewModel.newPicture)
            .into(binding.profiePic)
    }

    private fun updateProfile() {
        binding.continueBtn.setOnClickListener {
            dialog?.dismiss()
            dialogBuilder = requireContext().showDialog(
                cancelable = false,
                message = "Updating your profile"
            )
            dialog = dialogBuilder?.create()
            dialog?.show()

            when (profileEditType) {
                PHONE_TYPE -> {
                    viewModel.updateProfile(
                        profileEditType,
                        binding.phoneLayout.editText?.text.toString(),
                        null
                    )
                }
                NAME_TYPE -> {
                    viewModel.updateProfile(
                        profileEditType,
                        null,
                        binding.nameLayout.editText?.text.toString(),
                    )
                }
                PICTURE_TYPE -> {
                    viewModel.updateProfile(profileEditType, null, null)
                }
                NAME_PICTURE_TYPE -> {
                    viewModel.updateProfile(
                        profileEditType,
                        null,
                        binding.nameLayout.editText?.text.toString()
                    )
                }
                NAME_PHONE_TYPE -> {
                    viewModel.updateProfile(
                        profileEditType,
                        binding.phoneLayout.editText?.text.toString(),
                        binding.nameLayout.editText?.text.toString()
                    )
                }
                PHONE_PICTURE_TYPE -> {
                    viewModel.updateProfile(
                        profileEditType,
                        binding.phoneLayout.editText?.text.toString(),
                        null
                    )

                }
                else -> {
                    viewModel.updateProfile(
                        profileEditType,
                        binding.phoneLayout.editText?.text.toString(),
                        binding.nameLayout.editText?.text.toString()
                    )
                }
            }
        }
    }

    companion object {
        const val NAME = "name"
        const val PHONE = "phone"
        const val PROFILE_EDIT_TYPE = "profileEditType"
        const val PHONE_TYPE = "phoneType"
        const val NAME_TYPE = "nameType"
        const val ALL = "all"
        const val PICTURE = "picture"
        const val PICTURE_TYPE = "pictureType"
        const val NAME_PHONE_TYPE = "$NAME_TYPE$PHONE_TYPE"
        const val NAME_PICTURE_TYPE = "$NAME_TYPE$PICTURE_TYPE"
        const val PHONE_PICTURE_TYPE = "$PHONE_TYPE$PICTURE_TYPE"
    }
}