package com.mochyy.mochymusic.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.lyrics.LyricsUtils
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val DynamicIslandRoute = "swara_dynamic_island"

enum class IslandStyle(val id: String, val title: String, val subtitle: String) {
    CLASSIC("classic", "Classic (Notch)", "Menempel di tepi atas layar · Cocok untuk Tablet & bezel"),
    FLOATING("floating", "Floating (Dynamic Island)", "Melayang menyelimuti kamera · Cocok untuk HP punch-hole"),
}

enum class IslandPlaybackAction { PREV, TOGGLE_PLAY, NEXT }

data class IslandLiveTrack(
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String? = null,
    val isPlaying: Boolean = false,
    val currentLyric: String = "",
)

data class DynamicIslandConfig(
    val enabled: Boolean = false,
    val style: IslandStyle = IslandStyle.FLOATING,
    val overlayOutside: Boolean = true,
    val widthScale: Float = 1.0f,
    val offsetYDp: Float = 6f,
    val offsetXDp: Float = 0f,
    val showVisualizer: Boolean = true,
    val showLyrics: Boolean = true,
)

object SwaraDynamicIslandState {
    private const val PREFS = "swara_dynamic_island_prefs"
    private val _config = MutableStateFlow(DynamicIslandConfig())
    val config: StateFlow<DynamicIslandConfig> = _config

    private val _liveTrack = MutableStateFlow(IslandLiveTrack())
    val liveTrack: StateFlow<IslandLiveTrack> = _liveTrack

    private val _systemOverlayActive = MutableStateFlow(false)
    val systemOverlayActive: StateFlow<Boolean> = _systemOverlayActive

    @Volatile
    var playbackActionHandler: ((IslandPlaybackAction) -> Unit)? = null

    fun setSystemOverlayActive(active: Boolean) {
        _systemOverlayActive.value = active
    }

    fun updateLiveTrack(track: IslandLiveTrack) {
        _liveTrack.value = track
    }

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val styleId = sp.getString("style", IslandStyle.FLOATING.id)
        _config.value = DynamicIslandConfig(
            enabled = sp.getBoolean("enabled", false),
            style = IslandStyle.entries.firstOrNull { it.id == styleId } ?: IslandStyle.FLOATING,
            overlayOutside = sp.getBoolean("overlay_outside", true),
            widthScale = sp.getFloat("width_scale", 1.0f).coerceIn(0.75f, 1.45f),
            offsetYDp = sp.getFloat("offset_y_dp", 6f).coerceIn(-16f, 48f),
            offsetXDp = sp.getFloat("offset_x_dp", 0f).coerceIn(-60f, 60f),
            showVisualizer = sp.getBoolean("show_visualizer", true),
            showLyrics = sp.getBoolean("show_lyrics", true),
        )
    }

    fun update(context: Context, transform: (DynamicIslandConfig) -> DynamicIslandConfig) {
        val next = transform(_config.value)
        _config.value = next
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", next.enabled)
            .putString("style", next.style.id)
            .putBoolean("overlay_outside", next.overlayOutside)
            .putFloat("width_scale", next.widthScale)
            .putFloat("offset_y_dp", next.offsetYDp)
            .putFloat("offset_x_dp", next.offsetXDp)
            .putBoolean("show_visualizer", next.showVisualizer)
            .putBoolean("show_lyrics", next.showLyrics)
            .apply()
    }

    fun resetCalibration(context: Context) {
        update(context) {
            it.copy(
                widthScale = 1.0f,
                offsetYDp = if (it.style == IslandStyle.CLASSIC) 0f else 6f,
                offsetXDp = 0f,
            )
        }
    }
}

