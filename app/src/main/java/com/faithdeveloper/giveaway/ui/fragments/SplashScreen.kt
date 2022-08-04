package com.faithdeveloper.giveaway.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.faithdeveloper.giveaway.utils.Extensions.getUserDetails
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.databinding.LayoutSplashscreenBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreen : Fragment() {
    // initialize view
    private lateinit var repository: Repository
    private var _binding: LayoutSplashscreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutSplashscreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // get the repository used to check if user is registered
        val activity = activity as MainActivity
        repository = activity.getRepository()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        lifecycleScope.launch {
            // delay a little to show app logo
            delay(500)
            //check the user complete registration
            checkRegistration()
        }
        super.onStart()
    }

    private fun checkRegistration() {
        if (requireActivity().intent.data != null) {
            // user is directed from the web either after email verification or change of password
            findNavController().navigate(SplashScreenDirections.actionSplashScreenToSignIn())
        } else if (repository.checkUserRegistration() == null) {
            // no user is registered on the device
            findNavController().navigate(SplashScreenDirections.actionSplashScreenToSignUp())
        } else if (repository.checkUserRegistration() != null && requireContext().getUserDetails()[0] == null) {
            /*there is already a registered user on the app but the user has signed out. When a
            user is signed out, his personal details are deleted but the firebase cached data about the user still remains on the device. So,
            firebase still recognizes that there is a user is on the device, but the personal details have been removed from the device,
            the user has to sign in again so that the personal details are restored.
            * */
            findNavController().navigate(SplashScreenDirections.actionSplashScreenToSignIn())
        } else {
            // user email hasn't been verified
            if (!repository.emailIsVerified()!!) findNavController().navigate(
                SplashScreenDirections.actionSplashScreenToSignIn(
                    true
                )
            )
            // users is fully registered on the app
            else findNavController().navigate(SplashScreenDirections.actionSplashScreenToHome2())
        }
    }


    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}