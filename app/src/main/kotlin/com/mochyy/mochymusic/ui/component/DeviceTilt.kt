// DeviceTilt.kt (Swara v4.8.9 — Sensor gyroscope dihapus total demi suhu dingin & hemat baterai)

package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Stable
class DeviceTilt internal constructor() {
    val rotationX: Float = 0f
    val rotationY: Float = 0f
}

@Composable
fun rememberDeviceTilt(maxDegrees: Float = 14f): DeviceTilt = remember { DeviceTilt() }
