package com.jibo.apptoolkit.android.util

import android.util.Log
import com.jibo.apptoolkit.protocol.utils.LogUtilsInterface


import com.jibo.apptoolkit.android.BuildConfig

/**
 *
 * Helper methods that make logging more consistent throughout the app.
 */
internal class LogUtils : LogUtilsInterface {

    override fun makeLogTag(str: String): String {
        return if (str.length > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1)
        } else LOG_PREFIX + str
    }

    /**
     * WARNING: Don't use this when obfuscating class names with Proguard!
     */
    override fun makeLogTag(cls: Class<*>): String {
        return makeLogTag(cls.simpleName)
    }

    override fun LOGD(tag: String, message: String) {
        if (Log.isLoggable(tag, Log.DEBUG) || BuildConfig.DEBUG) {

            Log.d(tag, message)
        }
    }

    override fun LOGD(tag: String, message: String, cause: Throwable) {
        if (Log.isLoggable(tag, Log.DEBUG) || BuildConfig.DEBUG) {
            Log.d(tag, message, cause)
        }
    }

    override fun LOGV(tag: String, message: String) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }

    override fun LOGV(tag: String, message: String, cause: Throwable) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG) {
            Log.v(tag, message, cause)
        }
    }

    override fun LOGI(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun LOGI(tag: String, message: String, cause: Throwable) {
        Log.i(tag, message, cause)
    }

    override fun LOGW(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun LOGW(tag: String, message: String, cause: Throwable) {
        Log.w(tag, message, cause)
    }

    override fun LOGE(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun LOGE(tag: String, message: String, cause: Throwable) {
        Log.e(tag, message, cause)
    }

    companion object {

        var L = LogUtils()

        private val LOG_PREFIX = "ROMSDK_"
        private val LOG_PREFIX_LENGTH = LOG_PREFIX.length
        private val MAX_LOG_TAG_LENGTH = 23
    }
}