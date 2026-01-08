package com.example.driveondisplay

object NetworkClient {

    fun connect() {
        // TODO: socket TCP ou USB
    }

    fun sendTouch(x: Float, y: Float, action: Int) {
        // TODO: serializar e enviar
    }

    fun receiveFrame(): ByteArray? {
        // TODO: receber frame (JPEG/PNG/RGB)
        return null
    }
}
