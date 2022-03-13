package com.faithdeveloper.giveaway.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.faithdeveloper.giveaway.Extensions.disable
import com.faithdeveloper.giveaway.Extensions.enable
import com.faithdeveloper.giveaway.Extensions.makeGone
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.VMFactory
import com.faithdeveloper.giveaway.databinding.LayoutSignInBinding
import com.faithdeveloper.giveaway.viewmodels.SignInVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class SignIn : Fragment() {
    private var _binding: LayoutSignInBinding? = null
    private val binding get() = _binding!!
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var dialog: AlertDialog? = null
    private lateinit var viewModel: SignInVM
    private var forgotPassword = false
    private var unverifiedEmail = false
    private var passwordEmpty = true
    private var emailEmpty = true

    override fun onCreate(savedInstanceState: Bundle?) {
        arguments?.let {
            forgotPassword = it.getBoolean("forgotPassword")
            unverifiedEmail = it.getBoolean("unverifiedEmail")
        }
        viewModel = ViewModelProvider(
            this,
            VMFactory((activity as MainActivity).getRepository())
        ).get(SignInVM::class.java)
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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
        handleViewPresentation()

        super.onStart()
    }

    private fun handleViewPresentation() {
        if (forgotPassword) {
            binding.signIn.text = getString(R.string.resetPassword_text)
            binding.passwordLayout.makeGone()
            passwordEmpty = false
            watchEmailBox()

            return
        }
        if (unverifiedEmail) {
            binding.continueBtn.text = getString(R.string.verify)
            binding.signIn.text = getString(R.string.verify_email)
            binding.passwordLayout.makeGone()
            watchEmailBox()
            passwordEmpty = false
            return
        }
        // Sign in
        watchPasswordBox()
        watchEmailBox()
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
                    handleContinueBtn()
                }
            }

        })
    }

    private fun handleContinueBtn() {
        if (!emailEmpty && !passwordEmpty) {
            binding.continueBtn.enable()
        } else {
            binding.continueBtn.disable()
        }
    }

    private fun watchPasswordBox() {
        binding.passwordLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                text?.let {
                    if (it.isEmpty() || it.length < 6) {

                    }
                }
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
                    handleContinueBtn()
                }
            }
        }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}