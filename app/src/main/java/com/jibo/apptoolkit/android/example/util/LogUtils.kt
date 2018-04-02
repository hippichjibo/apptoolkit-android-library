package com.jibo.apptoolkit.android.example.util

import android.util.Log
import com.jibo.apptoolkit.android.example.BuildConfig

/**
 *
 * Helper methods that make logging more consistent throughout the app.
 */
object LogUtils {
    private val LOG_PREFIX = "ROMSDK_EXAMPLE_"
    private val LOG_PREFIX_LENGTH = LOG_PREFIX.length
    private val MAX_LOG_TAG_LENGTH = 23

    fun makeLogTag(str: String): String {
        return if (str.length > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1)
        } else LOG_PREFIX + str
    }

    /**
     * WARNING: Don't use this when obfuscating class names with Proguard!
     */
    fun makeLogTag(cls: Class<*>): String {
        return makeLogTag(cls.simpleName)
    }

    fun LOGD(tag: String, message: String) {
        if (Log.isLoggable(tag, Log.DEBUG) || BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun LOGD(tag: String, message: String, cause: Throwable) {
        if (Log.isLoggable(tag, Log.DEBUG) || BuildConfig.DEBUG) {
            Log.d(tag, message, cause)
        }
    }

    fun LOGV(tag: String, message: String) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG || Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, message)
        }
    }

    fun LOGV(tag: String, message: String, cause: Throwable) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.DEBUG || Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, message, cause)
        }
    }

    fun LOGI(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun LOGI(tag: String, message: String, cause: Throwable) {
        Log.i(tag, message, cause)
    }

    fun LOGW(tag: String, message: String) {
        Log.w(tag, message)
    }

    fun LOGW(tag: String, message: String, cause: Throwable) {
        Log.w(tag, message, cause)
    }

    fun LOGE(tag: String, message: String) {
        Log.e(tag, message)
    }

    fun LOGE(tag: String, message: String, cause: Throwable) {
        Log.e(tag, message, cause)
    }
}