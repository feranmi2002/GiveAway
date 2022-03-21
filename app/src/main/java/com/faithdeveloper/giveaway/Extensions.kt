package com.faithdeveloper.giveaway

import android.content.Context
import android.view.View
import androidx.core.content.edit
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

object Extensions {
    fun Context.showSnackbarShort(view: View, msg: String) {
        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
    }

    fun Context.showDialog(
        cancelable: Boolean, title: String? = null,
        message: String? = null, positiveButtonText: String? = null,
        negativeButtonText: String? = null,
        positiveAction: (() -> Unit)? = null,
        negativeAction: (() -> Unit)? = null,
        itemsId:Int? = null,
        itemsAction: ((itemCLicked:Int) -> Unit)? = null
    ): MaterialAlertDialogBuilder {
        class DialogHelper : DialogBaseHelper() {

            override val context: Context
                get() = this@showDialog
            override val positiveButtonText: String?
                get() = positiveButtonText
            override val negativeButtonText: String?
                get() = negativeButtonText
            override val itemsId: Int?
                get() = itemsId

            override fun positiveAction() {
                positiveAction?.invoke()
            }

            override fun itemsAction(itemClicked: Int) {
                itemsAction?.invoke(itemClicked)
            }

            override fun negativeAction() {
                negativeAction?.invoke()
            }

            override val cancelable: Boolean
                get() = cancelable
            override val title: String?
                get() = title
            override val message: String?
                get() = message
        }

        val dialogHelper = DialogHelper()
        return dialogHelper.create()
    }

    fun View.makeInVisible() {
        this.isVisible = false
    }

    fun View.makeVisible() {
        this.isVisible = true
    }

    fun View.makeGone() {
        this.visibility = View.GONE
    }

    fun View.enable() {
        this.isEnabled = true
    }

    fun View.disable() {
        this.isEnabled = false
    }

    fun Context.storeUserDetails(userName: String, email: String, phoneNumber: String) {
        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit {
            putString("Username", userName)
            putString("Email", email)
            putString("Phone number", phoneNumber)
            commit()
        }
    }

    fun Context.setSignInStatus(value: Boolean) {
        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit {
            putBoolean("newSignIn", value)
            commit()
        }
    }

    fun Context.getSignInStatus() = getSharedPreferences(
        getString(R.string.app_name),
        Context.MODE_PRIVATE
    ).getBoolean("newSignIn", true)

//    fun Context.storeProfilePictureDownloadUrl(uri: Uri) {
//        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit {
//            putString("downloadUrl", uri.toString())
//            commit()
//        }
//    }
//
//    fun Context.getProfilePicDownloadUrl() = getSharedPreferences(
//        getString(R.string.app_name),
//        Context.MODE_PRIVATE
//    ).getString("downloadUrl", "")

    fun Context.getUserDetails() = arrayOf(
        getSharedPreferences(
            getString(R.string.app_name),
            Context.MODE_PRIVATE
        ).getString("Username", "")!!,
        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).getString(
            "Email",
            ""
        )!!,
        getSharedPreferences(
            getString(R.string.app_name),
            Context.MODE_PRIVATE
        ).getString("Phone number", "")!!
    )
}
