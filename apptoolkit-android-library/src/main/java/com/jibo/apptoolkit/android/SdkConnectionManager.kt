package com.jibo.apptoolkit.android

import com.jibo.apptoolkit.protocol.CommandLibrary
import com.jibo.apptoolkit.protocol.ConnectionException
import com.jibo.apptoolkit.protocol.ConnectionException.*
import com.jibo.apptoolkit.protocol.OnConnectionListener
import com.jibo.apptoolkit.android.model.api.Certificates
import com.jibo.apptoolkit.android.util.FlavourHelper
import com.jibo.apptoolkit.android.util.LogUtils
import okhttp3.*
import okio.ByteString
import javax.net.ssl.SSLContext

/*
 * Created by alexz on 01.11.17.
 */
internal class SdkConnectionManager
(private val mCertificate: Certificates, onConnectionListener: OnConnectionListener?) {

    private var mSslContext: SSLContext? = null
    private var mHttpClient: OkHttpClient? = null
    private var mWebSocket: WebSocket? = null

    private var mDisconnectedByClient = false
    private var mOnConnectionListener: OnConnectionListener? = null
    private var mCommandLibrary: CommandLibrary? = null

    init {

        this.mOnConnectionListener = object : OnConnectionListener {
            override fun onConnected() {
                mCommandLibrary = CommandLibrary(mSslContext, mWebSocket, mCertificate.ipAddress, mOnConnectionListener)
                mCommandLibrary?.startSession()

                onConnectionListener?.onConnected()
            }

            override fun onSessionStarted(romCommander: CommandLibrary) {
                onConnectionListener?.onSessionStarted(mCommandLibrary)
            }

            override fun onConnectionFailed(throwable: Throwable) {
                onConnectionListener?.onConnectionFailed(throwable)

                disconnect()
            }

            override fun onDisconnected(code: Int) {
                //making sure we have all members reset and
                if (!mDisconnectedByClient) {
                    if (mCommandLibrary != null) mCommandLibrary?.disconnect()
                }

                onConnectionListener?.onDisconnected(code)
            }
        }

        try {
            mSslContext = FlavourHelper.initSslConnection(mCertificate)
        } catch (e: Exception) {
            LogUtils.L.LOGE(TAG, "SSLContext creation error", e)
            if (this.mOnConnectionListener != null) {
                (this.mOnConnectionListener as OnConnectionListener).onConnectionFailed(ConnectionException(ERROR_INTERNAL_SYSTEM))
            }
            throw RuntimeException(ConnectionException(ERROR_INTERNAL_SYSTEM))
        }

    }

    /** Connect to the robot whose IP address we already have  */
    fun connect() {
        try {
            _disconnect(EXIT_CODE_BY_CLIENT)
            mHttpClient = FlavourHelper.getOkHttpClient(mCertificate.ipAddress)
            val request = Request.Builder().url(StringBuilder(FlavourHelper.SOCKET_PROTOCOL).append(mCertificate
                                                                                                            .ipAddress)
                                                        .append(":")
                                                        .append(FlavourHelper.SOCKET_PORT).toString()).build()
            mWebSocket = mHttpClient?.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket?, response: Response?) {
                    mDisconnectedByClient = false
                    mOnConnectionListener?.onConnected()
                }

                override fun onMessage(webSocket: WebSocket?, text: String?) {
                    LogUtils.L.LOGD(TAG, "Receiving : " + text)

                    if (mCommandLibrary != null) mCommandLibrary?.parseJiboResponse(text)
                }

                override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
                    super.onMessage(webSocket, bytes)
                }

                override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
                    LogUtils.L.LOGD(TAG, "Closing : $code / $reason")
                    if (code >= EXIT_CODE_HEAD_TOUCH && code <= EXIT_CODE_INACTIVITY) {
                        _disconnect(code)
                    }
                }

                override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
                    LogUtils.L.LOGD(TAG, "Closed : $code / $reason")

                    mOnConnectionListener?.onDisconnected(code)
                }

                override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
                    LogUtils.L.LOGD(TAG, "Error : " + t?.message)

                    _disconnect(EXIT_CODE_PROTOCOL_ERROR)
                    //
                    mOnConnectionListener?.onConnectionFailed(ConnectionException(ERROR_ROBOT_DROPPED_CONNECTION))
                }
            })
        } catch (e: Exception) {
            LogUtils.L.LOGE(TAG, "", e)
            if (this.mOnConnectionListener != null) {
                mOnConnectionListener?.onConnectionFailed(ConnectionException(ERROR_COULD_NOT_CONNECT_TO_ROBOT))
            }
            throw RuntimeException(ConnectionException(ERROR_COULD_NOT_CONNECT_TO_ROBOT))
        }

    }


    fun disconnect() {

        mDisconnectedByClient = true

        _disconnect(EXIT_CODE_BY_CLIENT)
    }

    private fun _disconnect(code: Int) {

        if (mCommandLibrary != null) {
            mCommandLibrary?.disconnect()
            mCommandLibrary = null
        }

        if (mWebSocket != null) {
            //            java.lang.reflect.Method method;
            //            try {
            //                RealWebSocket socket = (RealWebSocket) mWebSocket;
            //                method = socket.getClass().getDeclaredMethod("close", int.class, String.class, long.class);
            //                method.setAccessible(true);
            //                method.invoke(socket, code, "Goodbye !", 100);
            //            } catch (Exception e) {
            mWebSocket?.close(code, "Goodbye !")
            //            }

            mWebSocket = null
        }

        if (mHttpClient != null) {
            mHttpClient?.dispatcher()?.executorService()?.shutdown()
            mHttpClient = null
        }
    }

    companion object {
        private val TAG = LogUtils.L.makeLogTag(SdkConnectionManager::class.java)

        private val EXIT_CODE_BY_CLIENT = 1000
        private val EXIT_CODE_PROTOCOL_ERROR = 1002
        private val EXIT_CODE_HEAD_TOUCH = 4000
        private val EXIT_CODE_ROBOT_ERROR = 4001
        private val EXIT_CODE_NEW_CONNECTION = 4002
        private val EXIT_CODE_INACTIVITY = 4003
    }
}
