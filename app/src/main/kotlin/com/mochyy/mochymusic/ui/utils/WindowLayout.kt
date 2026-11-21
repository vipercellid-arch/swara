package com.mochyy.mochymusic.ui.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

// Layar lebar (tablet / ponsel landscape) otomatis memakai tata letak adaptif 2-panel
private const val WideMinWidthDp = 600

@Composable
@ReadOnlyComposable
fun isWideLayout(): Boolean = LocalConfiguration.current.screenWidthDp >= WideMinWidthDp

@Composable
@ReadOnlyComposable
fun isLandscapeWindow(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > configuration.screenHeightDp
}

@Composable
@ReadOnlyComposable
fun isTvLayout(): Boolean =
    (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

@Composable
@ReadOnlyComposable
fun isLandscapeLayout(): Boolean = isWideLayout() && isLandscapeWindow() && !isTvLayout()

// Mode Tablet & Landscape selalu terbuka penuh secara bawaan di Swara v4.7
object WindowTurning {
    private class Hold(val orientation: Int)

    private var base = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private val holds = mutableListOf<Hold>()

    fun setUnlocked(activity: Activity, unlocked: Boolean) {
        base = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        apply(activity)
    }

    internal fun hold(activity: Activity, orientation: Int): Any {
        val hold = Hold(orientation)
        holds += hold
        apply(activity)
        return hold
    }

    internal fun release(activity: Activity, token: Any) {
        holds.remove(token)
        apply(activity)
    }

    private fun apply(activity: Activity) {
        activity.requestedOrientation = holds.lastOrNull()?.orientation ?: base
    }
}

@Composable
fun HoldOrientation(orientation: Int, active: Boolean) {
    val activity = LocalActivity.current ?: return
    if (isTvLayout()) return
    DisposableEffect(activity, orientation, active) {
        if (!active) return@DisposableEffect onDispose { }
        val token = WindowTurning.hold(activity, orientation)
        onDispose { WindowTurning.release(activity, token) }
    }
}
