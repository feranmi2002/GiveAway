package com.faithdeveloper.giveaway.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.faithdeveloper.giveaway.*
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.databinding.LayoutSignInBinding
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.disable
import com.faithdeveloper.giveaway.utils.Extensions.enable
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.viewmodels.SignInVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class SignIn : Fragment() {
    // init properties
    private var _binding: LayoutSignInBinding? = null
    private val binding get() = _binding!!
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var dialog: AlertDialog? = null
    private lateinit var viewModel: SignInVM
    private lateinit var activityObserver: ActivityObserver

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        watchEmailBox()
        watchPasswordBox()
        // initialize live data observer
        handleObserver()

        /*This functions navigate to their respective fragments*/
        onClickCreateAccount()

//        sends link to user's mail to reset password
        onClickForgotPassword()

        // handles required action
        loginBtnClick()
        super.onViewCreated(view, savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    private fun handleObserver() {
        viewModel.result.observe(viewLifecycleOwner, Observer {
            when (it) {
                // successful requests
                is Event.Success -> {
                    when {
                        // successful sign in
                        it.msg.contains("sign in successful", true) -> handleSignInSuccess()
                        // link to reset email successfully sent to user email
                        it.msg.contains("password email sent", true) -> handlePasswordResetSuccess()
                    }
                }
                // failed requests
                is Event.Failure -> {
                    when {
                        it.msg.contains("sign in failed", true) -> handleSignInFailure()
                        it.msg.contains(
                            "password reset failed",
                            true
                        ) -> handlePasswordResetFailure()
                        it.msg.contains("Email unverified", true) -> handleUnverifiedEmail()
                    }
                }
                is Event.InProgress -> {
                    // do nothing}
                }
            }
        })

        viewModel.timer.observe(viewLifecycleOwner, Observer {
            binding.forgotPassword.text = DateUtils.formatElapsedTime(it / 1000)
            if (it < 1)
                binding.loginBtn.enable()
            binding.signUp.enable()
            binding.forgotPassword.text = "Forgot Password?"
            binding.forgotPassword.isClickable = true

        })
    }

    private fun handleUnverifiedEmail() {
        findNavController().navigate(SignUpDirections.actionSignUpToUserUnverified(binding.emailLayout.editText?.text.toString()))
    }

    private fun handlePasswordResetFailure() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "We couldn't complete your request.Try again?",
            positiveButtonText = "OK",
            positiveAction = {
                viewModel.forgotPassword(binding.emailLayout.editText?.text.toString().trim())
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
            cancelable = false,
            message = "We couldn't sign you in.Try again?",
            positiveButtonText = "OK",
            positiveAction = {
                signIn()
            },
            negativeButtonText = "CANCEL",
            negativeAction = {
                //do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
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
        binding.loginBtn.disable()
        binding.signUp.disable()
        binding.forgotPassword.isClickable = false
        viewModel.startCounter()
    }

    private fun handleSignInSuccess() {
        dialog?.dismiss()
        hideKeyboard()
        findNavController().navigate(SignInDirections.actionSignInToHome2())
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            binding.root.windowToken,
            InputMethodManager.RESULT_UNCHANGED_SHOWN
        )
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
                        binding.emailLayout.error = null
                    } else {
                        binding.emailLayout.error = getString(R.string.enter_correct_email)
                    }
                }
            }

        })
    }

    private fun loginBtnClick() {
        binding.loginBtn.setOnClickListener {
            if (binding.emailLayout.error == null && binding.emailLayout.editText!!.text.isNotEmpty() && binding.emailLayout.editText!!.text.isNotBlank()
                && binding.passwordLayout.error == null && binding.passwordLayout.editText!!.text.isNotEmpty() && binding.passwordLayout.editText!!.text.isNotBlank()
            ) signIn()
            else requireContext().showSnackbarShort(binding.root, "Enter your login details")
        }
    }

    private fun signIn() {
        processDialog("Signing you in...")
        viewModel.signIn(
            binding.emailLayout.editText?.text.toString().trim(),
            binding.passwordLayout.editText?.text.toString().trim()
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
//            checks if  a correct email has been entered
            if (binding.emailLayout.error == null && binding.emailLayout.editText?.text.toString()
                    .isNotBlank()
                && binding.emailLayout.editText?.text.toString().isNotEmpty()
            ) {
                processDialog("Processing your request...")
                viewModel.forgotPassword(binding.emailLayout.editText?.text.toString().trim())
            } else requireContext().showSnackbarShort(binding.root, "Enter your email")
        }
    }

    private fun onClickCreateAccount() {
        binding.signUp.setOnClickListener {
            dialog?.dismiss()
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
                        binding.passwordLayout.error = null
                    } else {
                        binding.passwordLayout.error = getString(R.string.password_characters)
                    }
                }
            }
        }
        )
    }


    override fun onDestroyView() {
        _binding = null
        activity?.lifecycle?.removeObserver(activityObserver)

        super.onDestroyView()
    }
}