@Composable
fun DynamicIslandSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cfg by SwaraDynamicIslandState.config.collectAsState()
    var hasOverlayPerm by remember {
        mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context))
    }

    // Refresh overlay permission automatically when returning from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
                hasOverlayPerm = granted
                if (granted && cfg.enabled && cfg.overlayOutside) {
                    SwaraDynamicIslandState.update(context) { it.copy(overlayOutside = true) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SubSettingsScaffold(
        title = "Dynamic Island",
        onBack = { navController.navigateUp() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            IslandToggleCard(
                title = "Aktifkan Dynamic Island",
                subtitle = "Tampilkan kapsul pemutar musik interaktif di bagian atas layar",
                checked = cfg.enabled,
                onCheckedChange = { on -> SwaraDynamicIslandState.update(context) { it.copy(enabled = on) } },
            )

            Text(
                text = "Gaya Notch & Island",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                IslandStylePreviewCard(
                    style = IslandStyle.CLASSIC,
                    selected = cfg.style == IslandStyle.CLASSIC,
                    onClick = {
                        SwaraDynamicIslandState.update(context) { it.copy(style = IslandStyle.CLASSIC, offsetYDp = 0f) }
                    },
                    modifier = Modifier.weight(1f),
                )
                IslandStylePreviewCard(
                    style = IslandStyle.FLOATING,
                    selected = cfg.style == IslandStyle.FLOATING,
                    onClick = {
                        SwaraDynamicIslandState.update(context) { it.copy(style = IslandStyle.FLOATING, offsetYDp = 6f) }
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            IslandToggleCard(
                title = "Tampilkan di luar aplikasi (Overlay)",
                subtitle = if (hasOverlayPerm) {
                    "Izin aktif · Tetap tampil di Home Screen & di atas aplikasi lain"
                } else {
                    "Ketuk untuk mengaktifkan izin Overlay agar tampil di luar Swara"
                },
                checked = cfg.overlayOutside && hasOverlayPerm,
                onCheckedChange = { want ->
                    if (want && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        SwaraDynamicIslandState.update(context) { it.copy(overlayOutside = true) }
                        runCatching {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        }
                    } else {
                        SwaraDynamicIslandState.update(context) { it.copy(overlayOutside = want) }
                    }
                },
            )

            IslandToggleCard(
                title = "Tampilkan lirik berjalan",
                subtitle = "Menampilkan baris lirik yang sedang dinyanyikan langsung di Dynamic Island",
                checked = cfg.showLyrics,
                onCheckedChange = { on -> SwaraDynamicIslandState.update(context) { it.copy(showLyrics = on) } },
            )

            IslandToggleCard(
                title = "Gelombang audio animasi",
                subtitle = "Menampilkan indikator spektrum musik bergerak saat lagu diputar",
                checked = cfg.showVisualizer,
                onCheckedChange = { on -> SwaraDynamicIslandState.update(context) { it.copy(showVisualizer = on) } },
            )

            // Position & Size Calibration Card + 1-Tap Reset Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF141418))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kalibrasi Posisi & Ukuran", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Posisi Y = 0 dimulai dari tepi paling atas layar agar pas menyelimuti kamera.",
                            color = Color.White.copy(alpha = 0.58f),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { SwaraDynamicIslandState.resetCalibration(context) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text("Reset", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IslandSliderItem("Lebar Island", cfg.widthScale, 0.75f..1.45f, "${(cfg.widthScale * 100).toInt()}%") { v ->
                    SwaraDynamicIslandState.update(context) { it.copy(widthScale = v) }
                }
                IslandSliderItem("Posisi vertikal (Atas-Bawah)", cfg.offsetYDp, -16f..48f, "${cfg.offsetYDp.toInt()} dp") { v ->
                    SwaraDynamicIslandState.update(context) { it.copy(offsetYDp = v) }
                }
                IslandSliderItem("Posisi horizontal (Kiri-Kanan)", cfg.offsetXDp, -60f..60f, "${cfg.offsetXDp.toInt()} dp") { v ->
                    SwaraDynamicIslandState.update(context) { it.copy(offsetXDp = v) }
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun IslandStylePreviewCard(
    style: IslandStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) Color.White else Color.White.copy(alpha = 0.12f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) Color(0xFF1C1C22) else Color(0xFF121216))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF232532), Color(0xFF14151C)))),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isClassic = style == IslandStyle.CLASSIC
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(top = if (isClassic) 0.dp else 10.dp)
                    .width(96.dp)
                    .height(24.dp)
                    .clip(
                        if (isClassic) RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                        else CircleShape,
                    )
                    .background(Color.Black)
                    .padding(horizontal = 8.dp),
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                Text("Swara", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(style.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (selected) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.check), contentDescription = null, tint = Color.Black, modifier = Modifier.size(11.dp))
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(style.subtitle, color = Color.White.copy(alpha = 0.56f), fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun IslandToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141418))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                uncheckedTrackColor = Color(0xFF2A2A32),
            ),
        )
    }
}

@Composable
private fun IslandSliderItem(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(display, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.2f)),
        )
    }
}

/**
 * Host in-app yang selalu menyinkronkan lirik aktif ke SwaraDynamicIslandState
 * dan menampilkan kapsul in-app ketika izin Overlay sistem belum aktif.
 */
