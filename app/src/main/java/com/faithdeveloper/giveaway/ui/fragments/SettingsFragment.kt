package com.faithdeveloper.giveaway.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.faithdeveloper.giveaway.*
import com.faithdeveloper.giveaway.utils.Extensions.clearPreferenceDueToSignOut
import com.faithdeveloper.giveaway.utils.Extensions.showDialog
import com.faithdeveloper.giveaway.utils.Extensions.showSnackbarShort
import com.faithdeveloper.giveaway.utils.Extensions.storeCommentsMode
import com.faithdeveloper.giveaway.utils.Extensions.storeDataSavingMode
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.utils.ActivityObserver
import com.faithdeveloper.giveaway.utils.Event
import com.faithdeveloper.giveaway.utils.VMFactory
import com.faithdeveloper.giveaway.viewmodels.SettingsVM
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : PreferenceFragmentCompat() {
    private var dialogBuilder: MaterialAlertDialogBuilder? = null
    private var dialog: AlertDialog? = null
    private lateinit var viewModel: SettingsVM
    private lateinit var activityObserver: ActivityObserver
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

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
                    this@SettingsFragment,
                    VMFactory(
                        (requireActivity() as MainActivity).getRepository()
                    )
                ).get(SettingsVM::class.java)
            }
        }
        activity?.lifecycle?.addObserver(activityObserver)
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        val profileEdit = findPreference<Preference>("profileEdit")
        profileEdit?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToProfileEdit())
            return@setOnPreferenceClickListener true
        }
        val signOut = findPreference<Preference>("signOut")
        signOut?.setOnPreferenceClickListener {
            dialog?.dismiss()
            dialogBuilder = requireContext().showDialog(
                cancelable = true,
                title = "Sign Out",
                message = "Continue to sign you out of your account?",
                positiveButtonText = "Continue",
                positiveAction = {
                    signUserOut()
                },
                negativeButtonText = "Cancel",
                negativeAction = {
                    // do nothing
                }
            )
            dialog = dialogBuilder?.create()
            dialog?.show()
            return@setOnPreferenceClickListener true

        }
        val contactDev = findPreference<Preference>("contactDev")
        contactDev?.setOnPreferenceClickListener {
            val recipientArray = arrayOf("appgiveaway2022@gmail.com")
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, recipientArray)
                putExtra(Intent.EXTRA_TEXT, "I saw your advert on the Giveaway app...")
            }
            return@setOnPreferenceClickListener if (intent.resolveActivity(this.requireActivity().packageManager) != null) {
                startActivity(intent)
                true
            } else {
                requireContext().showSnackbarShort(
                    requireView(),
                    "There is no app to perform this action on this device"
                )
                false
            }

        }
        val dataSaving = findPreference<SwitchPreferenceCompat>("dataSaving")
        dataSaving?.setOnPreferenceChangeListener { preference, newValue ->
            requireContext().storeDataSavingMode(newValue as Boolean)
            return@setOnPreferenceChangeListener true
        }

        val comments = findPreference<SwitchPreferenceCompat>("comments")
        comments?.setOnPreferenceChangeListener { preference, newValue ->
            requireContext().storeCommentsMode(newValue as Boolean)
            return@setOnPreferenceChangeListener true
        }
        handleObserver()
        super.onStart()
    }

    private fun signUserOut() {
        dialog?.dismiss()
        dialogBuilder = requireContext().showDialog(
            cancelable = false,
            message = "Signing you out of your account"
        )
        dialog = dialogBuilder?.create()
        dialog?.show()
        viewModel.signUserOut()
    }

    private fun handleObserver() {
        viewModel.result.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Event.Success -> {
                    dialog?.dismiss()
                    preferenceManager.sharedPreferences?.edit()?.clear()?.apply()
                    requireContext().clearPreferenceDueToSignOut()
                    findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToSignUp())
                }
                else -> {
                    dialog?.dismiss()
                    dialogBuilder = requireContext().showDialog(
                        cancelable = true,
                        message = "Couldn't sign you out at this time",
                        positiveButtonText = "Retry",
                        positiveAction = {
                            signUserOut()
                        },
                        negativeButtonText = "Cancel",
                        negativeAction = {
                            // do nothing
                        }
                    )
                    dialog = dialogBuilder?.create()
                    dialog?.show()

                }
            }
        })
    }

    override fun onDestroyView() {
        activity?.lifecycle?.removeObserver(activityObserver)
        super.onDestroyView()
    }
}