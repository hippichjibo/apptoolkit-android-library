package com.jibo.apptoolkit.android.example

import android.app.Application
import com.jibo.apptoolkit.android.JiboRemoteControl

/**
 * Created by calvinator on 3/29/18.
 */
class CommandSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        JiboRemoteControl.init(this, getString(R.string.appId), getString(R.string.appSecret))

    }
}
