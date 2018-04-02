package com.jibo.apptoolkit.android.example.ui.activity

import android.os.Bundle
import android.os.StrictMode
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.jibo.apptoolkit.android.example.R
import com.jibo.apptoolkit.android.example.ui.fragment.WelcomeFragment


class MainActivity : AppCompatActivity() {
    internal var args = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment, WelcomeFragment(), WelcomeFragment::class.java.name)
                    .commit()
        }
    }
}
