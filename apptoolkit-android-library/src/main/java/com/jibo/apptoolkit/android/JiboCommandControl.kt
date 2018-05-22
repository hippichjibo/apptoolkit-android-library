package com.jibo.apptoolkit.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import com.google.gson.GsonBuilder
import com.jibo.apptoolkit.android.model.api.Certificates
import com.jibo.apptoolkit.android.model.api.Robot
import com.jibo.apptoolkit.android.ui.ProgressFragment
import com.jibo.apptoolkit.android.ui.SignInActivity
import com.jibo.apptoolkit.android.util.LogUtils
import com.jibo.apptoolkit.protocol.ConnectionException
import com.jibo.apptoolkit.protocol.ConnectionException.*
import com.jibo.apptoolkit.protocol.JiboRemoteInitializationException
import com.jibo.apptoolkit.protocol.JiboRemoteInitializationException.*
import com.jibo.apptoolkit.protocol.OnConnectionListener
import com.jibo.apptoolkit.protocol.api.RobotData
import com.jibo.apptoolkit.protocol.api.Token
import retrofit2.Call
import retrofit2.Callback
import java.io.IOException
import java.util.*


/*
 * Created by alexz on 20.10.17.
 */

/**
 * Connectivity information
 */
class JiboCommandControl private constructor() {

    /** `true` if the robot has been successfully authenticated  */
    val isAuthenticated: Boolean
        get() {
            val token = sDataStorage?.token
            return if (token != null) {
                System.currentTimeMillis() - token.timestamp < TOKEN_LIFESPAN
            } else false
        }

    private var mCertificatePollerThread: Thread? = null

    var parentSignInActivity: AppCompatActivity? = null
        private set

    /**
     * Authenticate with Jibo cloud.
     * This function will prompt users to sign into their Jibo account
     * with their email and password. Once they have authenticated
     * their account, they will be able to connect their robot to your app.
     */
    fun signIn(activity: AppCompatActivity?, onAuthenticationListener: OnAuthenticationListener?) {
        parentSignInActivity = null

        checkInitStatus()

        if (activity == null) {
            throw RuntimeException(JiboRemoteInitializationException(ERROR_CONTEXT_MUST_BE_PROVIDED))
        }

        //looks like we have token
        if (sDataStorage?.token != null) {
            //doing autologin with getting robots
            openAutoLoginWindow(activity, onAuthenticationListener)
        } else {
            //if we came back from SignInFragment, so we want to authenticate with code&state
            //and get robots afterwards
            if (onAuthenticationListener != null
                && onAuthenticationListener.javaClass.name == SignInActivity::class.java.name
                && onAuthenticationListener is SignInActivity) {
                authenticate(activity, onAuthenticationListener)
            } else {
                sTempOnAuthenticationListener = onAuthenticationListener
                //if we don' have token then lets open webview
                openSignInWindow(activity, onAuthenticationListener)
            }
        }
    }

    /**
     * Remove authentication for the account.
     * Users will have to authenticate again to connect to your app.
     */
    fun logOut() {
        sDataStorage?.clearToken()
    }

    /** Cancel an in-progress authentication.  */
    fun cancel() {
        parentSignInActivity = null

        if (sTempOnAuthenticationListener != null) {
            sTempOnAuthenticationListener?.onCancel()
            sTempOnAuthenticationListener = null
        }
    }

