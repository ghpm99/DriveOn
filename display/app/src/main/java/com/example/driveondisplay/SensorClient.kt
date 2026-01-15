package com.example.driveondisplay

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorClient(
    context: Context,
    private val state: GForceState
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accel =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gravity = FloatArray(3)
    private val alpha = 0.8f // filtro correto

    fun start() {
        accel?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]

        // ganho visual
        val gain = 2.2f

        state.gx = ((event.values[0] - gravity[0]) /
                SensorManager.GRAVITY_EARTH * gain).coerceIn(-1f, 1f)

        state.gy = ((event.values[1] - gravity[1]) /
                SensorManager.GRAVITY_EARTH * gain).coerceIn(-1f, 1f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
