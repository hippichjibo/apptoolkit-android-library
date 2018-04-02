package com.jibo.apptoolkit.android.example.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import com.jibo.apptoolkit.android.example.R

class ErrorDialog : DialogFragment() {

    private var mOnClickListener: DialogInterface.OnClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity as Context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(if (arguments?.containsKey(TITLE)!!)
                    arguments?.getInt(TITLE)!!
                          else
                              R.string.error)
                .setMessage(arguments?.getString(MESSAGE)!!)
                .setPositiveButton(android.R.string.ok
                                  ) { dialog, whichButton ->
                    doButtonClick(DialogInterface.BUTTON_POSITIVE, null)
                    dismiss()
                }
                .create()
    }

    fun setClickListener(mListener: DialogInterface.OnClickListener): ErrorDialog {
        this.mOnClickListener = mListener
        return this
    }

    protected fun doButtonClick(button: Int, message: String?) {
        if (mOnClickListener != null) {
            try {
                mOnClickListener?.onClick(dialog, button)
            } catch (ex: Exception) {
            }

        }
    }

    override fun show(manager: FragmentManager, tag: String) {
        try {
            val frag = manager.findFragmentByTag(tag)
            if (frag != null) {
                manager.beginTransaction().remove(frag).commit()
            }
            super.show(manager, tag)
        } catch (ex: Exception) {
        }

    }

    companion object {
        val TAG = ErrorDialog::class.java.simpleName

        val MESSAGE = "message"
        val TITLE = "title"

        fun newInstance(title: String?, ex: Exception): ErrorDialog {
            val frag = ErrorDialog()
            val args = Bundle()
            args.putString(MESSAGE, if (TextUtils.isEmpty(ex.localizedMessage))
                ex.toString()
            else
                ex.localizedMessage)
            if (title != null) args.putString(TITLE, title)
            frag.arguments = args
            return frag
        }

        fun newInstance(title: String?, message: String): ErrorDialog {
            val frag = ErrorDialog()
            val args = Bundle()
            args.putString(MESSAGE, message)
            if (title != null) args.putString(TITLE, title)
            frag.arguments = args
            return frag
        }
    }
}
