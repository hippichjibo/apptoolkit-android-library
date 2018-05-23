package com.jibo.apptoolkit.android.example.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.widget.TextView

class LogDialog : DialogFragment() {

    private var mOnClickListener: DialogInterface.OnClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity as Context)
                .setMessage(arguments?.getString(MESSAGE))
                .setPositiveButton(android.R.string.ok) { dialog, whichButton ->
                    doButtonClick(DialogInterface.BUTTON_POSITIVE)
                    dismiss()
                }
                .setNeutralButton("REFRESH", null)
                .create()
    }

    override fun onResume() {
        super.onResume()
        dialog.findViewById<TextView>(android.R.id.message).textSize = 8f
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { doButtonClick(DialogInterface.BUTTON_NEUTRAL) }
    }

    fun setClickListener(mListener: DialogInterface.OnClickListener): LogDialog {
        this.mOnClickListener = mListener
        return this
    }

    protected fun doButtonClick(button: Int) {
        try {
            mOnClickListener?.onClick(dialog, button)
        } catch (ex: Exception) {
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

    fun updateMessage(message: String) {
        if (dialog != null && dialog.isShowing) {
            dialog.findViewById<TextView>(android.R.id.message).text = message
        }
    }

    companion object {
        val TAG = LogDialog::class.java.simpleName

        val MESSAGE = "message"

        fun newInstance(message: String): LogDialog {
            val frag = LogDialog()
            val args = Bundle()
            args.putString(MESSAGE, message)
            frag.arguments = args
            return frag
        }
    }
}