    /** Connect to a robot. Can only be called for robots where `isAuthenticated = true`
     * @param robot See [Robot]
     * @param onConnectionListener See [OnConnectionListener]
     */
    fun connect(robot: Robot, onConnectionListener: OnConnectionListener?) {
        checkInitStatus()

        if (mRomSdkConnectionManager != null) {
            mRomSdkConnectionManager?.disconnect()
            mRomSdkConnectionManager = null
        }

        mCertificatePollerThread = Thread(object : Runnable {
            private val MAX_TRIES = 20

            private var retries = 0

            override fun run() {
                try {
                    val response = sRomApiConnectionManager?.createCertificates(robot.robotName)?.execute()

                    if (response != null) {
                        if (!response.isSuccessful) {
                            //ok, if we have problem and its 401 then we leave this nasty place and notify user of AUTH problems
                            if (response.code() == 401) {
                                onConnectionListener?.onConnectionFailed(ConnectionException(ERROR_AUTHORIZATION_PROBLEMS))
                            } else {
                                onConnectionListener?.onConnectionFailed(ConnectionException(ERROR_CERTIFICATE_CREATION_PROBLEMS))
                            }
                            return
                        }
                    }
                    if (mCertificatePollerThread != null) {
                        while (retries < MAX_TRIES && !(mCertificatePollerThread!!.isInterrupted)) {
                            try {
                                val responseClient = sRomApiConnectionManager?.getClientCertificates(robot.robotName)?.execute()

                                //yahoo, we've got the IP
                                if (responseClient?.isSuccessful!!) {
                                    //ok, we have cert, lets connect to jibo
                                    val args = Bundle()
                                    val certificates = responseClient.body()
                                    args.putParcelable(Certificates::class.java?.getName(), certificates)
                                    args.putParcelable(Robot::class.java?.getName(), robot)
                                    val msg = Message()
                                    msg.data = args
                                    msg.obj = onConnectionListener
                                    mCertificatePollerHandler.sendMessage(msg)

                                    return
                                } else {

                                    if (response?.code() == 401) {
                                        onConnectionListener?.onConnectionFailed(ConnectionException(ERROR_AUTHORIZATION_PROBLEMS))
                                        return
                                    }

                                    retries++
                                }
                                try {
                                    Thread.sleep(2000)
                                } catch (e: InterruptedException) {
                                    return
                                }

                            } catch (e: Exception) {
                                LogUtils.L.LOGE(TAG, "Could not fetch certificates", e)
                                onConnectionListener?.onConnectionFailed(ConnectionException(ERROR_CONNECTION_PROBLEMS))
                            }

                        }
                    }



                    //if we get here, then we were not able to connect to Jibo and get its IP from server
                    onConnectionListener?.onConnectionFailed(ConnectionException(ERROR_COULD_NOT_CONNECT_TO_ROBOT))

                } catch (e: IOException) {
                    onConnectionListener?.onConnectionFailed(ConnectionException(ERROR_CONNECTION_PROBLEMS))
                }

            }
        })
        mCertificatePollerThread?.start()
    }

    /** Disconnect from the currently connected robot.  */
    fun disconnect() {
        if (mCertificatePollerThread != null) {
            mCertificatePollerThread?.interrupt()
            mCertificatePollerThread = null
        }

        if (mRomSdkConnectionManager != null) {
            mRomSdkConnectionManager?.disconnect()
        }
    }

    private fun authenticate(activity: AppCompatActivity?, onAuthenticationListener: OnAuthenticationListener?) {
        checkInitStatus()

        if (activity == null || onAuthenticationListener == null
            || onAuthenticationListener.javaClass.name != SignInActivity::class.java.name
            || onAuthenticationListener !is SignInActivity)
            throw RuntimeException(ConnectionException(ERROR_SPOOFING_DETECTED))

        val signInActivity = onAuthenticationListener as SignInActivity?
        val state = signInActivity?.state
        val code = signInActivity?.code
        signInActivity?.finish()

        //checking states values
        if (!sRomApiConnectionManager?.isStateValid(state)!!) {
            if (sTempOnAuthenticationListener != null) {
                sTempOnAuthenticationListener?.onError(ConnectionException(ERROR_STATES_MISMATCH))
                sTempOnAuthenticationListener = null
            }
            return
        }

        //trying to get our token
        code?.let {
            sRomApiConnectionManager?.getToken(it, object : Callback<Token> {
            override fun onResponse(call: Call<Token>, response: retrofit2.Response<Token>) {

                if (response.isSuccessful) {
                    signIn(activity, sTempOnAuthenticationListener)
                } else {
                    LogUtils.L.LOGD(TAG, call.toString())
                    LogUtils.L.LOGD(TAG, response.toString())
                    if (sTempOnAuthenticationListener != null) {
                        sTempOnAuthenticationListener?.onError(ConnectionException(ERROR_AUTHORIZATION_PROBLEMS))
                        sTempOnAuthenticationListener = null
                    }
                }
            }

            override fun onFailure(call: Call<Token>, t: Throwable) {
                if (sTempOnAuthenticationListener != null) {
                    sTempOnAuthenticationListener?.onError(ConnectionException(ERROR_CONNECTION_PROBLEMS))
                    sTempOnAuthenticationListener = null
                }
            }
        })
        }
    }

