package com.faithdeveloper.giveaway.fragments

import android.annotation.SuppressLint
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
import com.faithdeveloper.giveaway.Extensions.getUserDetails
import com.faithdeveloper.giveaway.Extensions.makeGone
import com.faithdeveloper.giveaway.Extensions.makeInVisible
import com.faithdeveloper.giveaway.Extensions.makeVisible
import com.faithdeveloper.giveaway.Extensions.showDialog
import com.faithdeveloper.giveaway.databinding.LayoutSignInBinding
import com.faithdeveloper.giveaway.viewmodels.SignInVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.properties.Delegates


class SignIn : Fragment() {
    private var _binding: LayoutSignInBinding? = null
    private val binding get() = _binding!!
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var dialog: AlertDialog? = null
    private lateinit var viewModel: SignInVM
    private var signUpVerifiedEmail by Delegates.notNull<Boolean>()
    private var forgotPassword by Delegates.notNull<Boolean>()
    private var unverifiedEmail by Delegates.notNull<Boolean>()
    private var passwordEmpty = true
    private var emailEmpty = true
    private lateinit var activityObserver: ActivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        arguments?.let {
            forgotPassword = it.getBoolean("forgotPassword")
            unverifiedEmail = it.getBoolean("unverifiedEmail")
            signUpVerifiedEmail = it.getBoolean("signUpVerifiedEmailSuccess")
        }

