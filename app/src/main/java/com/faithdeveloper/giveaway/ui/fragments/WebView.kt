package com.faithdeveloper.giveaway.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.faithdeveloper.giveaway.databinding.LayoutWebviewBinding
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible

class WebView : Fragment() {
    private var _binding: LayoutWebviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        try {
            binding.webView.settings.javaScriptEnabled = true
            binding.webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressCircular.makeVisible()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressCircular.makeInVisible()
                }
            }
            arguments?.getString("url")?.let {
                binding.webView.loadUrl(it)
            }
        } catch (e: Exception) {
            findNavController().popBackStack()
        }

        super.onStart()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}