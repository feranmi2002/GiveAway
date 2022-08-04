package com.faithdeveloper.giveaway.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

abstract class ActivityObserver: DefaultLifecycleObserver {
    abstract fun onCreateAction()
    abstract fun onResumeAction()
    abstract fun onPauseAction()

    override fun onCreate(owner: LifecycleOwner) {
        onCreateAction()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        onResumeAction()
        super.onResume(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        onPauseAction()
        super.onPause(owner)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
    }
}