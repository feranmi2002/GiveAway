package com.faithdeveloper.giveaway.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.databinding.LayoutUnverifiedEmailBinding
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.Extensions.disable
import com.faithdeveloper.giveaway.utils.Extensions.enable
import com.faithdeveloper.giveaway.utils.Extensions.makeGone
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.viewmodels.UserUnverifiedVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseNetworkException

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
        handleObserver()

        binding.verify.setOnClickListener {
            handleVerify()
        }

        binding.signIn.setOnClickListener {
            findNavController().navigate(UserUnverifiedDirections.actionUserUnverifiedToSignIn())
        }

        if (verificationLinkAlreadySent) {
            binding.verify.disable()
            binding.signIn.disable()
            binding.time.makeVisible()
            viewModel.startCounter()
            verificationLinkAlreadySent = false
        }
        binding.emailLayout.editText!!.setText(emailAddress ?: viewModel.getUserEmail())
        binding.emailLayout.disable()
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
                showVerificationSuccessDialog()
                binding.signIn.disable()
                binding.verify.disable()
                binding.time.makeVisible()
                viewModel.startCounter()
            } else {
                when (it.data) {
                    is FirebaseNetworkException -> "We couldn't verify your email. Check your internet connection and try again".showVerificationFailureDialog()
                    else -> "This email isn't matched with any account. Ensure you enter the email with which you created your Connect account".showVerificationFailureDialog()
                }

            }
        }
        viewModel.timer.observe(viewLifecycleOwner) {
            binding.time.text = DateUtils.formatElapsedTime(it / 1000)
            if (it < 1000) {
                binding.signIn.enable()
                binding.verify.enable()
                binding.time.makeGone()
            }
        }
    }

    private fun showVerificationSuccessDialog() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "Link to verify your email has been sent to ${
                binding.emailLayout.editText?.text.toString().trim()
            }. Go to your inbox to complete your registration.",
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    private fun handleVerify() {
        processDialog()
        viewModel.verifyEmail()
    }

    private fun String.showVerificationFailureDialog() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = this,
            positiveButtonText = "OK",
            positiveAction = {
                // do nothing
            }
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}