package com.faithdeveloper.giveaway

import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class DialogBaseHelper {

    abstract val itemsId:Int?
    abstract val cancelable:Boolean
    abstract val context: Context
    lateinit var dialogBuilder: MaterialAlertDialogBuilder
    abstract val title: String?
    abstract val message: String?
    abstract val positiveButtonText: String?
    abstract val negativeButtonText: String?
    abstract fun positiveAction()
    abstract fun negativeAction()
    abstract fun itemsAction(itemClicked: Int)

    fun create():MaterialAlertDialogBuilder {
        dialogBuilder = MaterialAlertDialogBuilder(context).
            setTitle(title)
            .setMessage(message)
            .setCancelable(cancelable)
            positiveButtonText?.let {
                dialogBuilder.setPositiveButton(positiveButtonText, object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        positiveAction()
                    }
                })
            }
            negativeButtonText?.let {
                dialogBuilder.setNegativeButton(negativeButtonText, object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        negativeAction()
                    }
                })
            }
        itemsId?.let {
            dialogBuilder.setItems(it, object : DialogInterface.OnClickListener{
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    itemsAction(p1)
                }
            })
        }
        return dialogBuilder
    }


    }