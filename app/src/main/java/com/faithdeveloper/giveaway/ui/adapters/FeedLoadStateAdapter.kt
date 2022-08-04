package com.faithdeveloper.giveaway.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.faithdeveloper.giveaway.utils.Extensions.makeInVisible
import com.faithdeveloper.giveaway.utils.Extensions.makeVisible
import com.faithdeveloper.giveaway.databinding.ErrorBinding

class FeedLoadStateAdapter(private val retry: () -> Unit) :
    LoadStateAdapter<FeedLoadStateAdapter.FeedLoadStateViewHolder>() {
    inner class FeedLoadStateViewHolder(val binding: ErrorBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(loadState: LoadState) {
            with(binding) {
                when (loadState) {
                    is LoadState.Error -> {
                        errorText.makeVisible()
                        retryButton.makeVisible()
                        progressCircular.makeInVisible()
                    }
                    is LoadState.Loading -> {
                        errorText.makeInVisible()
                        retryButton.makeInVisible()
                        progressCircular.makeVisible()
                    }
                    else -> {
                        // do nothing
                    }
                }
                retryButton.setOnClickListener {
                    retry.invoke()
                }

            }
        }
    }

        override fun onBindViewHolder(holder: FeedLoadStateViewHolder, loadState: LoadState) {
            holder.bind(loadState)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            loadState: LoadState
        ): FeedLoadStateViewHolder {
            val binding = ErrorBinding.inflate(LayoutInflater.from(parent.context))
            return FeedLoadStateViewHolder(binding)
        }
    }