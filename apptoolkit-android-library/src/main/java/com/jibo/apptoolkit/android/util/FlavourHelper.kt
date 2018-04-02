package com.jibo.apptoolkit.android.util

import android.util.Base64
import com.jibo.apptoolkit.protocol.utils.Util
import com.jibo.apptoolkit.android.model.api.Certificates
import okhttp3.OkHttpClient
import okio.Buffer
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.CertificateException
import javax.net.ssl.*

/*
 * Created by alexz on 13/12/2017.
 */
internal object FlavourHelper {
    private val TAG = LogUtils.L.makeLogTag(FlavourHelper::class.java)

    const val JIBO_MODE = true
    const val SOCKET_PROTOCOL = "wss://"
    const val SOCKET_PORT = "7160"
    const val URLS_PROTOCOL = "https://"

    private var mKeyStore: KeyStore? = null
    private var mSslContext: SSLContext? = null
    private var mKeyManagers: Array<KeyManager>? = null
    private var mTrustManagers: Array<TrustManager>? = null
    private var mSslSocketFactory: SSLSocketFactory? = null

    @Throws(Exception::class)
    private fun getCertFingerPrint(mdAlg: String, cert: Certificate): String {
        val encCertInfo = cert.encoded
        val md = MessageDigest.getInstance(mdAlg)
        val digest = md.digest(encCertInfo)
        return Util.toHexString(digest)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getTrustManagers(mCertificate: Certificates): Array<TrustManager> {
        return arrayOf(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                LogUtils.L.LOGD(TAG, "fingerprint : " + mCertificate.fingerprint.toUpperCase())
                var isFound = true
                for (i in chain.indices) {
                    try {
                        LogUtils.L.LOGD(TAG, "chain" + i + ":" + getCertFingerPrint("SHA1", chain[i]))
                        if (getCertFingerPrint("SHA1", chain[i]) == mCertificate.fingerprint.toUpperCase()) {
                            isFound = true
                            break
                        }
                    } catch (e: Exception) {
                        LogUtils.L.LOGD(TAG, "", e)
                    }

                }
                if (!isFound) throw CertificateException()
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                return arrayOf()
            }
        })
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun initKeyStore(mCertificate: Certificates) {
        mKeyStore = KeyStore.getInstance("PKCS12")
        mKeyStore?.load(Buffer().write(Base64.decode(mCertificate.p12, Base64.DEFAULT)).inputStream(), "".toCharArray())

        val kmf = KeyManagerFactory.getInstance("X509")
        kmf.init(mKeyStore, "".toCharArray())
        mKeyManagers = kmf.keyManagers

        mTrustManagers = getTrustManagers(mCertificate)

    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun initSslConnection(mCertificate: Certificates): SSLContext {
        initKeyStore(mCertificate)

        mSslContext = SSLContext.getInstance("TLS")

        mSslContext?.init(mKeyManagers, mTrustManagers, null)
        mSslSocketFactory = mSslContext?.socketFactory

        return mSslContext!!
    }

    fun getOkHttpClient(ipAddress: String): OkHttpClient {
        return OkHttpClient.Builder()
                .sslSocketFactory(mSslSocketFactory, mTrustManagers?.get(0) as X509TrustManager)
                .hostnameVerifier { s, sslSession -> ipAddress == s }
                .build()
    }

}
