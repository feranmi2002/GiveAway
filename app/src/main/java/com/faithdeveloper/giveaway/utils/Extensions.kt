package com.faithdeveloper.giveaway.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.databinding.LinkLayoutBinding
import com.faithdeveloper.giveaway.ui.fragments.CommentsBottomSheet
import com.faithdeveloper.giveaway.ui.fragments.FeedDirections
import com.faithdeveloper.giveaway.ui.fragments.FullPostMediaBottomSheet
import com.faithdeveloper.giveaway.utils.interfaces.FragmentCommentsInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.net.URLEncoder
import java.util.*

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
        itemsId: Int? = null,
        itemsAction: ((itemCLicked: Int) -> Unit)? = null,
        multiChoiceItemsId: Int? = null,
        multiChoiceItemsAction: ((itemCLicked: Int, state: Boolean) -> Unit)? = null,
        checkedItems: BooleanArray? = null,
        linkBinding: LinkLayoutBinding? = null,
        oldLink: String? = null,
        linkAction: ((link: String?) -> Unit)? = null
    ): MaterialAlertDialogBuilder {
        class DialogHelper : DialogBaseHelper() {
            override val view: LinkLayoutBinding?
                get() = linkBinding

            override val oldLink: String?
                get() = oldLink

            override fun linkAction(link: String?) {
                linkAction?.invoke(link)
            }

            override val context: Context
                get() = this@showDialog
            override val positiveButtonText: String?
                get() = positiveButtonText
            override val negativeButtonText: String?
                get() = negativeButtonText
            override val itemsId: Int?
                get() = itemsId
            override val checkedItems: BooleanArray?
                get() = checkedItems
            override val multiChoiceItemsId: Int?
                get() = multiChoiceItemsId

            override fun positiveAction() {
                positiveAction?.invoke()
            }

            override fun itemsAction(itemClicked: Int) {
                itemsAction?.invoke(itemClicked)
            }

            override fun multiChoiceItemsAction(itemClicked: Int, state: Boolean) {
                multiChoiceItemsAction?.invoke(itemClicked, state)
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

    fun Context.storeUserDetails(userName: String?, email: String?, phoneNumber: String?) {
        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit {
            putString("Username", userName)
            putString("Email", email)
            putString("Phone number", phoneNumber)
            apply()
        }
    }

    fun Context.storeTimelineOption(timelineOption: String) {
        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit {
            putString("TimelineOption", timelineOption)
            commit()
        }
    }

    fun Context.storeDataSavingMode(dataSavingMode: Boolean) {
        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit {
            putBoolean("DataSavingMode", dataSavingMode)
            commit()
        }
    }

    fun Context.getDataSavingMode() = getSharedPreferences(
        getString(R.string.app_name), Context.MODE_PRIVATE
    ).getBoolean("DataSavingMode", false)

    fun Context.storeCommentsMode(commentsMode: Boolean) {
        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit {
            putBoolean("CommentsMode", commentsMode)
            commit()
        }
    }

    fun Context.getCommentsMode() = getSharedPreferences(
        getString(R.string.app_name), Context.MODE_PRIVATE
    ).getBoolean("CommentsMode", false)

    fun Context.getTimelineOption() = getSharedPreferences(
        getString(R.string.app_name), Context.MODE_PRIVATE
    ).getString("TimelineOption", "All")

    fun Context.storeUserProfilePicUrl(profilePicUrl: String?) {
        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit {
            putString("ProfilePicUrl", profilePicUrl)
            commit()
        }
    }
//    fun Context.getAppMode() = getSharedPreferences(
//        getString(R.string.app_name), Context.MODE_PRIVATE
//    ).getString("AppMode", )
//
//    fun Context.storeAppMode(appMode: String){
//        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit{
//            putString("AppMode", appMode)
//        }
//    }

    fun Context.clearPreferenceDueToSignOut() {
        storeUserDetails(null, null, null)
        storeUserProfilePicUrl(null)
        setSignInStatus(false)
    }

    fun Context.getUserProfilePicUrl() = getSharedPreferences(
        getString(R.string.app_name), Context.MODE_PRIVATE
    ).getString("ProfilePicUrl", null)

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

    fun Context.getUserDetails() = arrayOf(
        getSharedPreferences(
            getString(R.string.app_name),
            Context.MODE_PRIVATE
        ).getString("Username", null),

        getSharedPreferences(
            getString(R.string.app_name),
            Context.MODE_PRIVATE
        ).getString("Phone number", null),

        getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).getString(
            "Email",
            null
        ),
        getSharedPreferences(
            getString(R.string.app_name), Context.MODE_PRIVATE
        ).getString("ProfilePicUrl", null)
    )

    fun Context.checkTypeOfMedia(uri: Uri): String {
        val mediaType = contentResolver.getType(uri)
        if (mediaType?.startsWith("image") == true)
            return "image"
        else return "video"
    }

    fun convertTime(date: Date?): String {
        val time = (date ?: Date()).time
        var timeDiff: String = ""
        val diff = System.currentTimeMillis().minus(time)
        if (diff <= 1000) {
            timeDiff = "1s"
        } else if (diff in 1001..60000) {
            timeDiff = "${diff / 1000}s"
        } else if (diff in 60001..3600000) {
            timeDiff = "${diff / 60000}m"
        } else if (diff in 3600001..86400000) {
            timeDiff = "${diff / 3600000}h"
        } else if (diff in 86400001..604800000) {
            timeDiff = "${diff / 86400000}d"
        } else if (diff in 604800001..2419200000) {
            timeDiff = "${diff / 604800000}w"
        } else if (diff in 2419200001..29030400000) {
            val result = diff / 2419200000
            if (result.compareTo(1) == 0) {
                timeDiff = "${result}mth"
            } else {
                timeDiff = "${result}mths"
            }
        } else {
            timeDiff = "${diff / 29030400000}y"
        }
        return timeDiff
    }

    fun formatCount(count: Int): String {
        var formattedCount = ""
        if (count == 0) {
            formattedCount = "0"
        } else if (count > 1000) {
            formattedCount = "${count.div(1000)}k"
        }else formattedCount = count.toString()
        return formattedCount
    }

    fun Fragment.sendWhatsapp(whatsappNumber: String): Event {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(
                "whatsapp://send?phone=$whatsappNumber&text=${
                    URLEncoder.encode(
                        "I saw your advert on the Giveaway app...",
                        "UTF-8"
                    )
                }"
            )
        }
        return if (intent.resolveActivity(this.requireActivity().packageManager) != null) {
            startActivity(intent)
            Event.Success("")
        } else {
            Event.Failure("")
        }

    }

    fun Fragment.showComments(
        postID: String,
        commentsCount: Int,
        fragmentCommentsInterface: FragmentCommentsInterface?
    ) {
        val commentsBottomSheet =
            CommentsBottomSheet.instance(postID, commentsCount, fragmentCommentsInterface)
        commentsBottomSheet.show(requireActivity().supportFragmentManager, CommentsBottomSheet.TAG)
    }

    fun Fragment.showMedia(media: Array<String>, mediaType: String, position: Int) {
        val mediaBtmSheet = FullPostMediaBottomSheet.instance(media, mediaType, position)
        mediaBtmSheet.show(requireActivity().supportFragmentManager, FullPostMediaBottomSheet.TAG)
    }

    fun Fragment.launchLink(link: String) {
        findNavController().navigate(FeedDirections.actionHomeToWebView2(link))
//        val intent = Intent(Intent.ACTION_VIEW).apply {
//            data = Uri.parse(link)
//        }
//        if (intent.resolveActivity(requireActivity().packageManager) != null) {
//            startActivity(intent)
//        } else {
//            requireContext().showSnackbarShort(
//                binding.root,
//                "There is no app to perform this action on your device"
//            )
//        }
    }

    fun Fragment.sendPhone(phoneNumber: String): Event {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        return if (intent.resolveActivity(this.requireActivity().packageManager) != null) {
            startActivity(intent)
            Event.Success("")
        } else {
            Event.Failure("")
        }
    }

    fun Fragment.sendEmail(emailAddress: String): Event {
        val recipientArray = arrayOf(emailAddress)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, recipientArray)
            putExtra(Intent.EXTRA_TEXT, "I saw your advert on the Giveaway app...")
        }
        return if (intent.resolveActivity(this.requireActivity().packageManager) != null) {
            startActivity(intent)
            Event.Success("")
        } else {
            Event.Failure("")
        }
    }

    fun Context.hideKeyboard(rootView: View) {
        val inputMethodManager =
            this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            rootView.windowToken,
            InputMethodManager.RESULT_UNCHANGED_SHOWN
        )
    }

    fun mediaSize(uri: Uri) = File(uri.path!!).length()


    const val USERS_DATABASE = "users"


}
