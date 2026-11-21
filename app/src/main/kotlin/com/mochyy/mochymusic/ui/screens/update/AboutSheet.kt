// Halaman Tentang Swara — By Teves (100% bersih tanpa profil pengembang lama maupun tautan komunitas lama)

package com.mochyy.mochymusic.ui.screens.update

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.PopupSheetState
import com.mochyy.mochymusic.ui.component.RemoteFocus
import com.mochyy.mochymusic.ui.component.pressBounce
import com.mochyy.mochymusic.ui.player.menu.MenuRowSurface
import com.mochyy.mochymusic.ui.screens.donate.showDonateSheet

private const val TevesInstagramUrl = "https://www.instagram.com/t3ves_?evlk=MXJuZHQybmxjMTV4NA=="

private const val TevesTikTokUrl = "https://www.tiktok.com/@miller_tipak?_r=1&_t=ZS-9BaeWYWUIs8"

fun PopupSheetState.showCreatorProfileSheet() {
    show {
        val context = LocalContext.current
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.creator_teves),
                contentDescription = "Teves",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A22)),
            )
            Spacer(Modifier.height(12.dp))
            Text("Teves", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("@t3ves_ · Pembuat Swara", fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
            Spacer(Modifier.height(20.dp))
            AboutPillRow(
                text = "Instagram (@t3ves_)",
                onClick = { context.openUrl(TevesInstagramUrl) },
            )
            Spacer(Modifier.height(10.dp))
            AboutPillRow(
                text = "TikTok (@miller_tipak)",
                onClick = { context.openUrl(TevesTikTokUrl) },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

fun PopupSheetState.showAboutSheet(onOpenDevSpace: (Int) -> Unit = {}) {
    val popup = this
    show(buttonBar = { SheetPrimaryButton(text = stringResource(R.string.done), onClick = popup::dismiss) }) {
        AboutSheetContent(popup = popup)
    }
}

@Composable
private fun ColumnScope.AboutSheetContent(popup: PopupSheetState) {
    val context = LocalContext.current
    val installed = shortVersion(BuildConfig.VERSION_NAME)
    val scrollState = rememberScrollState()
    val ringScrollRangePx = with(LocalDensity.current) { 180.dp.toPx() }

    Box(Modifier.fillMaxWidth()) {
        VersionRing(
            text = "SWARA $installed",
            scrollProgress = { scrollState.value / ringScrollRangePx },
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .layout { measurable, constraints ->
                        val placeable =
                            measurable.measure(
                                constraints.copy(
                                    minWidth = 0,
                                    minHeight = 0,
                                    maxWidth = Constraints.Infinity,
                                    maxHeight = Constraints.Infinity,
                                ),
                            )
                        layout(0, 0) {
                            placeable.place(x = -placeable.width / 2, y = (-166).dp.roundToPx())
                        }
                    },
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
        ) {
            Spacer(Modifier.height(18.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MenuRowSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mochy_mark),
                        contentDescription = "Swara",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Tentang Swara",
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.2).sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Swara v$installed · By Teves",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }

            Spacer(Modifier.height(18.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MenuRowSurface)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Text(
                    text = "Swara adalah pemutar musik modern buatan Teves dengan Swara Cloud pribadi, sinkronisasi multi-perangkat, Swara Blend, Dengar Bersama, dukungan penuh Tablet & Landscape, serta bebas iklan.",
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Pembuat & Pengembang")
            Spacer(Modifier.height(8.dp))
            val devInteraction = remember { MutableInteractionSource() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .pressBounce(devInteraction, pressedScale = 0.98f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MenuRowSurface)
                        .clickable(interactionSource = devInteraction, indication = RemoteFocus) {
                            context.openUrl(TevesInstagramUrl)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.creator_teves),
                    contentDescription = "Teves",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF1A1A22)),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Teves (@t3ves_)",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Pembuat Swara · Ketuk untuk buka Instagram",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Aksi Cepat")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AboutPillRow(
                    text = "Instagram Pembuat (@t3ves_)",
                    onClick = { context.openUrl(TevesInstagramUrl) },
                )
                AboutPillRow(
                    text = "Dukung Swara (Donasi QRIS)",
                    onClick = { popup.showDonateSheet() },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
        color = Color.White.copy(alpha = 0.55f),
    )
}

@Composable
private fun AboutPillRow(
    text: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            Modifier
                .pressBounce(interaction, pressedScale = 0.97f)
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MenuRowSurface)
                .clickable(interactionSource = interaction, indication = RemoteFocus, onClick = onClick)
                .padding(horizontal = 18.dp),
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
