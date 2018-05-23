package com.jibo.apptoolkit.android.example.ui.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import com.jibo.apptoolkit.android.JiboCommandControl
import com.jibo.apptoolkit.android.example.R
import com.jibo.apptoolkit.android.example.ui.fragment.mjpeg.MjpegVideoFragment
import com.jibo.apptoolkit.android.model.api.Robot
import com.jibo.apptoolkit.protocol.CommandRequester
import com.jibo.apptoolkit.protocol.OnConnectionListener
import com.jibo.apptoolkit.protocol.model.Command
import com.jibo.apptoolkit.protocol.model.EventMessage
import kotlinx.android.synthetic.main.fragment_control.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * A placeholder fragment containing a simple view.
 */
class ControlFragment : BaseFragment(), OnConnectionListener, CommandRequester.OnCommandResponseListener {

    private val LOOK_AT_OPTIONS = arrayOf("PositionTarget", "AngleTarget", "EntityTarget", "CameraTarget")

    private var latestCommandID: String? = null

    private var mRobot: Robot? = null
    private var mCommandRequester: CommandRequester? = null
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
        spinnerLookAt.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        editLookAt1.isEnabled = true
                        editLookAt2.isEnabled = true

                        editLookAt0.setText("")
                        editLookAt1.setText("")
                        editLookAt2.setText("")

