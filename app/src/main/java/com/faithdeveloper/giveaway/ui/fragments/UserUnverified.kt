package com.faithdeveloper.giveaway.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.databinding.LayoutUnverifiedEmailBinding
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.disable
import com.faithdeveloper.giveaway.utils.Extensions.enable
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.viewmodels.UserUnverifiedVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UserUnverified : Fragment() {

    // init properties
    private var _binding: LayoutUnverifiedEmailBinding? = null
    private val binding get() = _binding!!
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var dialog: AlertDialog? = null
    private lateinit var viewModel: UserUnverifiedVM
    private lateinit var activityObserver: ActivityObserver
    private var emailAddress: String? = null
    private var verificationLinkAlreadySent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        emailAddress = arguments?.getString("email")
        verificationLinkAlreadySent = arguments?.getBoolean("linkSent", false) == true
        activityObserver = object : ActivityObserver() {
            override fun onResumeAction() {
                (activity as MainActivity).getRepository().setAppState(Repository.APP_STARTED)
            }

            override fun onPauseAction() {
                (activity as MainActivity).getRepository().setAppState(Repository.APP_PAUSED)
            }

            override fun onCreateAction() {
                viewModel = ViewModelProvider(
                    this@UserUnverified,
                    VMFactory((activity as MainActivity).getRepository())
                ).get(UserUnverifiedVM::class.java)
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
        _binding = LayoutUnverifiedEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        watchEmailBox()
        handleObserver()

        binding.verify.setOnClickListener {
            handleVerify()
        }

        if (verificationLinkAlreadySent) {
            binding.verify.disable()
            viewModel.startCounter()
            verificationLinkAlreadySent = false
        }

        if (emailAddress != null) {
            binding.emailLayout.editText!!.setText(emailAddress!!)
            handleVerify()
            emailAddress = null
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun processDialog() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "Verifying your email..."
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    @SuppressLint("SetTextI18n")
    private fun handleObserver() {
        viewModel.result.observe(viewLifecycleOwner) {
            if (it is Event.Success) {
                binding.verify.disable()
                viewModel.startCounter()
            } else showVerificationFailureDialog()
        }
        viewModel.timer.observe(viewLifecycleOwner) {
            binding.verify.text = DateUtils.formatElapsedTime(it / 1000)
            if (it < 1)
                binding.verify.enable()
            binding.verify.text = "Verify"
        }
    }

    private fun handleVerify() {
        processDialog()
        viewModel.verifyEmail(binding.emailLayout.editText!!.text.toString().trim())
    }

    private fun showVerificationFailureDialog() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "We couldn't verify your account. ",
            positiveButtonText = "TRY AGAIN",
            negativeButtonText = "CANCEL",
            positiveAction = {
                viewModel.verifyEmail(binding.emailLayout.editText!!.text.toString().trim())
            },
            negativeAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
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
                        binding.verify.enable()
                    } else {
                        binding.emailLayout.error = getString(R.string.enter_correct_email)
                    }
                }
            }

        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}