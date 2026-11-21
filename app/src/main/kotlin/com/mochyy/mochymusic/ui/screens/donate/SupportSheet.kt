// Halaman Dukung Pengembangan Swara — By Teves

package com.mochyy.mochymusic.ui.screens.donate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.core.updater.GithubRepoUrl
import com.mochyy.mochymusic.ui.component.PopupSheetHandleHeight
import com.mochyy.mochymusic.ui.component.PopupSheetState
import com.mochyy.mochymusic.ui.screens.update.openUrl

private const val BandEndsOnCharacter = 0.53f

fun PopupSheetState.showSupportSheet() {
    val popup = this
    show(fullBleed = true, buttonBar = { SupportButtons(popup) }) {
        SupportSheetContent()
    }
}

@Composable
private fun SupportSheetContent() {
    val scroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val characterWidth = characterWidth(maxWidth, maxHeight)
        val bandHeight = PopupSheetHandleHeight + CharacterTop + characterWidth * CharacterAspect * BandEndsOnCharacter

        NameMarquee(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, bandHeight.roundToPx()))
                    layout(constraints.maxWidth, 0) { placeable.place(0, -PopupSheetHandleHeight.roundToPx()) }
                }
                .graphicsLayer { translationY = -scroll.value.toFloat() },
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 26.dp),
        ) {
            Spacer(Modifier.height(CharacterTop))
            DonateCharacter(width = characterWidth)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Suka pakai Swara?",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DonateSoftText,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Dukung Pengembangan Swara!",
                fontSize = 30.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center,
                color = Color.White,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Swara akan selalu gratis dan bebas iklan untukmu.\n\n" +
                    "Dukunganmu membantu biaya server Swara Cloud agar sinkronisasi akun, musik, dan pembaruan otomatis tetap berjalan lancar setiap hari.",
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = DonateSoftText,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SupportButtons(popup: PopupSheetState) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DonatePillButton(
            text = "Donasi QRIS",
            onClick = { popup.showDonateSheet() },
        )
        DonatePillButton(
            text = "Nanti Saja",
            onClick = popup::dismiss,
            quiet = true,
        )
    }
}
