package com.example.driveondisplay

class GForceState {

    // G atual
    var gx = 0f
    var gy = 0f

    // Máximos por direção
    var maxPosX = 0f
    var maxNegX = 0f
    var maxPosY = 0f
    var maxNegY = 0f

    // Rastro (buffer circular fixo)
    val trailX = FloatArray(64)
    val trailY = FloatArray(64)
    var trailIndex = 0
}