                        editLookAt0.hint = "X"
                        editLookAt1.hint = "Y"
                        editLookAt2.hint = "Z"
                    }
                    1 -> {
                        editLookAt1.isEnabled = true
                        editLookAt2.isEnabled = false

                        editLookAt0.setText("")
                        editLookAt1.setText("")
                        editLookAt2.setText("")

                        editLookAt0.hint = "θ"
                        editLookAt1.hint = "ψ"
                        editLookAt2.hint = ""
                    }
                    2 -> {
                        editLookAt1.isEnabled = false
                        editLookAt2.isEnabled = false

                        editLookAt0.setText("")
                        editLookAt1.setText("")
                        editLookAt2.setText("")

                        editLookAt0.hint = ""
                        editLookAt1.hint = ""
                        editLookAt2.hint = ""
                    }
                    3 -> {
                        editLookAt1.isEnabled = true
                        editLookAt2.isEnabled = false

                        editLookAt0.setText("")
                        editLookAt1.setText("")
                        editLookAt2.setText("")

                        editLookAt0.hint = "X"
                        editLookAt1.hint = "Y"
                        editLookAt2.hint = ""
                    }
                }
            }

        }

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
        btnFace.setOnClickListener { onFaceEntity() }
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


    private fun EditText.toInt() : Int = if (TextUtils.isEmpty(text)) 0 else text.toString().toInt()


    fun onConnectClick() {
        progressFragment = ProgressFragment()
        progressFragment?.show(fragmentManager, ProgressFragment::class.java.simpleName)

        mRobot?.let { JiboCommandControl.instance.connect(it, this) }
    }

    fun onDisconnectClick() {
        JiboCommandControl.instance.disconnect()
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

    fun onBtnTakePhotoClick() {
        if (mCommandRequester != null) {
            latestCommandID = mCommandRequester?.media?.capture?.photo(Command.TakePhotoRequest.Camera.Left, Command.TakePhotoRequest
                    .CameraResolution
                    .MedRes, false, this)
        }
    }


    // TODO: send emojis too
    fun onBtnSayClick() {
        if (mCommandRequester != null) {
//            mCommandRequester?.say("<pitch " +
//                                "mult=\"0.65\"><anim cat='cat' meta='(rom)' nonBlocking='true' />" +
//                                "</pitch>", this)

            var text = editSay.text.toString()
            if (text.isEmpty()){
                text = "<anim cat='cat' meta='(rom)' nonBlocking='true' />Hello"
            }

            latestCommandID = mCommandRequester?.expression?.say(text, this)
        }
    }

    fun onBtnLootAtClick() {
        if (mCommandRequester != null) {
            val v0 = editLookAt0.toInt()
            val v1 = editLookAt1.toInt()
            val v2 = editLookAt2.toInt()
            when (spinnerLookAt.selectedItemPosition) {
                0 -> latestCommandID = mCommandRequester?.expression?.look(Command.LookAtRequest.PositionTarget(intArrayOf(v0, v1, v2)), this)
                1 -> latestCommandID = mCommandRequester?.expression?.look(Command.LookAtRequest.AngleTarget(floatArrayOf(v0.toFloat(), v1.toFloat())), this)
                2 -> latestCommandID = mCommandRequester?.expression?.look(Command.LookAtRequest.EntityTarget(v0.toLong()), this)
                3 -> latestCommandID = mCommandRequester?.expression?.look(Command.LookAtRequest.CameraTarget(intArrayOf(v0, v1)), this)
            }
        }
    }

    fun onVideoClick() {
        if (mCommandRequester != null) {
            latestCommandID = mCommandRequester?.media?.capture?.video(Command.VideoRequest.VideoType.Debug, 0, this)
        }
    }

    fun onBtnCancelClick() {
        if (mCommandRequester != null && latestCommandID != null) {
            latestCommandID = mCommandRequester?.cancel(latestCommandID, this)
        }
    }

    fun onScreenGesture() {
        if (mCommandRequester != null) {
            latestCommandID = mCommandRequester?.display?.subscribe?.gesture(Command.ScreenGestureRequest.ScreenGestureFilter(Command.ScreenGestureRequest.ScreenGestureFilter.ScreenGestureType.Tap,
                                                                                           Command.ScreenGestureRequest.ScreenGestureFilter.Rectangle(100f, 100f,1280f,720f)), this)
        }
    }

    fun onFetchAsset() {
        if (mCommandRequester != null) {
            latestCommandID = mCommandRequester?.assets?.load("https://upload.wikimedia.org/wikipedia/commons/d/d2/2010_Cynthia_Breazeal_4641804653.png", "Cynthia", this);
        }
    }

    fun onDisplay() {
        if (mCommandRequester != null) {
            val text = editDisplay.text.toString()
            if (TextUtils.isEmpty(text)) latestCommandID = mCommandRequester?.display?.eye(Command.DisplayRequest.EyeView( "eye"), this)
            else latestCommandID = mCommandRequester?.display?.text(Command.DisplayRequest.TextView("TextName", text), this)
        }
    }

    fun onListen() {
        if (mCommandRequester != null) {
            latestCommandID = mCommandRequester?.listen?.start(3000L, 3000L, "en", this)
        }
    }

    fun onMotion() {
        if (mCommandRequester != null) {
            latestCommandID = mCommandRequester?.perception?.subscribe?.motion(this)
        }
    }

    fun onSpeech() {
        if (mCommandRequester != null) {
            latestCommandID = mCommandRequester?.speech(true, this)
        }
    }

    fun onSetConfig() {
        if (mCommandRequester != null) {
            latestCommandID =  mCommandRequester?.config?.set(Command.SetConfigRequest.SetConfigOptions(0.5f), this)
        }
    }

    fun onGetConfig() {
        if (mCommandRequester != null) {
            latestCommandID = mCommandRequester?.config?.get(this)
        }
    }

    fun onListenForHeadTouch() {
        if (mCommandRequester != null) {
            latestCommandID = mCommandRequester?.perception?.subscribe?.headTouch(this)
        }
    }

    fun onFaceEntity() {
        if (mCommandRequester != null) {
            latestCommandID = mCommandRequester?.perception?.subscribe?.face(this)
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

    override fun onSessionStarted(commandRequester: CommandRequester) {
        log("SESSION STARTED")
        mCommandRequester = commandRequester
        hideProgress()
    }

    override fun onConnectionFailed(throwable: Throwable) {
        log("CONNECTION FAILED: " + throwable.localizedMessage)
        mCommandRequester = null
        hideProgress()
    }

    override fun onDisconnected(code: Int) {
        log("DISCONNECTED:" + code)
        mCommandRequester = null
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
