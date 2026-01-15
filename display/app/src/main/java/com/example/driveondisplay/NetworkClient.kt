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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun send(msg: String) {
        out?.println(msg)
    }
}
