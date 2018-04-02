package com.jibo.apptoolkit.android.example

import android.app.Application
import com.jibo.apptoolkit.android.JiboRemoteControl
import com.jibo.apptoolkit.protocol.utils.Commons

/**
 * Created by calvinator on 3/29/18.
 */
class CommandSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            Commons.setRootEndpoint(Commons.STG_ENDPOINT)
        } catch (e: Commons.InvalidParameterValueException) {
            e.printStackTrace()
        }

        JiboRemoteControl.init(this, getString(R.string.appId), getString(R.string.appSecret))

    }
}