package com.faithdeveloper.giveaway.utils.interfaces

interface FullImageAdapterInterface {
    fun updateCount(position:Int, size:Int)
    fun mediaAvailabilityState(state:Boolean, mediaUrl:String?)
}