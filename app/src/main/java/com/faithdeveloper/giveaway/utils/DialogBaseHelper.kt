package com.faithdeveloper.giveaway.utils

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import com.faithdeveloper.giveaway.R
import com.faithdeveloper.giveaway.databinding.LinkLayoutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class DialogBaseHelper {
    private var addedLInk: String? = null

    abstract val itemsId: Int?
    abstract val checkedItems: BooleanArray?
    abstract val multiChoiceItemsId: Int?
    abstract val cancelable: Boolean
    abstract val context: Context
    abstract val title: String?
    abstract val message: CharSequence?
    abstract val positiveButtonText: String?
    abstract val negativeButtonText: String?
    abstract val view: LinkLayoutBinding?
    abstract val oldLink: String?
    abstract fun linkAction(link: String?)
    abstract fun positiveAction()
    abstract fun negativeAction()
    abstract fun itemsAction(itemClicked: Int)
    abstract fun multiChoiceItemsAction(itemClicked: Int, state: Boolean)

    lateinit var dialogBuilder: MaterialAlertDialogBuilder

    fun create(): MaterialAlertDialogBuilder {
        dialogBuilder = MaterialAlertDialogBuilder(context).setTitle(title)
            .setMessage(message)
            .setCancelable(cancelable)
        view?.let { binding ->
            binding.linkTextLayout.editText?.setLines(2)
            binding.linkTextLayout.editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    // do nothing
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    // do nothing
                }

                override fun afterTextChanged(editable: Editable?) {
                    editable?.let {
                        if (it.isNotEmpty() && android.util.Patterns.WEB_URL.matcher(editable)
                                .matches()
                        ) {
                            addedLInk = binding.linkTextLayout.editText?.text.toString()
                            binding.linkTextLayout.error = null
                        } else {
                            addedLInk = null
                            binding.linkTextLayout.error = context.getString(R.string.addLink_error)
                        }
                    }
                }
            })
            if (oldLink != null) binding.linkTextLayout.editText?.setText(oldLink!!)
            dialogBuilder.setView(binding.root)
        }
        positiveButtonText?.let {
            dialogBuilder.setPositiveButton(
                positiveButtonText,
                object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        positiveAction()
                        view?.let {
                            linkAction(addedLInk)
                        }
                    }
                })
        }
        negativeButtonText?.let {
            dialogBuilder.setNegativeButton(
                negativeButtonText,
                object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        negativeAction()
                    }
                })
        }
        itemsId?.let {
            dialogBuilder.setItems(it, object : DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    itemsAction(p1)
                }
            })
        }

        multiChoiceItemsId?.let {
            dialogBuilder.setMultiChoiceItems(
                it,
                checkedItems,
                object : DialogInterface.OnMultiChoiceClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int, p2: Boolean) {
                        multiChoiceItemsAction(p1, p2)
                    }
                })
        }
        return dialogBuilder
    }


}