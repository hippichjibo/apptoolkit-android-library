package com.jibo.apptoolkit.android.example.ui.fragment

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatDialog
import android.util.Base64
import com.jibo.apptoolkit.android.example.R
import kotlinx.android.synthetic.main.fragment_image.view.*

/**
 * Created by alexz on 24.11.17.
 */

class ImageFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
        val view = activity?.layoutInflater?.inflate(R.layout.fragment_image, null)

        val decodedString: ByteArray?
        if (savedInstanceState != null) {
            decodedString = Base64.decode(savedInstanceState.getString(PARAM_DATA, ""), Base64.DEFAULT)
        } else if (arguments != null) {
            decodedString = Base64.decode(arguments?.getString(PARAM_DATA, ""), Base64.DEFAULT)
        } else {
            decodedString = null
        }

        if (decodedString != null && decodedString.size > 0) {
            view?.image?.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size))
        }

        dialog.setContentView(view)
        return dialog
    }

    companion object {

        val PARAM_DATA = "PARAM_DATA"
    }

}
