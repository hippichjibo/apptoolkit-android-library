package com.jibo.apptoolkit.android.ui

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jibo.apptoolkit.android.JiboRemoteControl
import com.jibo.apptoolkit.android.R
import com.jibo.apptoolkit.android.model.api.Robot
import com.jibo.apptoolkit.android.util.LogUtils
import kotlinx.android.synthetic.main.fragment_signin.*
import java.util.*

class SignInActivity : AppCompatActivity(), JiboRemoteControl.OnAuthenticationListener {

    companion object {
        val TAG = SignInActivity::class.java.simpleName

        val PARAM_URL = "PARAM_URL"
        val PARAM_ROBOTS = "PARAM_ROBOTS"
    }


    var code: String? = null
        private set
    var state: String? = null
        private set

    private val isActivityValid: Boolean
        get() = !isDestroyed && !isFinishing


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        webview.settings.javaScriptEnabled = true
        webview.webViewClient = SignInViewClient()
        if (intent.hasExtra(PARAM_URL)) {
            webview.loadUrl(intent.getStringExtra(PARAM_URL))
        }
    }

    override fun onBackPressed() {
        JiboRemoteControl.instance.cancel()
        super.onBackPressed()
    }

    override fun onSuccess(robots: ArrayList<Robot>) {
        // Do nothing
    }

    override fun onError(throwable: Throwable) {
        // Do nothing
    }

    override fun onCancel() {
        JiboRemoteControl.instance.cancel()
        finish()
    }


    private inner class SignInViewClient(/*fragment: SignInFragment*/) : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            val uri = Uri.parse(url)

            //just making sure we're still alive
            if (!isActivityValid) return true

            if (uri.scheme == "jibo-rom" && uri.host == "callback") {
                //no error, user pressed YES
                if (TextUtils.isEmpty(uri.getQueryParameter("error"))) {
                    try {
                        code = uri.getQueryParameter("code")
                        state = uri.getQueryParameter("state")
                        JiboRemoteControl.instance.signIn(JiboRemoteControl.instance.parentSignInActivity, this@SignInActivity)
                    } catch (e: Exception) {
                        e.message?.let { LogUtils.L.LOGD(TAG, it, e) }
                        JiboRemoteControl.instance.cancel()
                        finish()
                    }

                } else {
                    //error, user pressed NO
                    JiboRemoteControl.instance.cancel()
                    finish()
                }
                return true
            } else {
                return false
            }

        }
    }
}
