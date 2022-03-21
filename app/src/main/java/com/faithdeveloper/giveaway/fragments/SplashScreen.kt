package com.faithdeveloper.giveaway.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.faithdeveloper.giveaway.MainActivity
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.databinding.LayoutSplashscreenBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreen : Fragment() {
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
        val activity = activity as MainActivity
        repository = activity.getRepository()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        lifecycleScope.launch {
            delay(1000)
            //check the user complete registration
            checkRegistration()
        }
        super.onStart()
    }

    private fun checkRegistration() {
        if (requireActivity().intent.data!=null){
            findNavController().navigate(SplashScreenDirections.actionSplashScreenToSignIn())
        }
        else if (repository.checkUserRegistration() == null){
            findNavController().navigate(SplashScreenDirections.actionSplashScreenToSignUp())
        }else {
            if (!repository.emailIsVerified()!!) findNavController().navigate(SplashScreenDirections.actionSplashScreenToSignIn(true))
            else findNavController().navigate(SplashScreenDirections.actionSplashScreenToHome2())
        }
    }


    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}