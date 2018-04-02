package com.jibo.apptoolkit.android.example.ui.fragment.mjpeg

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.jibo.apptoolkit.android.example.ui.fragment.BaseFragment
import java.io.InputStream
import java.util.*

/**
 * Created by alexz on 11/28/17.
 */
class MjpegVideoFragment : BaseFragment() {

    protected var doLog = true
    protected var mSurfaceEventsListener: SurfaceEventsListener = object : SurfaceEventsListener {
        override fun surfaceCreated() {
            mIsSurfaceCreated = true

            if (doLog) Log.d(TAG, "Surface created")

            //surface created, lets connect to server
            runOnUiThread{ connect() }
        }

        override fun surfaceChanged(width: Int, height: Int) {
            if (doLog) Log.d(TAG, "Surface surfaceChanged $width $height")
        }

        override fun surfaceDestroyed() {
            mIsSurfaceCreated = false
            if (doLog) Log.d(TAG, "Surface destroyed")
        }
    }

    private var mBitrateCheckTimer: Timer? = null

    private var mMjpegGLSurfaceView: MjpegGLSurfaceView? = null
    private var mMjpegViewThread: MjpegViewThread? = null
    private var mMjpegInputStream: MjpegInputStream? = null

    @Volatile
    protected var mIsSurfaceCreated = false
    private var mIsMjpegThreadRunning = false
    private var mPlayBackStarted = false
    private var mLastBitrateCheckTime: Long = 0
    private var mBytesReceived: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = RelativeLayout(activity)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            mMjpegGLSurfaceView = activity?.let { MjpegGLSurfaceView(it) }
            mMjpegGLSurfaceView?.setSurfaceEventsListener(mSurfaceEventsListener)

            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                                  FrameLayout.LayoutParams.MATCH_PARENT)
            mMjpegGLSurfaceView?.layoutParams = params
            (view as ViewGroup).addView(mMjpegGLSurfaceView)

        } catch (e: Exception) {
            Log.d(TAG, "Error", e)
        }

    }

    override fun onResume() {
        super.onResume()

        mIsSurfaceCreated = false

        if (mMjpegGLSurfaceView != null) {
            mMjpegGLSurfaceView?.onResume()
        }
    }

    override fun onPause() {
        super.onPause()

        mIsSurfaceCreated = false
        if (mMjpegGLSurfaceView != null) {
            mMjpegGLSurfaceView?.onPause()
        }

        stop()
    }

    protected fun connect() {

        mMjpegInputStream = sDataStream?.let { MjpegInputStream(it) }
        startPlayback()

        // start idle check timer
        if (mBitrateCheckTimer != null)
            mBitrateCheckTimer?.cancel()
        mBitrateCheckTimer = Timer()
        mBitrateCheckTimer?.schedule(IdleCheckTimerTask(), IDLE_CHECK_TIMER_PERIOD, IDLE_CHECK_TIMER_PERIOD)
    }

    //private

    /**
     * Starting new thread that will read mjpeg frames from server
     */
    private fun startPlayback() {
        if (mMjpegInputStream == null) {
            if (doLog) Log.e(TAG, "startPlayback null inputStream")

            view?.postDelayed({
                                   runOnUiThread{
                                       try {
                                           activity?.onBackPressed()
                                       } catch (e: Exception) {

                                       }
                                   }
                               }, 100)
            return
        }

        // remove existing mMjpegViewThread if it exists
        if (mIsMjpegThreadRunning || mMjpegViewThread != null) {
            mIsMjpegThreadRunning = false
            mMjpegViewThread?.interrupt()
            mMjpegViewThread = null
        }

        mLastBitrateCheckTime = System.currentTimeMillis()
        mBytesReceived = 0

        // create a new mMjpegViewThread to process stream
        if (doLog) Log.d(TAG, "startPlayback")
        mMjpegViewThread = MjpegViewThread()
        mMjpegViewThread?.start()
    }

    /**
     * Stopping thread that reads frames and closing input stream
     */
    private fun stopPlayback() {
        if (doLog) Log.d(TAG, "stopPlayback")
        if (!mIsMjpegThreadRunning) {
            if (doLog) Log.d(TAG, "stopPlayback early return - already stopping")
            return
        }
        mIsMjpegThreadRunning = false

        object : Thread() {
            override fun run() {
                if (doLog) Log.d(TAG, "stopPlayback() start Thread.run()")
                try {
                    if (mMjpegInputStream != null) {            // Android 2.2 (version 8) has a bug that calling mMjpegInputStream.close() will crash the app
                        mMjpegInputStream?.close()
                        mMjpegInputStream = null
                    }
                } catch (e: Exception) {
                    if (doLog) Log.e(TAG, "catch Exception hit in stopPlayback close inputstream")
                }

                if (doLog)
                    Log.d(TAG, "stopPlayback() finished mMjpegInputStream.close()")
                try {
                    if (mMjpegViewThread != null) mMjpegViewThread?.join(200)
                } catch (e: Exception) {
                    if (doLog) Log.e(TAG, "catch Exception hit in stopPlayback", e)
                }

                mMjpegViewThread = null
                if (doLog) Log.d(TAG, "stopPlayback() finished mMjpegViewThread.join()")
            }
        }.start()

    }

    private fun frameDecoded() {
        if (!mPlayBackStarted && !isActivityValid) {
            mPlayBackStarted = true
        }
    }

    /**
     * @param minTime
     * @return
     */
    private fun getDataRateBps(minTime: Long): Int {
        val now = System.currentTimeMillis()
        val timeDiff = now - mLastBitrateCheckTime
        if (timeDiff < minTime) {
            return -1
        }

        val rate = (mBytesReceived * 8000 / timeDiff).toInt()
        mBytesReceived = 0
        mLastBitrateCheckTime = now
        return rate
    }

    //

    protected fun stop() {
        try {

            mPlayBackStarted = false

            if (mBitrateCheckTimer != null) {
                mBitrateCheckTimer?.cancel()
                mBitrateCheckTimer = null
            }

        } catch (e: Exception) {
            if (doLog) Log.e(TAG, "Exception on closing video:", e)
        }

        stopPlayback()
    }

    ////
    inner class MjpegViewThread : Thread() {

        override fun run() {
            mIsMjpegThreadRunning = true
            var bitmap: Bitmap?
            while (mIsMjpegThreadRunning) {
                if (mIsSurfaceCreated) {
                    try {
                        if (mMjpegInputStream == null)
                            break
                        if (!mIsMjpegThreadRunning)
                            return
                        bitmap = mMjpegInputStream?.readFrame()
                        if (bitmap == null) {
                            Thread.sleep(20)
                            continue
                        }

                        mMjpegGLSurfaceView?.setBmpRecalculate(bitmap)
                        // track rxBytes
                        mBytesReceived += mMjpegInputStream?.getCurRxBytes()!!
                        frameDecoded()

                    } catch (e: Exception) {
                        if (doLog) Log.e(TAG, "caught exception: ", e)
                        if (mIsMjpegThreadRunning) {
                            runOnUiThread {
                                try {
                                    activity?.onBackPressed()
                                } catch (e: Exception) {

                                }
                            }
                        }
                    }

                } else {
                    try {
                        Thread.sleep(500)
                        if (doLog) Log.d(TAG, "MjpegView.run Waiting for surface done")
                    } catch (ex: Exception) {
                    }

                }
            }
        }
    }

    private inner class IdleCheckTimerTask : TimerTask() {

        override fun run() {
            try {
                if (!isActivityValid) return

                if (mPlayBackStarted) {
                    val rate = getDataRateBps(IDLE_CHECK_TIMER_PERIOD / 4)
                    if (doLog) Log.d(TAG, "getDataRateBps=" + rate)
                    if (rate == 0) {
                        if (doLog)
                            Log.i(TAG, "Idle detection found stale connection, finishing...")
                        runOnUiThread{
                            try {
                                activity?.onBackPressed()
                            } catch (e: Exception) {

                            }
                        }
                        return
                    }
                }
            } catch (ex: Exception) {
                if (doLog) Log.e(TAG, "IdleCheckTimerTask.run caught " + ex)
            }

        }
    }

    interface SurfaceEventsListener {
        fun surfaceCreated()

        fun surfaceChanged(width: Int, height: Int)

        fun surfaceDestroyed()
    }

    companion object {

        private val TAG = MjpegVideoFragment::class.java.name

        private val IDLE_CHECK_TIMER_PERIOD = (5 * 1000).toLong()

        var sDataStream: InputStream? = null
    }
}