    private fun openSignInWindow(activity: AppCompatActivity, onAuthenticationListener: OnAuthenticationListener?) {
        this.parentSignInActivity = activity

        val intent = Intent(activity, SignInActivity::class.java)
        intent.putExtra(SignInActivity.PARAM_URL, sRomApiConnectionManager?.signInUrl)
        activity.startActivity(intent)
    }

    private fun openAutoLoginWindow(activity: AppCompatActivity, onAuthenticationListener: OnAuthenticationListener?) {
        var fragment: ProgressFragment? = ProgressFragment()
        try {
            fragment?.show(activity.supportFragmentManager, ProgressFragment::class.java.simpleName)
        } catch (e: Exception) {
            fragment = null
        }

        sTempOnAuthenticationListener = null

        sRomApiConnectionManager?.getRobots(object : Callback<RobotData> {
            override fun onResponse(call: Call<RobotData>, response: retrofit2.Response<RobotData>) {
                fragment?.dismissAllowingStateLoss()
                if (response.isSuccessful) {
                    response.body()?.robots?.let { Robot.getRobot(it) }?.let { onAuthenticationListener?.onSuccess(it) }
                } else {
                    if (response.code() == 401) {
                        sDataStorage?.clearToken()
                        signIn(activity, onAuthenticationListener)
                        return
                    }
                    onAuthenticationListener?.onError(ConnectionException(ERROR_BAD_REQUEST_OR_SOMETHING))
                }
            }

            override fun onFailure(call: Call<RobotData>, t: Throwable) {
                fragment?.dismissAllowingStateLoss()

                onAuthenticationListener?.onError(ConnectionException(ERROR_CONNECTION_PROBLEMS))
            }
        })
    }

    //private methods

    private fun checkInitStatus() {
        if (sDataStorage == null || sRomApiConnectionManager == null || sInstance == null) {
            throw RuntimeException(JiboRemoteInitializationException(ERROR_JIBO_REMOTE_CONTROL_NOT_INITIALIZED))
        }
    }

    /** Interface for authenticating a robot  */
    interface OnAuthenticationListener {
        /** We authenticated the account and got a list of robots back that we can connect to */
        fun onSuccess(robots: ArrayList<Robot>)

        /** There was an error while authenticating  */
        fun onError(throwable: Throwable)

        /** The authentication was canceled  */
        fun onCancel()
    }

    private class DataStorage(context: Context?) {

        val token: Token?
            get() {
                if (sContext == null) return null

                var token: Token? = null
                try {
                    val pref = sContext?.getSharedPreferences(sContext?.packageName, Context.MODE_PRIVATE)
                    if (pref?.contains(TOKEN_FILE)!!) {
                        val tokenStr = pref.getString(TOKEN_FILE, "")
                        if (!TextUtils.isEmpty(tokenStr)) {
                            token = sGson.fromJson(tokenStr, Token::class.java)
                        }
                    }
                } catch (e: Exception) {
                    LogUtils.L.LOGE(TAG, "getToken() error", e)
                }

                return token
            }

        init {
            sContext = context?.applicationContext
        }

        fun putToken(token: Token) {
            if (sContext == null) return

            try {
                token.timestamp = System.currentTimeMillis()
                val pref = sContext?.getSharedPreferences(sContext?.packageName, Context.MODE_PRIVATE)
                pref?.edit()?.putString(TOKEN_FILE, sGson.toJson(token))?.apply()
            } catch (e: Exception) {
                LogUtils.L.LOGE(TAG, "putToken() error", e)
                throw RuntimeException(Exception(ERROR_FILE_SYSTEM_ERROR))
            }

        }

