package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class DeviceTilt {
    var rotationX by mutableFloatStateOf(0f)
        internal set

    var rotationY by mutableFloatStateOf(0f)
        internal set
}

/** Gyroscope disabled for Swara to keep battery cool and UI smooth. */
@Composable
fun rememberDeviceTilt(maxDegrees: Float = 14f): DeviceTilt = remember { DeviceTilt() }
