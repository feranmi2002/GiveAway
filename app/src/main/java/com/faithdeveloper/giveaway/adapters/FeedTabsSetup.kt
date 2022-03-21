package com.faithdeveloper.giveaway.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.faithdeveloper.giveaway.fragments.Exchange
import com.faithdeveloper.giveaway.fragments.Gift
import com.faithdeveloper.giveaway.fragments.Need

class FeedTabsSetup(fragment:Fragment ): FragmentStateAdapter(fragment) {
    override fun getItemCount() = 3

    override fun createFragment(position: Int) = when(position){
        0 -> Gift()
        1 -> Need()
        else ->  Exchange()
    }

}