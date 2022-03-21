package com.faithdeveloper.giveaway.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import com.faithdeveloper.giveaway.Event
import com.faithdeveloper.giveaway.Extensions.storeUserDetails
import com.faithdeveloper.giveaway.UserProfile
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class Repository(private val auth: FirebaseAuth, val context: Context) {

    private var database: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null

    fun checkUserRegistration() = auth.currentUser

    suspend fun signUp(phone: String, name: String, email: String, password: String): Event {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Log.i("GA", "Account creation successful")
            context.storeUserDetails(name, email, phone)
            Event.Success(data = null, msg = "Account creation successful")
        } catch (e: Exception) {
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
                setAndroidPackageName("com.faithdeveloper.giveaway", true, "1")
            }
            auth.currentUser!!.sendEmailVerification(actionSettingsCodeInfo.build()).await()
            Log.i("GA", "Email is verified")
            Event.Success(null, "Email verified")
        } catch (e: Exception) {
            Log.e("GA", "Failed to verify email")
            Event.Failure(null, "Failed to verify email")
        }
    }

    fun emailIsVerified() = auth.currentUser?.isEmailVerified

    suspend fun forgotPassword(email: String): Event {
        return try {
            val actionCodeSettings = ActionCodeSettings.newBuilder()
            actionCodeSettings.apply {
                handleCodeInApp = false
                url = "https://faithdeveloper.page.link.pass"
                dynamicLinkDomain = "faithdeveloper.page.link"
                setAndroidPackageName("com.faithdeveloper.giveaway", true, "1")
            }
            auth.sendPasswordResetEmail(email, actionCodeSettings.build()).await()
            Log.i("GA", "Password email sent")
            Event.Success(null, "Password email sent")
        } catch (e: Exception) {
            Log.e("GA", "Password reset failed")
            Event.Failure(null, "Password reset failed")
        }
    }

    suspend fun signIn(email: String, password: String, userDetails: Array<String>): Event {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Log.i("GA", "Sign in successful")
            val reference = database().collection(USERS).document(userUid()!!).get().await()
            if (reference.exists()) {
                // do nothing
            } else {
                createUserProfile(
                    userDetails[USERNAME],
                    userDetails[PHONE_NUMBER],
                    userDetails[EMAIL]
                )
                Log.i("GA", "Created profile successfully")
            }

            Event.Success(null, "Sign in successful")
        } catch (e: Exception) {
            Log.e("GA", "Sign in failed")
            Log.e("GA", e.message!!)
            Event.Failure(null, "Sign in failed")
        }
    }

    private suspend fun createUserProfile(
        name: String,
        phoneNumber: String,
        email: String,
    ) {
        database().collection(USERS).document(userUid()!!).set(
            UserProfile(
                userUid()!!,
                name,
                phoneNumber,
                email,
                reports = 0
            )
        ).await()
    }

//    suspend fun uploadProfilePicDownload(name: String, phoneNumber: String, email: String) {
//        try {
//            database().collection(USERS).add(
//                UserProfile(
//                    userUid()!!,
//                    name,
//                    phoneNumber,
//                    email,
//                    storage().getReference(
//                        PROFILE_PHOTOS
//                    ).child(userUid()!!).downloadUrl.await().toString(),
//                    reports = 0
//                )
//            )
//
//        } catch (e: Exception) {
//
//        }
//    }

    suspend fun createProfilePicture(profilePicUri: Uri): Event {
        return try {
            val reference = storage().getReference(PROFILE_PHOTOS).child(userUid()!!)
            reference.putBytes(compressPicture(profilePicUri)).await()
            Event.Success(null, "Profile pic update successful")
        } catch (e: Exception) {
            Log.e("GA", e.message!!)
            Event.Failure(null, "Profile pic update failed")
        }
    }

//    suspend fun checkIfProfilePicIsUploaded(): Event {
//        return try {
//            val profilePic =
//                storage().getReference(PROFILE_PHOTOS).child(userUid()!!).getBytes(2048).await()
//            Event.Success(profilePic)
//        } catch (e: Exception) {
//            Event.Failure(null)
//        }
//    }

    private suspend fun compressPicture(profilePicUri: Uri): ByteArray {
        return withContext(coroutineContext) {
            Compressor.compress(context, profilePicUri.toFile()) {
                size(MAX_PROFILE_PHOTO_SIZE.toLong())
                quality(PICTURE_QUALITY)
                format(Bitmap.CompressFormat.PNG)
            }.readBytes()
        }
    }


    private fun database() = database ?: Firebase.firestore
    private fun storage() = storage ?: FirebaseStorage.getInstance()
    private fun userUid() = auth.currentUser?.uid

    companion object {
        const val PICTURE_QUALITY = 80
        const val MAX_PROFILE_PHOTO_SIZE = 1048576
        const val USERS = "users"
        const val USERNAME = 0
        const val EMAIL = 1
        const val PHONE_NUMBER = 2
        const val PROFILE_PHOTOS = "Profile_pictures"
    }
}