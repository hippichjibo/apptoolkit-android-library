package com.jibo.apptoolkit.android.example.ui.fragment

import android.content.Context
import android.support.v4.app.Fragment
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

open class BaseFragment : Fragment() {

    protected val isActivityValid: Boolean
        get() = activity != null && !activity?.isFinishing!! && !isRemoving

    protected fun runOnUiThread(runnable: () -> Unit) {
        if (isActivityValid)
            activity?.runOnUiThread(runnable)
    }

    protected fun hideSoftwareKeyboard() {
        try {
            if (activity != null){
                if (activity?.window?.currentFocus != null) {
                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(
                        activity?.window?.currentFocus?.windowToken, 0)
                }
            }
        } catch (e: Exception) {
        }

    }

    protected fun showSoftwareKeyboard(control: EditText) {
        try {
            if (activity != null) {
                val imm = activity?.getSystemService(
                        Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(control, InputMethodManager.SHOW_IMPLICIT)
            }
        } catch (e: Exception) {
        }

    }

    companion object {
        val TAG = BaseFragment::class.java.simpleName
    }

}
