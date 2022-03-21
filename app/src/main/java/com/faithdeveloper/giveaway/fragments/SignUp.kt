package com.faithdeveloper.giveaway.fragments

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
import com.faithdeveloper.giveaway.*
import com.faithdeveloper.giveaway.Extensions.disable
import com.faithdeveloper.giveaway.Extensions.enable
import com.faithdeveloper.giveaway.Extensions.showDialog
import com.faithdeveloper.giveaway.databinding.LayoutAccountCreationBinding
import com.faithdeveloper.giveaway.viewmodels.SignUpVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SignUp : Fragment() {
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

        savedInstanceState.apply {
            this?.let {
                passwordEmpty = it.getBoolean("passwordEmpty", true)
                emailEmpty = it.getBoolean("emailEmpty", true)
                nameEmpty = it.getBoolean("nameEmpty", true)
                phoneEmpty = it.getBoolean("phoneEmpty", true)
            }
        }

        activityObserver = object : ActivityObserver() {
            override fun onCreateAction() {
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
        watchEmailBox()
        watchNameBox()
        watchPhoneBox()
        watchPasswordBox()
        handleTerms()
        handleForgotPassword()
        handleContinueButton()
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
                nameText?.let {
                    if (it.isNotEmpty()) {
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
        findNavController().navigate(SignUpDirections.actionSignUpToSignIn(unverifiedEmail = true))
    }

    private fun handleForgotPassword() {
        binding.forgotPassword.setOnClickListener {
            findNavController().navigate(SignUpDirections.actionSignUpToSignIn(forgotPassword = true))
        }

    }

    private fun handleTerms() {
        binding.termsCondition.setOnClickListener {
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
        binding.signIn.setOnClickListener {
            findNavController().navigate(SignUpDirections.actionSignUpToSignIn())
        }

    }

    private fun handleObserver() {
        viewModel.result.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is Event.Success -> {
                    if (event.msg.contains("Account creation successful")) showVerificationInProgressDialog()
                    else if (event.msg.contains("Email verified")) handleEmailVerificationSuccess()
                }
                is Event.Failure -> {
                    if (event.msg.contains("Failed to create account")) showAccountCreationFailureDialog()
                    else if (event.msg.contains("Failed to verify email")) showVerificationFailureDialog()

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
                binding.emailLayout.editText?.toString()?.trim()
            }",
            positiveButtonText = "OK",
            positiveAction = {
                findNavController().navigate(
                    SignUpDirections.actionSignUpToSignIn(
                        signUpVerifiedEmailSuccess = true
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
            cancelable = true,
            message = "We couldn't verify your account. ",
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

    private fun showAccountCreationFailureDialog() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            message = "We couldn't create your account. ",
            positiveButtonText = "TRY AGAIN",
            negativeButtonText = "OK",
            positiveAction = {
                signUp()
            },
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
        binding.continueBtn.setOnClickListener {
            signUp()
        }
    }

    private fun signUp() {
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
            putBoolean("emailEmpty",emailEmpty)
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