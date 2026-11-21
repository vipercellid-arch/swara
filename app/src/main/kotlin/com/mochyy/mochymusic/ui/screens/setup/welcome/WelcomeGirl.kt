// Swara onboarding mascot: smooth entrance and gentle breathing animation using Swara's monochrome mascot

package com.mochyy.mochymusic.ui.screens.setup.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.R

@Composable
internal fun WelcomeGirl(
    motion: GirlMotion,
    seconds: FloatState,
    modifier: Modifier = Modifier,
    screen: @Composable BoxScope.() -> Unit = { MochyPhoneScreen() },
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.88f, stiffness = 95f))
    }
    val lift = with(LocalDensity.current) { 36.dp.toPx() }

    Box(
        modifier = modifier.graphicsLayer {
            seconds.floatValue
            val shown = appear.value.coerceIn(0f, 1f)
            alpha = shown
            translationY = (1f - shown) * lift
            val breath = motion.pose.breath
            scaleX = 1f + 0.004f * breath
            scaleY = 1f + 0.008f * breath
            transformOrigin = TransformOrigin(0.5f, 1f)
        },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Image(
            painter = painterResource(R.drawable.swara_mascot),
            contentDescription = "Swara",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun BoxScope.MochyPhoneScreen() {
    // Kept for binary/call-site compatibility
}
