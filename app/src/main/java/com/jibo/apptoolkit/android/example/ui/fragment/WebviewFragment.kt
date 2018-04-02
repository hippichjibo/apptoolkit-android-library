package com.jibo.apptoolkit.android.example.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatDialog
import com.jibo.apptoolkit.android.example.R
import kotlinx.android.synthetic.main.fragment_webview.view.*

/**
 * Created by alexz on 02.11.17.
 */

class WebviewFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
        val view = activity?.layoutInflater?.inflate(R.layout.fragment_webview, null)

        val webSettings = view?.webview?.settings
        webSettings?.javaScriptEnabled = true

        if (savedInstanceState != null) {
            view?.webview?.loadUrl(savedInstanceState.getString(PARAM_URL))
        } else if (arguments != null) {
            view?.webview?.loadUrl(arguments?.getString(PARAM_URL))
        } else {

        }

        dialog.setContentView(view)
        return dialog
    }

    companion object {

        val PARAM_URL = "PARAM_URL"
    }

}