@Composable
fun SwaraDynamicIslandHost(
    visible: Boolean,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cfg by SwaraDynamicIslandState.config.collectAsState()
    val systemOverlayActive by SwaraDynamicIslandState.systemOverlayActive.collectAsState()
    val connection = LocalPlayerConnection.current ?: return
    val track by connection.uiState.trackInfo.collectAsState()
    val transport by connection.uiState.transportState.collectAsState()
    val lyricsEntity by connection.currentLyrics.collectAsState(initial = null)
    val lyricsOffset by connection.lyricsOffsetMs.collectAsState()

    val parsedLyrics = remember(lyricsEntity?.lyrics) {
        lyricsEntity?.lyrics?.let { LyricsUtils.parseLyrics(it) }.orEmpty()
    }
    var currentLyricLine by remember { mutableStateOf("") }

    // Keep live lyrics synced every 450ms whenever Dynamic Island is enabled
    LaunchedEffect(cfg.enabled, cfg.showLyrics, parsedLyrics, transport.isPlaying, track.title, lyricsOffset) {
        if (!cfg.enabled || !cfg.showLyrics || parsedLyrics.isEmpty()) {
            currentLyricLine = ""
            SwaraDynamicIslandState.updateLiveTrack(
                IslandLiveTrack(track.title, track.artist, track.thumbnailUrl, transport.isPlaying, ""),
            )
            return@LaunchedEffect
        }
        while (true) {
            val pos = connection.player.currentPosition + lyricsOffset
            val idx = LyricsUtils.findCurrentLineIndex(parsedLyrics, pos)
            val line = parsedLyrics.getOrNull(idx)?.text.orEmpty().trim()
            if (line != currentLyricLine) currentLyricLine = line
            SwaraDynamicIslandState.updateLiveTrack(
                IslandLiveTrack(track.title, track.artist, track.thumbnailUrl, transport.isPlaying, line),
            )
            delay(450L)
        }
    }

    // Always push track metadata even when lyrics are empty
    LaunchedEffect(track.title, track.artist, track.thumbnailUrl, transport.isPlaying) {
        SwaraDynamicIslandState.updateLiveTrack(
            IslandLiveTrack(track.title, track.artist, track.thumbnailUrl, transport.isPlaying, currentLyricLine),
        )
    }

    // If System-Wide Overlay is already showing, yield so we never show 2 islands at once
    if (!cfg.enabled || !visible || systemOverlayActive || track.title.isBlank()) return

    var expanded by remember { mutableStateOf(false) }
    val isClassic = cfg.style == IslandStyle.CLASSIC
    val shape = if (isClassic) {
        RoundedCornerShape(bottomStart = if (expanded) 26.dp else 18.dp, bottomEnd = if (expanded) 26.dp else 18.dp)
    } else {
        RoundedCornerShape(if (expanded) 28.dp else 999.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = cfg.offsetXDp.dp, y = cfg.offsetYDp.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Generous touch hit-box wrapper so tapping near top edge is effortless on HP & Tablet
        Box(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = !expanded }
                .padding(bottom = if (expanded) 0.dp else 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .shadow(18.dp, shape)
                    .clip(shape)
                    .background(Color(0xFA060609))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
                    .animateContentSize(
                        spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ),
            ) {
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = tween(220))) togetherWith
                            (fadeOut(tween(180)) + scaleOut(targetScale = 0.94f, animationSpec = tween(180)))
                    },
                    label = "swaraIslandFluid",
                ) { isExp ->
                    if (!isExp) {
                        val compactText = if (cfg.showLyrics && currentLyricLine.isNotBlank()) currentLyricLine else track.title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .width((186f * cfg.widthScale).dp)
                                .padding(horizontal = 10.dp, vertical = if (isClassic) 7.dp else 6.dp),
                        ) {
                            AsyncImage(
                                model = track.thumbnailUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF22222A)),
                            )
                            Text(
                                text = compactText,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (cfg.showVisualizer) {
                                IslandMiniWave(playing = transport.isPlaying)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .width((312f * cfg.widthScale.coerceIn(0.85f, 1.20f)).dp)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    expanded = false
                                    onOpenPlayer()
                                },
                            ) {
                                AsyncImage(
                                    model = track.thumbnailUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF22222A)),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (cfg.showVisualizer) {
                                    IslandMiniWave(playing = transport.isPlaying)
                                }
                            }
                            if (cfg.showLyrics && currentLyricLine.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "♪ $currentLyricLine",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = "Sebelumnya",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp).clickable { connection.player.seekToPrevious() },
                                )
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .clickable {
                                            if (transport.isPlaying) connection.player.pause() else connection.player.play()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(if (transport.isPlaying) R.drawable.pause else R.drawable.play),
                                        contentDescription = "Putar/Jeda",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = "Berikutnya",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp).clickable { connection.player.seekToNext() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandMiniWave(playing: Boolean) {
    val inf = rememberInfiniteTransition(label = "islandWave")
    val b1 by inf.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1",
    )
    val b2 by inf.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2",
    )
    val b3 by inf.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b3",
    )
    val heights = if (playing) listOf(b1, b2, b3) else listOf(0.35f, 0.35f, 0.35f)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(14.dp)) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((14f * h).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4ADE80)),
            )
        }
    }
}
