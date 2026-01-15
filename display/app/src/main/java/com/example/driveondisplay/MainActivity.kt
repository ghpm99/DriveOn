package com.example.driveondisplay

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager

class MainActivity : Activity() {

    private lateinit var sensors: SensorClient
    private lateinit var view: GForceView
    private lateinit var sender: NetworkSender

    private val state = GForceState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        NetworkClient.connectAsync()

//        sensors = SensorClient(this, state)
//        view = GForceView(this, state)
//        sender = NetworkSender(state)

        setContentView(FpsTestView(this))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
//        sensors.start()
//        view.start()
//        sender.start()
    }

    override fun onPause() {
        super.onPause()
//        sensors.stop()
//        view.stop()
//        sender.stop()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
    }
}
