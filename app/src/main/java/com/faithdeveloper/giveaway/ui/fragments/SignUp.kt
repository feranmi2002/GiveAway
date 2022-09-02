package com.faithdeveloper.giveaway.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.databinding.LayoutAccountCreationBinding
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.disable
import com.faithdeveloper.giveaway.utils.Extensions.enable
import com.faithdeveloper.giveaway.utils.Extensions.hideKeyboard
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.viewmodels.SignUpVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class SignUp : Fragment() {
    // init properties and binding
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var dialog: AlertDialog? = null
    private lateinit var viewModel: SignUpVM
    private var _binding: LayoutAccountCreationBinding? = null
    private val binding get() = _binding!!
    private var phoneEmpty = true
    private var nameEmpty = true
    private var emailEmpty = true
    private var passwordEmpty = true
    private lateinit var activityObserver: ActivityObserver


    override fun onCreate(savedInstanceState: Bundle?) {
        // all these are supposed to be used for retention of editTexts values
        savedInstanceState.apply {
            this?.let {
                passwordEmpty = it.getBoolean("passwordEmpty", true)
                emailEmpty = it.getBoolean("emailEmpty", true)
                nameEmpty = it.getBoolean("nameEmpty", true)
                phoneEmpty = it.getBoolean("phoneEmpty", true)
            }
        }

        /*
        * activityObserver observes the lifecycle of the activity and performs corresponding actions
        * */
        activityObserver = object : ActivityObserver() {
            override fun onResumeAction() {
                // inform repository that app is in a started state
                (activity as MainActivity).getRepository().setAppState(Repository.APP_STARTED)
            }

            override fun onPauseAction() {
                // inform repository that app is a paused state
                (activity as MainActivity).getRepository().setAppState(Repository.APP_PAUSED)
            }

            override fun onCreateAction() {
                // initialize view model
                viewModel = ViewModelProvider(
                    this@SignUp,
                    VMFactory((activity as MainActivity).getRepository())
                ).get(SignUpVM::class.java)
            }
        }
        activity?.lifecycle?.addObserver(activityObserver)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutAccountCreationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        handleObserver()
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // watch all input views and update them based on user input
        watchEmailBox()
        watchNameBox()
        watchPhoneBox()
        watchPasswordBox()

        // opens terms and condition of app
        handleTerms()

        /*These function updates the continue_button view
        * based on the user inputs.*/
        handleContinueButton()

        // Navigates to the sign in page.
        handleSignIn()

        super.onViewCreated(view, savedInstanceState)
    }

    private fun watchNameBox() {

        binding.phoneLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(nameText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun afterTextChanged(nameText: Editable?) {
                // update view
                nameText?.let {
                    if (!it.isNullOrEmpty() && it.isNotBlank()) {
                        nameEmpty = false
                        binding.nameLayout.error = null
                    } else {
                        nameEmpty = true
                        binding.nameLayout.error = getString(R.string.correct_name)
                    }
                    handleContinueBtnEnablement()
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
                // update view
                phoneText?.let {
                    if (it.isNotEmpty() && android.util.Patterns.PHONE.matcher(it).matches()) {
                        phoneEmpty = false
                        binding.phoneLayout.error = null
                    } else {
                        phoneEmpty = true
                        binding.phoneLayout.error = getString(R.string.phone_num_error)
                    }
                    handleContinueBtnEnablement()
                }
            }
        })
    }


    private fun watchEmailBox() {
        binding.emailLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(emailText: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun afterTextChanged(editable: Editable?) {
                // update view
                editable?.let {
                    if (it.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(editable)
                            .matches()
                    ) {
                        emailEmpty = false
                        binding.emailLayout.error = null
                    } else {
                        emailEmpty = true
                        binding.emailLayout.error = getString(R.string.enter_correct_email)
                    }
                    handleContinueBtnEnablement()
                }
            }

        })
    }

    private fun handleContinueBtnEnablement() {
        // update view
        if (!nameEmpty && !phoneEmpty && !emailEmpty && !passwordEmpty) {
            binding.continueBtn.enable()
        } else binding.continueBtn.disable()
    }

    private fun watchPasswordBox() {
        binding.passwordLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                do nothing
            }

            override fun afterTextChanged(text: Editable?) {
                // update view
                text?.let {
                    if (it.isNotEmpty() && it.length > 5) {
                        passwordEmpty = false
                        binding.passwordLayout.error = null
                    } else {
                        passwordEmpty = true
                        binding.passwordLayout.error = getString(R.string.password_characters)
                    }
                }
                handleContinueBtnEnablement()
            }
        })
    }

    private fun handleUnverifiedEmail() {
        /*user has signed up and has to verify email. So navigate to
        *UserUnverified fragment so user can request
        * for another email verification link*/
        findNavController().navigate(SignUpDirections.actionSignUpToUserUnverified(binding.emailLayout.editText?.text.toString()))
    }

    private fun handleTerms() {
        // open up terms and conditions
        binding.termsCondition.setOnClickListener {
            requireContext().hideKeyboard(binding.root)
            dialog?.dismiss()
            dialogBuilder = requireContext().showDialog(
                cancelable = true,
                title = "Terms and Privacy Policy",
                message = "Make sure you adhere to these terms.",
                positiveButtonText = "OK",
                positiveAction = {
                    // do nothing
                }
            )
            dialog = dialogBuilder?.create()
            dialog?.show()
        }
    }

    private fun handleSignIn() {
        // users wants to sign in. Navigate to sign in page
        binding.signIn.setOnClickListener {
            requireContext().hideKeyboard(binding.root)
            findNavController().navigate(SignUpDirections.actionSignUpToSignIn())
        }

    }

    private fun handleObserver() {
        viewModel.result.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                // successful requests
                is Event.Success -> {
                    // user successfully signed up
                    if (event.msg.contains("Account creation successful")) showVerificationInProgressDialog()
                    // verification link has been sent to user email
                    else if (event.msg.contains("Email verified")) handleEmailVerificationSuccess()
                }
                // failed requests
                is Event.Failure -> {
                    // failed to send verification link to user email
                    if (event.msg.contains("Failed to verify email")) showVerificationFailureDialog()
                    else {
                        when (event.data) {
                            // failed to sign user up
                            is FirebaseAuthUserCollisionException -> {
                                "This email is already in  use by another user. Enter another email".showAccountCreationFailureDialog()
                            }
                            is FirebaseNetworkException -> {
                                "Couldn't create account. Check your internet connection and try again".showAccountCreationFailureDialog()
                            }
                        }
                    }
                }
                is Event.InProgress -> {
                }
            }
        })
    }

    private fun handleEmailVerificationSuccess() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "Link to verify your email has been sent to ${
                binding.emailLayout.editText?.text.toString().trim()
            }. Go to your inbox to complete your registration.",
            positiveButtonText = "OK",
            positiveAction = {
                findNavController().navigate(
                    SignUpDirections.actionSignUpToUserUnverified(
                        binding.emailLayout.editText?.text.toString(),
                        true
                    )
                )
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun showVerificationFailureDialog() {
        dialog?.dismiss()

        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "We couldn't verify your account. Please try again ",
            positiveButtonText = "TRY AGAIN",
            negativeButtonText = "CANCEL",
            positiveAction = {
                viewModel.verifyEmailAddress()
            },
            negativeAction = {
                handleUnverifiedEmail()
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun String.showAccountCreationFailureDialog() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            message = this,
            negativeButtonText = "OK",
            negativeAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun showVerificationInProgressDialog() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "Verifying your email..."
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
        viewModel.verifyEmailAddress()
    }

    private fun showAccountCreationDialog() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "Creating your account..."
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun handleContinueButton() {
        // user has filled in all necessary inputs
        binding.continueBtn.setOnClickListener {
            requireContext().hideKeyboard(binding.root)
            signUp()
        }
    }

    private fun signUp() {
        // show progress
        showAccountCreationDialog()
        viewModel.signUp(
            binding.phoneLayout.editText?.text.toString().trim(),
            binding.nameLayout.editText?.text.toString().trim(),
            binding.emailLayout.editText?.text.toString().trim(),
            binding.passwordLayout.editText?.text.toString()
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putBoolean("passwordEmpty", passwordEmpty)
            putBoolean("emailEmpty", emailEmpty)
            putBoolean("phoneEmpty", phoneEmpty)
            putBoolean("nameEmpty", nameEmpty)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        _binding = null
        activity?.lifecycle?.removeObserver(activityObserver)
        super.onDestroy()
    }
}