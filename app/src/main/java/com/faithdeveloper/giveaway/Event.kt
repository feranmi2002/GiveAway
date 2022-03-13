package com.faithdeveloper.giveaway

sealed class Event(val data: Any?, val msg: String){
    class Success(data:Any?, msg:String = "Success"):Event(data, msg)
    class InProgress(data:Any?, msg:String):Event(data, msg)
    class Failure(data:Any?, msg:String):Event(data, msg)


}
