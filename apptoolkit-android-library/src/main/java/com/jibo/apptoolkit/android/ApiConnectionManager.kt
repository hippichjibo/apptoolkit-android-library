package com.jibo.apptoolkit.android

import android.net.Uri
import android.util.LruCache
import com.google.gson.Gson
import com.jibo.apptoolkit.android.util.LogUtils
import com.jibo.apptoolkit.protocol.api.*
import com.jibo.apptoolkit.protocol.utils.Commons
import com.jibo.apptoolkit.protocol.utils.Util
import com.jibo.apptoolkit.android.model.api.Certificates
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Call
import retrofit2.Callback
import java.nio.charset.Charset

/**
 * Created by alexz on 01.11.17.
 */
internal class ApiConnectionManager(private val mClientId: String, private val mClientSecret: String, private var mToken: Token?, private val mOnTokenRetrievedListener: OnTokenRetrievedListener?) {

    private lateinit var mErrors: LruCache<String, Errors>
    private val mRomApiService: ApiServiceGenerator.ROMApiService
    private val mGuestROMApiService: ApiServiceGenerator.ROMApiService
    private var mState: String? = null

    private val baseUrl: String
        get() = Uri.Builder().scheme("https").path(Commons.ROOT_ENDPOINT).build().toString()

    val signInUrl: String
        get() {
            mState = Util.md5(System.currentTimeMillis().toString())
            return Uri.Builder().scheme("https").path(Commons.ROOT_ENDPOINT).appendPath("login")
                    .appendQueryParameter("client_id", mClientId)
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("redirect_uri", CALLBACK_URI)
                    .appendQueryParameter("scope", "rom")
                    .appendQueryParameter("state", mState)
                    .build().toString()
        }

    private val mErrorInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code() >= 400) {
            val errors: Errors

            try {
                val source = response.body()?.source()
                source?.request(java.lang.Long.MAX_VALUE)
                val buffer = source?.buffer()
                val errorJsonBody = buffer?.clone()?.readString(Charset.forName("UTF-8"))
                errors = Gson().fromJson(errorJsonBody, Errors::class.java)
                mErrors.put(request.url().toString(), errors)
            } catch (e: Exception) {
                LogUtils.L.LOGE(TAG, "", e)
            }

        }

        response
    }

    private val mAuthInterceptor = Interceptor { chain ->
        val original = chain.request()

        val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-type", "application/json")
                .header("Authorization",
                        mToken?.tokenType + " " + mToken?.accessToken)
                .method(original.method(), original.body())

        val request = requestBuilder.build()
        chain.proceed(request)
    }

    private val mTokenRefresher = Authenticator { route, response ->
        if (responseCount(response) >= 2) {
            // If both the original call and the call with refreshed token failed,
            // it will probably keep failing, so don't try again.
            //and lets clear what we have saved as token
            mOnTokenRetrievedListener?.onTokenRefreshFailure()

            return@Authenticator null
        }

        // We need a new client, since we don't want to make another call using our client with access token
        val call = mToken?.refreshToken?.let { refreshToken(it) }
        try {
            val tokenResponse = call?.execute()
            if (tokenResponse?.code() == 200) {
                mToken = tokenResponse.body()
                //saving token
                mOnTokenRetrievedListener?.onTokenRetrieved(mToken)

                return@Authenticator response.request().newBuilder()
                        .header("Authorization", mToken?.tokenType + " " + mToken?.accessToken)
                        .build()
            } else {
                return@Authenticator null
            }
        } catch (e: Exception) {
            LogUtils.L.LOGE(TAG, "", e)
            return@Authenticator null
        }
    }

    init {
        this.mRomApiService = ApiServiceGenerator.getROMApiService(baseUrl, mAuthInterceptor,
                                                                      mTokenRefresher, mErrorInterceptor)
        this.mGuestROMApiService = ApiServiceGenerator.createGuestROMApiService(baseUrl, mErrorInterceptor)
        mErrors = LruCache(20)
    }

    fun isStateValid(state: String?): Boolean {
        if (state == null) return false
        return mState == state
    }

    fun getToken(code: String, callback: Callback<Token>?) {
        val tokenExchangeRequest = Request.TokenExchangeRequest(mClientId, mClientSecret, CALLBACK_URI, code)
        mGuestROMApiService.getToken(tokenExchangeRequest.grantType, tokenExchangeRequest.clientId,
                                     tokenExchangeRequest.clientSecret, tokenExchangeRequest.redirectUri, tokenExchangeRequest.code)
                .enqueue(object : Callback<Token> {
                    override fun onResponse(call: Call<Token>, response: retrofit2.Response<Token>) {
                        if (response.isSuccessful) {
                            mToken = response.body()
                            mOnTokenRetrievedListener?.onTokenRetrieved(mToken)
                        }

                        callback?.onResponse(call, response)

                    }

                    override fun onFailure(call: Call<Token>, t: Throwable) {
                        callback?.onFailure(call, t)
                    }
                })
    }

    fun refreshToken(refreshToken: String): Call<Token> {
        val tokenExchangeRequest = Request.TokenRefreshRequest(mClientId, mClientSecret, refreshToken)
        return mGuestROMApiService.refreshToken(tokenExchangeRequest.grantType, tokenExchangeRequest.clientId,
                                                tokenExchangeRequest.clientSecret, tokenExchangeRequest.refreshToken)
    }

    fun getUserInfo(callback: Callback<UserInfo>) {
        mRomApiService.userInfo.enqueue(callback)
    }

    fun getRobots(callback: Callback<RobotData>) {
        mRomApiService.robots.enqueue(callback)
    }

    fun createCertificates(friendlyId: String, callback: Callback<CertData>) {
        mRomApiService.createCertificates(Request.CertificatesRequest(friendlyId)).enqueue(callback)
    }

    fun createCertificates(friendlyId: String): Call<CertData> {
        return mRomApiService.createCertificates(Request.CertificatesRequest(friendlyId))
    }

    fun getClientCertificates(friendlyId: String, callback: Callback<Certificates>) {
        mRomApiService.getClientCertificates(friendlyId).enqueue(callback)
    }

    fun getClientCertificates(friendlyId: String): Call<Certificates> {
        return mRomApiService.getClientCertificates(friendlyId)
    }

    private fun getError(requestUrl: String?): Errors? {
        return if (requestUrl != null) mErrors.get(requestUrl) else null
    }

    internal interface OnTokenRetrievedListener {
        fun onTokenRetrieved(token: Token?)

        fun onTokenRefreshFailure()
    }

    companion object {

        private val TAG = LogUtils.L.makeLogTag(ApiConnectionManager::class.java)

        val CALLBACK_URI = "jibo-rom://callback"

        private fun responseCount(response: Response?): Int {
            var resp = response?.priorResponse()
            var result = 1
            while (resp != null) {
                resp = resp.priorResponse()
                result++
            }
            return result
        }
    }
}