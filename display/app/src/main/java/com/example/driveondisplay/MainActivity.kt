package com.example.driveondisplay

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager

class MainActivity : Activity() {

    private lateinit var sensors: SensorClient
    private lateinit var view: GForceView
    private val state = GForceState()
    private val server = NetworkClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        server.connectAsync()
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        Log.d("DriveOn", "Resolution: ${width}x${height}")


        sensors = SensorClient(this, state)
        view = GForceView(this, state)

        setContentView(view)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
        sensors.start()
    }

    override fun onPause() {
        super.onPause()
        sensors.stop()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
    }
}
