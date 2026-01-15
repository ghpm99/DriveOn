package com.example.driveondisplay

class NetworkSender(
    private val state: GForceState
) : Runnable {

    @Volatile private var running = false
    private val intervalMs = 33L // ~30Hz

    fun start() {
        if (running) return
        running = true
        Thread(this, "NetworkSender").start()
    }

    fun stop() {
        running = false
    }

    override fun run() {
        while (running) {
            val gx = state.gx
            val gy = state.gy

            NetworkClient.send("A $gx $gy")

            try {
                Thread.sleep(intervalMs)
            } catch (_: InterruptedException) {}
        }
    }
}