        fun clearToken() {
            if (sContext == null) return

            try {
                val pref = sContext?.getSharedPreferences(sContext?.packageName, Context.MODE_PRIVATE)
                pref?.edit()?.remove(TOKEN_FILE)?.apply()
            } catch (e: Exception) {
                LogUtils.L.LOGE(TAG, "clearToken() error", e)
                throw RuntimeException(Exception(ERROR_FILE_SYSTEM_ERROR))
            }

        }

        companion object {
            private val TOKEN_FILE = "tkn"
            private val ERROR_FILE_SYSTEM_ERROR = "Error accessing file system. Please try again..."

            private var sInstance: DataStorage? = null
            private var sContext: Context? = null

            @Synchronized
            fun getInstance(context: Context): DataStorage? {
                var tmpInstance = sInstance
                if (tmpInstance == null) {
                    synchronized(DataStorage::class.java) {
                        tmpInstance = sInstance
                        if (tmpInstance == null) {
                            tmpInstance = DataStorage(context)
                            sInstance = tmpInstance
                        }
                    }
                }
                return tmpInstance
            }
        }
    }

    companion object {
        private val TAG = LogUtils.L.makeLogTag(JiboCommandControl::class.java)

        private val TOKEN_LIFESPAN = (3600 * 1000).toLong() //1h

        internal val SIGN_IN_REQUEST_CODE = 1000

        internal var sGson = GsonBuilder().serializeNulls().create()

        private var sDataStorage: DataStorage? = null
        private var sRomApiConnectionManager: ApiConnectionManager? = null
        private var mRomSdkConnectionManager: SdkConnectionManager? = null

        private var sInstance: JiboCommandControl? = null

        private var sTempOnAuthenticationListener: OnAuthenticationListener? = null

        private val sOnTokenRetrievedListener = object : ApiConnectionManager.OnTokenRetrievedListener {
            override fun onTokenRetrieved(token: Token?) {
                token?.let { sDataStorage?.putToken(it) }
            }

            override fun onTokenRefreshFailure() {
                sDataStorage?.clearToken()
            }
        }

        /** Instantiate a JiboCommandControl app with your client ID and passcode.
         * See [Client ID Docs](https://app-toolkit.jibo.com/clientid/) for more info on client ids.
         * @param context this
         * @param clientId Your client ID as provided to you by Jibo. Inc.
         * @param clientSecret Your passcode as provided to you by Jibo, Inc.
         */
        @Synchronized
        fun init(context: Context?, clientId: String, clientSecret: String) {
            if (context == null || TextUtils.isEmpty(clientId) || TextUtils.isEmpty(clientSecret)) {
                throw RuntimeException(JiboRemoteInitializationException(ERROR_INVALID_INIT_INPUT))
            }

            //
            sDataStorage = DataStorage.getInstance(context.applicationContext)

            sRomApiConnectionManager = ApiConnectionManager(clientId, clientSecret, sDataStorage?.token, sOnTokenRetrievedListener)

            _init()
        }

        @Synchronized
        private fun _init() {
            var tmpInstance = sInstance
            if (tmpInstance == null) {
                synchronized(JiboCommandControl::class.java) {
                    tmpInstance = sInstance
                    if (tmpInstance == null) {
                        sInstance = JiboCommandControl()
                    }
                }
            }
        }

        /** Get an instance of JiboCommandControl  */
        val instance: JiboCommandControl
            get() {
                if (sInstance == null)
                    throw RuntimeException(
                            JiboRemoteInitializationException(ERROR_JIBO_REMOTE_CONTROL_NOT_INITIALIZED))
                return sInstance as JiboCommandControl
            }
        private val mCertificatePollerHandler = object : Handler() {

            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                //this is the IP we getting
                if (msg.data != null && msg.obj != null && msg.obj is OnConnectionListener) {
                    val certificates = msg.data.getParcelable<Certificates>(Certificates::class.java?.getName())
                    val robot = msg.data.getParcelable<Robot>(Robot::class.java?.getName())
                    mRomSdkConnectionManager = SdkConnectionManager(certificates,
                                                                       msg.obj as OnConnectionListener)
                    mRomSdkConnectionManager?.connect()
                }
            }
        }
    }

}
