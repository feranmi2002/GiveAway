package com.faithdeveloper.giveaway.data

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.faithdeveloper.giveaway.Event
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class Repository(val auth:FirebaseAuth) {

    fun checkUserRegistration() = auth.currentUser

    suspend fun signUp(phone: String, name: String, email: String, password: String): Event {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Log.i("GA", "Account creation successful")
            Event.Success(data = null, msg = "Account creation successful" )
        }catch (e:Exception){
            Log.e("GA", "Failed to create account")
            Event.Failure(data = null, "Failed to create account")
        }
    }


    suspend fun verifyEmail(): Event {
        return try {
            val actionSettingsCodeInfo = ActionCodeSettings.newBuilder()
            actionSettingsCodeInfo.apply {
                handleCodeInApp = false
                url = "https://faithdeveloper.page.link.verification"
                dynamicLinkDomain = "faithdeveloper.page.link"
                setAndroidPackageName("com.faithdeveloper.giveaway", false, "1")
            }
            auth.currentUser!!.sendEmailVerification(actionSettingsCodeInfo.build()).await()
            Log.e("GA", "Email is verified")
            Event.Success(null, "Email verified")
        }catch (e:Exception){
            Log.e("GA", "Failed to verify email")
            Event.Failure(null, "Failed to verify email")
        }
    }

    fun emailIsVerified() = auth.currentUser?.isEmailVerified
}