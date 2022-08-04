package com.faithdeveloper.giveaway.work

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.faithdeveloper.giveaway.data.Repository
import kotlinx.coroutines.CoroutineDispatcher

//
class GetAllUsersWorker(val appContext: Context, val workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override val coroutineContext: CoroutineDispatcher
        get() = super.coroutineContext

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return  ForegroundInfo(0, Notification()
//            NotificationUtil.getAllUsersNotificationID, NotificationUtil.getAllUsersNotification()
        )
    }

    override suspend fun doWork(): Result {
        return try {
            setForegroundAsync(getForegroundInfo())
            val repository = inputData.keyValueMap["repo"] as Repository
         //   repository.getAllUsers()
            Result.success()
        } catch (e:Exception){
            Log.e("GA", e.message?: "Failed to get users")
            Result.failure()
        }
    }
}