        savedInstanceState.apply {
            this?.let {
                passwordEmpty = it.getBoolean("passwordEmpty", true)
                emailEmpty = it.getBoolean("emailEmpty", true)
            }
        }
        activityObserver = object : ActivityObserver() {
            override fun onCreateAction() {
                viewModel = ViewModelProvider(
                    this@SignIn,
                    VMFactory((activity as MainActivity).getRepository())
                ).get(SignInVM::class.java)
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
        _binding = LayoutSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        onClickCreateAccount()
        onClickForgotPassword()
        handleViewPresentation()
        handleObserver()
        onClickContinue()
        onClickSignIn()
        super.onStart()
    }

    @SuppressLint("SetTextI18n")
    private fun handleObserver() {
        viewModel.result.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Event.Success -> {
                    when {
                        it.msg.contains("sign in successful", true) -> handleSignInSuccess()
                        it.msg.contains("password email sent", true) -> handlePasswordResetSuccess()
                        else -> handleVerifyEmailSuccess()
                    }
                }
                is Event.Failure -> {
                    when {
                        it.msg.contains("sign in failed", true) -> handleSignInFailure()
                        it.msg.contains(
                            "password reset failed",
                            true
                        ) -> handlePasswordResetFailure()
                        else -> handleVerifyEmailFailure()
                    }

                }
                is Event.InProgress -> {
                    // do nothing}
                }
            }
        })

        viewModel.timer.observe(viewLifecycleOwner, Observer {
            when {
                unverifiedEmail -> {
                    if (it.equals("0")) {
                        binding.emailLayout.enable()
                        binding.continueBtn.enable()
                        binding.forgotPassword.enable()
                        binding.signIn.enable()
                        binding.signUp.enable()
                        binding.info.text = getString(R.string.verify_email)
                    } else {
                        binding.emailLayout.disable()
                        binding.continueBtn.disable()
                        binding.forgotPassword.disable()
                        binding.signIn.disable()
                        binding.signUp.disable()
                        binding.info.text =
                            "Can't find verification link? Request in ${it} seconds"
                    }
                }
                forgotPassword -> {
                    if (it.equals("0")) {
                        binding.emailLayout.enable()
                        binding.signIn.enable()
                        binding.signUp.enable()
                        binding.continueBtn.enable()
                        binding.info.text = getString(R.string.resetPassword_text)
                    } else {
                        binding.emailLayout.disable()
                        binding.continueBtn.disable()
                        binding.signIn.disable()
                        binding.signUp.disable()
                        binding.info.text =
                            "Can't find password reset link? Request in ${it} seconds"
                    }
                }
            }
        })
    }

    private fun handleVerifyEmailFailure() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            message = "We couldn't complete your request.Try again?",
            positiveButtonText = "OK",
            positiveAction = {
                handleContinue()
            },
            negativeButtonText = "CANCEL",
            negativeAction = {
                //do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun handlePasswordResetFailure() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            message = "We couldn't complete your request.Try again?",
            positiveButtonText = "OK",
            positiveAction = {
                handleContinue()
            },
            negativeButtonText = "CANCEL",
            negativeAction = {
                //do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun handleSignInFailure() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            message = "We couldn't sign you in.Try again?",
            positiveButtonText = "OK",
            positiveAction = {
                handleContinue()
            },
            negativeButtonText = "CANCEL",
            negativeAction = {
                //do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun handleVerifyEmailSuccess() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            message = "Link to verify your email has been sent to ${
                binding.emailLayout.editText?.text.toString().trim()
            }",
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
        viewModel.startCounter()
    }

    private fun handlePasswordResetSuccess() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = true,
            message = "Link to reset your password has been sent to ${
                binding.emailLayout.editText?.text.toString().trim()
            }",
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
        viewModel.startCounter()
    }

    private fun handleSignInSuccess() {
        findNavController().navigate(SignInDirections.actionSignInToHome2())
    }

    private fun handleViewPresentation() {
        when {
            forgotPassword -> {
                binding.info.text = getString(R.string.resetPassword_text)
                binding.passwordLayout.makeGone()
                binding.continueBtn.text = getString(R.string.continue_button)
                passwordEmpty = false
                binding.forgotPassword.makeInVisible()
                binding.signIn.makeVisible()
                watchEmailBox()
            }
            unverifiedEmail -> {
                binding.continueBtn.text = getString(R.string.verify)
                binding.info.text = getString(R.string.verify_email)
                binding.passwordLayout.makeGone()
                watchEmailBox()
                passwordEmpty = false

            }
            signUpVerifiedEmail -> {
                if (!viewModel.timer.hasActiveObservers()) {
                    viewModel.startCounter()
                }
                binding.continueBtn.text = getString(R.string.verify)
                binding.passwordLayout.makeGone()
                watchEmailBox()
                passwordEmpty = false

            }
            else -> {
                binding.info.text = getString(R.string.sign_in)
                binding.passwordLayout.makeVisible()
                binding.continueBtn.text = getString(R.string.continue_button)
                binding.signIn.makeInVisible()
                binding.forgotPassword.makeVisible()
                watchPasswordBox()
                watchEmailBox()
            }
        }
    }

    private fun onClickSignIn() {
        binding.signIn.setOnClickListener {
            forgotPassword = false
            unverifiedEmail = false
            signUpVerifiedEmail = false
            handleViewPresentation()

        }
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
        if (!emailEmpty && !passwordEmpty) {
            binding.continueBtn.enable()
        } else {
            binding.continueBtn.disable()
        }
    }

    private fun handleContinue() {
        when {
            forgotPassword -> forgotPassword()
            unverifiedEmail -> verifyEmail()
            signUpVerifiedEmail -> verifyEmail()
            else -> signIn()
        }
    }

    private fun onClickContinue() {
        binding.continueBtn.setOnClickListener {
            handleContinue()
        }
    }

    private fun forgotPassword() {
        processDialog("Processing your request...")
        viewModel.forgotPassword(
            binding.emailLayout.editText?.text.toString().trim()
        )

    }

    private fun verifyEmail() {
        processDialog("Verifying your email...")
        viewModel.verifyEmail()
    }

    private fun signIn() {
        processDialog("Signing you in...")
        viewModel.signIn(
            binding.emailLayout.editText?.text.toString().trim(),
            binding.passwordLayout.editText?.text.toString().trim(),
            requireContext().getUserDetails()
        )
    }

    private fun processDialog(msg: String) {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = msg
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun onClickForgotPassword() {
        binding.forgotPassword.setOnClickListener {
            forgotPassword = true
            unverifiedEmail = false
            signUpVerifiedEmail = false
            handleViewPresentation()

        }
    }

    private fun onClickCreateAccount() {
        binding.signUp.setOnClickListener {
            findNavController().navigate(SignInDirections.actionSignInToSignUp())
        }
    }

    private fun watchPasswordBox() {
        binding.passwordLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun afterTextChanged(editable: Editable?) {
                editable?.let {
                    if (it.isNotEmpty() && it.length > 5
                    ) {
                        passwordEmpty = false
                        binding.passwordLayout.error = null
                    } else {
                        passwordEmpty = true
                        binding.passwordLayout.error = getString(R.string.password_characters)
                    }
                    handleContinueBtnEnablement()
                }
            }
        }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleContinueBtnEnablement()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putBoolean("passwordEmpty", passwordEmpty)
            putBoolean("emailEmpty", emailEmpty)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        _binding = null
        activity?.lifecycle?.removeObserver(activityObserver)

        super.onDestroyView()
    }
}