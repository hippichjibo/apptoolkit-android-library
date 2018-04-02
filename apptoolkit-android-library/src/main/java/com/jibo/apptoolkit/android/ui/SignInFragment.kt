package com.jibo.apptoolkit.android.ui

import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialog
import android.text.TextUtils
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jibo.apptoolkit.android.JiboRemoteControl
import com.jibo.apptoolkit.android.R
import com.jibo.apptoolkit.android.model.api.Robot
import com.jibo.apptoolkit.android.util.LogUtils
import kotlinx.android.synthetic.main.fragment_signin.view.*
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by alexz on 31.10.17.
 */
internal class SignInFragment : DialogFragment(), JiboRemoteControl.OnAuthenticationListener {

    var code: String? = null
        private set
    var state: String? = null
        private set

    private val isFragmentValid: Boolean
        get() = activity != null && isResumed && !isDetached && !activity?.isFinishing!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
        val view = activity?.layoutInflater?.inflate(R.layout.fragment_signin, null as ViewGroup?)

        val webSettings = view?.webview?.settings

        if (webSettings != null) {
            webSettings.javaScriptEnabled = true
        }

        view?.webview?.setWebViewClient(SignInViewClient(this))

        if (savedInstanceState != null) {
            view?.webview?.loadUrl(savedInstanceState.getString(PARAM_URL))
        } else if (arguments != null) {
            view?.webview?.loadUrl(arguments?.getString(PARAM_URL))
        } else {

        }

        dialog.setContentView(view)
        return dialog
    }

    override fun onSuccess(robots: ArrayList<Robot>) {}

    override fun onError(throwable: Throwable) {

    }

    override fun onCancel(dialog: DialogInterface?) {
        JiboRemoteControl.instance.cancel()
        super.onCancel(dialog)
    }

    override fun onCancel() {

    }

    override fun dismiss() {
        if (!isFragmentValid) return

        super.dismiss()
    }

    //private methods

    private inner class SignInViewClient(fragment: SignInFragment) : WebViewClient() {
        private val mFragmentWeakReference: WeakReference<SignInFragment>

        init {
            mFragmentWeakReference = WeakReference(fragment)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            val uri = Uri.parse(url)
            val fragment = mFragmentWeakReference.get()
            //just making sure we're still alive
            if (fragment == null || !fragment.isFragmentValid) return true

            if (uri.scheme == "jibo-rom" && uri.host == "callback") {
                //no error, user pressed YES
                if (TextUtils.isEmpty(uri.getQueryParameter("error"))) {
                    try {
                        code = uri.getQueryParameter("code")
                        state = uri.getQueryParameter("state")
                        JiboRemoteControl.instance.signIn(fragment.activity as AppCompatActivity, fragment)
                    } catch (e: Exception) {
                        e.message?.let { LogUtils.L.LOGD(TAG, it, e) }
                        JiboRemoteControl.instance.cancel()
                        fragment.dismiss()
                    }

                } else {
                    //error, user pressed NO
                    JiboRemoteControl.instance.cancel()
                    fragment.dismiss()
                }
                return true
            } else {
                return false
            }

        }
    }

    companion object {
        val TAG = SignInFragment::class.java.simpleName

        val PARAM_URL = "PARAM_URL"
        val PARAM_ROBOTS = "PARAM_ROBOTS"
    }

}
