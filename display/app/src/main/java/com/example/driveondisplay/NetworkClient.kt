package com.example.driveondisplay

import java.io.PrintWriter
import java.net.Socket

object NetworkClient {

    private var socket: Socket? = null
    private var out: PrintWriter? = null

    fun connectAsync() {
        Thread {
            try {
                socket = Socket("192.168.18.7", 9000)
                out = PrintWriter(socket!!.getOutputStream(), true)
                out!!.println("hello from tablet")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun sendAsync(msg: String) {
        Thread {
            out?.println(msg)
        }.start()
    }

    fun sendTouch(x: Float, y: Float, action: Int) {
        sendAsync("T $x $y $action")
    }

    fun sendAccelerometer(x: Float, y: Float){
        sendAsync("A $x $y")
    }

    fun close() {
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

