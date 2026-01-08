package com.example.driveondisplay

import android.content.Context
import android.hardware.*
import kotlin.math.abs
import kotlin.math.max

class SensorClient(
    context: Context,
    private val state: GForceState
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accel =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gravity = FloatArray(3)

    private val alpha = 0.15f // suavização

    fun start() {
        accel?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Remove gravidade (low-pass)
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]

        val linX = (event.values[0] - gravity[0]) / SensorManager.GRAVITY_EARTH
        val linY = (event.values[1] - gravity[1]) / SensorManager.GRAVITY_EARTH

        state.gx = linX
        state.gy = linY

        // Máximos por direção
        if (linX > 0) state.maxPosX = max(state.maxPosX, linX)
        else state.maxNegX = max(state.maxNegX, abs(linX))

        if (linY > 0) state.maxPosY = max(state.maxPosY, linY)
        else state.maxNegY = max(state.maxNegY, abs(linY))

        // Atualiza rastro
        val i = state.trailIndex
        state.trailX[i] = linX
        state.trailY[i] = linY
        state.trailIndex = (i + 1) % state.trailX.size
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
