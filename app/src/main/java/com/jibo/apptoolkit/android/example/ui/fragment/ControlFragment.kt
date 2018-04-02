package com.jibo.apptoolkit.android.example.ui.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.jibo.apptoolkit.protocol.CommandLibrary
import com.jibo.apptoolkit.protocol.OnConnectionListener
import com.jibo.apptoolkit.protocol.model.Command
import com.jibo.apptoolkit.protocol.model.EventMessage
import com.jibo.apptoolkit.android.JiboRemoteControl
import com.jibo.apptoolkit.android.example.R
import com.jibo.apptoolkit.android.example.ui.fragment.mjpeg.MjpegVideoFragment
import com.jibo.apptoolkit.android.model.api.Robot
import kotlinx.android.synthetic.main.fragment_control.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * A placeholder fragment containing a simple view.
 */
class ControlFragment : BaseFragment(), OnConnectionListener, CommandLibrary.OnCommandResponseListener {

    private val LOOK_AT_OPTIONS = arrayOf("PositionTarget", "AngleTarget", "EntityTarget", "CameraTarget")

    private var latestCommandID: String? = null
    private var isEyeDisplayed = true

    private var mRobot: Robot? = null
    private var mCommandLibrary: CommandLibrary? = null
    private var progressFragment: ProgressFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mRobot = savedInstanceState.getParcelable(Robot::class.java.name)
        }
        if (arguments != null) {
            mRobot = arguments?.getParcelable(Robot::class.java.name)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerLookAt.adapter = ArrayAdapter(activity, R.layout.item_dropdown, LOOK_AT_OPTIONS)
        spinnerLookAt.setSelection(0, false)

        button1.setOnClickListener { onConnectClick() }
        button2.setOnClickListener { onDisconnectClick() }
        btnTakePhoto.setOnClickListener { onBtnTakePhotoClick() }
        btnSay.setOnClickListener { onBtnSayClick() }
        btnLookAt.setOnClickListener { onBtnLootAtClick() }
        button3.setOnClickListener { onVideoClick() }
        btnCancel.setOnClickListener { onBtnCancelClick() }
        btnScreenGesture.setOnClickListener { onScreenGesture() }
        btnFetchAsset.setOnClickListener { onFetchAsset() }
        btnDisplay.setOnClickListener { onDisplay() }
        btnListen.setOnClickListener { onListen() }
        btnMotion.setOnClickListener { onMotion() }
        btnSpeech.setOnClickListener { onSpeech() }
        btnSetConfig.setOnClickListener { onSetConfig() }
        btnGetConfig.setOnClickListener { onGetConfig() }
        btnHeadTouch.setOnClickListener { onListenForHeadTouch() }
    }

    override fun onResume() {
        super.onResume()

        if (mRobot == null) activity?.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Robot::class.java.name, mRobot)
    }

    override fun onDestroy() {
        super.onDestroy()

        onDisconnectClick()
    }

    //    @OnClick(android.R.id.button1)
    fun onConnectClick() {
        progressFragment = ProgressFragment()
        progressFragment?.show(fragmentManager, ProgressFragment::class.java.simpleName)

        mRobot?.let { JiboRemoteControl.instance.connect(it, this) }
    }

    //    @OnClick(android.R.id.button2)
    fun onDisconnectClick() {
        JiboRemoteControl.instance.disconnect()
        hideProgress()
        textLog?.text = ""
    }

    private fun hideProgress() {
        runOnUiThread {
            if (progressFragment != null) {
                progressFragment?.dismissAllowingStateLoss()
                progressFragment = null
            }
        }
    }

    //    @OnClick(R.id.btnTakePhoto)
    fun onBtnTakePhotoClick() {
        if (mCommandLibrary != null) {
            latestCommandID = mCommandLibrary?.takePhoto(Command.TakePhotoRequest.Camera.Left, Command.TakePhotoRequest
                    .CameraResolution
                    .MedRes, false, this)
        }
    }


    //    @OnClick(R.id.btnSay)
    // TODO: send emojis too
    fun onBtnSayClick() {
        if (mCommandLibrary != null) {
//            mCommandLibrary?.say("<pitch " +
//                                "mult=\"0.65\"><anim cat='cat' meta='(rom)' nonBlocking='true' />" +
//                                "</pitch>", this)

            var text = editSay.text.toString()
            if (text.isEmpty()){
                text = "<anim cat='cat' meta='(rom)' nonBlocking='true' />Hello"
            }

            mCommandLibrary?.say(text, this)
        }
    }

    //    @OnClick(R.id.btnLookAt)
    fun onBtnLootAtClick() {
        if (mCommandLibrary != null) {
            when (spinnerLookAt.selectedItemPosition) {
                0 -> mCommandLibrary?.lookAt(Command.LookAtRequest.PositionTarget(intArrayOf(10, 10, 10)), this)
                1 -> mCommandLibrary?.lookAt(Command.LookAtRequest.AngleTarget(intArrayOf(10, 10)), this)
                2 -> mCommandLibrary?.lookAt(Command.LookAtRequest.EntityTarget(10L), this)
                3 -> mCommandLibrary?.lookAt(Command.LookAtRequest.CameraTarget(intArrayOf(10, 10)), this)
            }
        }
    }

    //    @OnClick(android.R.id.button3)
    fun onVideoClick() {
        if (mCommandLibrary != null) {
            latestCommandID = mCommandLibrary?.video(Command.VideoRequest.VideoType.Debug, 0, this)
        }
    }

    //    @OnClick(R.id.btnCancel)
    fun onBtnCancelClick() {
        if (mCommandLibrary != null && latestCommandID != null) {
            mCommandLibrary?.cancel(latestCommandID, this)
        }
    }

    //    @OnClick(R.id.btnScreenGesture)
    fun onScreenGesture() {
        if (mCommandLibrary != null) {
            mCommandLibrary?.screenGesture(Command.ScreenGestureRequest.ScreenGestureFilter(Command.ScreenGestureRequest.ScreenGestureFilter.ScreenGestureType.Tap,
                                                                                           Command.ScreenGestureRequest.ScreenGestureFilter.Rectangle(100f, 100f,1280f,720f)), this)
        }
    }

    //    @OnClick(R.id.btnFetchAsset)
    fun onFetchAsset() {
        if (mCommandLibrary != null) {
            mCommandLibrary?.fetchAsset("https://upload.wikimedia.org/wikipedia/commons/d/d2/2010_Cynthia_Breazeal_4641804653.png", "Cynthia", this);
        }
    }

    //    @OnClick(R.id.btnDisplay)
    fun onDisplay() {
        if (mCommandLibrary != null) {
            if (isEyeDisplayed) mCommandLibrary?.display(Command.DisplayRequest.TextView("TextName", "Text Text"), this)
            else mCommandLibrary?.display(Command.DisplayRequest.DisplayView(Command.DisplayRequest.DisplayViewType.Eye, "eye"), this)
            isEyeDisplayed = !isEyeDisplayed
        }
    }

    //    @OnClick(R.id.btnListen)
    fun onListen() {
        if (mCommandLibrary != null) {
            mCommandLibrary?.listen(3000L, 3000L, "en", this)
        }
    }

    //    @OnClick(R.id.btnMotion)
    fun onMotion() {
        if (mCommandLibrary != null) {
            mCommandLibrary?.motion(this)
        }
    }

    //    @OnClick(R.id.btnSpeech)
    fun onSpeech() {
        if (mCommandLibrary != null) {
            mCommandLibrary?.speech(true, this)
        }
    }

    //    @OnClick(R.id.btnSetConfig)
    fun onSetConfig() {
        if (mCommandLibrary != null) {
            mCommandLibrary?.setConfig(Command.SetConfigRequest.SetConfigOptions(0.5f), this)
        }
    }

    //    @OnClick(R.id.btnGetConfig)
    fun onGetConfig() {
        if (mCommandLibrary != null) {
            mCommandLibrary?.getConfig(this)
        }
    }

    //    @OnClick(R.id.btnHeadTouch)
    fun onListenForHeadTouch() {
        if (mCommandLibrary != null) {
            mCommandLibrary?.headTouch(this)
        }
    }

    private fun log(msg: String) {
        if (activity == null) return
        activity?.runOnUiThread {
            val sb = StringBuilder(textLog.text).append(msg).append("\n")
            textLog.text = sb.toString()
        }
    }

    override fun onConnected() {
        log("CONNECTED")
    }

    override fun onSessionStarted(CommandLibrary: CommandLibrary) {
        log("SESSION STARTED")
        mCommandLibrary = CommandLibrary
        hideProgress()
    }

    override fun onConnectionFailed(throwable: Throwable) {
        log("CONNECTION FAILED: " + throwable.localizedMessage)
        mCommandLibrary = null
        hideProgress()
    }

    override fun onDisconnected(code: Int) {
        log("DISCONNECTED:" + code)
        mCommandLibrary = null
        hideProgress()
    }

    override fun onSuccess(transactionID: String) {
        log("SUCCESS:" + transactionID)
    }

    override fun onError(transactionID: String, errorMessage: String) {
        log("ERROR:$transactionID $errorMessage")
    }

    override fun onEventError(transactionID: String, errorData: EventMessage.ErrorEvent.ErrorData) {
        log("EVENT ERROR:" + transactionID + " " + errorData.errorString)
    }

    override fun onSocketError() {
        log("SOCKET ERROR")
        hideProgress()
    }

    override fun onEvent(transactionID: String, event: EventMessage.BaseEvent) {
        log("EVENT:" + transactionID + ":" + event.event.toString())
    }

    override fun onPhoto(transactionID: String, event: EventMessage.TakePhotoEvent, inputStream: InputStream) {
        try {
            if (inputStream != null) {

                //very robust image passing mechanism, dont do like this:)
                val byteArrayOutputStream = ByteArrayOutputStream()
                BitmapFactory.decodeStream(inputStream).compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

                val byteArray = byteArrayOutputStream.toByteArray()
                val encodedBitmap = Base64.encodeToString(byteArray, Base64.DEFAULT)

                runOnUiThread {
                    val fragment = ImageFragment()
                    val args = Bundle()
                    args.putString(ImageFragment.PARAM_DATA, encodedBitmap)
                    fragment.arguments = args
                    fragment.show(fragmentManager, ImageFragment::class.java.simpleName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }

            }
        }
    }

    override fun onVideo(transactionID: String, event: EventMessage.VideoReadyEvent, inputStream: InputStream) {
        try {
            if (inputStream != null) {

                runOnUiThread {
                    MjpegVideoFragment.sDataStream = inputStream
                    activity?.supportFragmentManager?.
                            beginTransaction()?.
                            replace(R.id.fragment, MjpegVideoFragment(), MjpegVideoFragment::class.java.name)?.
                            addToBackStack(null)?.
                            commit()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onListen(transactionID: String, speech: String) {
        log("LISTEN:$transactionID:$speech")
    }

    override fun onParseError() {
        log("UNEXPECTED RESPONSE")
    }
}
