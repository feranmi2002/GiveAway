package com.faithdeveloper.giveaway

import android.content.Context
import android.view.View
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
        negativeAction: (() -> Unit)? = null
    ): MaterialAlertDialogBuilder {
        class DialogHelper : DialogBaseHelper() {

            override val context: Context
                get() = this@showDialog
            override val positiveButtonText: String?
                get() = positiveButtonText
            override val negativeButtonText: String?
                get() = negativeButtonText

            override fun positiveAction() {
                positiveAction?.invoke()
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

    fun View.makeInVisible(){
        this.isVisible = false
    }

    fun View.makeVisible(){
        this.isVisible = true
    }

    fun View.makeGone(){
        this.visibility = View.GONE
    }

    fun View.enable(){
        this.isEnabled = true
    }
    fun View.disable(){
        this.isEnabled = false
    }
}
