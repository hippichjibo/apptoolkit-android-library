package com.jibo.apptoolkit.android.example.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatDialog
import com.jibo.apptoolkit.android.R

/**
 * Created by alexz on 31.10.17.
 */
/** @hide
 */
internal class ProgressFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(activity, R.style.ProgressDialog)
        val view = activity?.layoutInflater?.inflate(R.layout.fragment_progress, null)
        dialog.setContentView(view)
        isCancelable = false
        return dialog
    }

}