package com.faithdeveloper.giveaway.utils

sealed class Event(val data: Any?, val msg: String){
    class Success(data:Any?, msg:String = "Success"): Event(data, msg)
    class InProgress(data:Any?, msg:String = "In progress"): Event(data, msg)
    class Failure(data:Any?, msg:String = "Failure"): Event(data, msg)


}
