// Swara v4.9.1: Swara Connect Bar & Live Remote Lyrics Sheet

package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.utils.SwaraCloudClient
import kotlinx.coroutines.delay

private data class SyncedLyricLine(val timeMs: Long, val text: String)

private val LrcLineRegex = Regex("""^\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?](.*)$""")

private fun parseSyncedLyrics(raw: String): List<SyncedLyricLine> {
    if (raw.isBlank()) return emptyList()
    val out = mutableListOf<SyncedLyricLine>()
    var hasTimestamps = false
    raw.lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@forEach
        val m = LrcLineRegex.matchEntire(trimmed)
        if (m != null) {
            hasTimestamps = true
            val min = m.groupValues[1].toLongOrNull() ?: 0L
            val sec = m.groupValues[2].toLongOrNull() ?: 0L
            val fracRaw = m.groupValues[3]
            val ms = when (fracRaw.length) {
                0 -> 0L
                1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                else -> fracRaw.take(3).toLongOrNull() ?: 0L
            }
            val txt = m.groupValues[4].trim()
            if (txt.isNotEmpty()) {
                out.add(SyncedLyricLine(min * 60_000L + sec * 1_000L + ms, txt))
            }
        } else if (!trimmed.startsWith("[")) {
            out.add(SyncedLyricLine(-1L, trimmed))
        }
    }
    return if (hasTimestamps) out.sortedBy { it.timeMs } else out
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L)
    val m = totalSec / 60L
    val s = totalSec % 60L
    return "%d:%02d".format(m, s)
}

@Composable
fun SwaraConnectBar(
    playback: SwaraCloudClient.SyncedPlayback,
    onRemoteAction: (String) -> Unit,
    onTakeOverHere: () -> Unit,
    onOpenRemoteSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF14221B))
            .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.38f), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenRemoteSheet)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (playback.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = playback.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (playback.isPlaying) Color(0xFF22C55E) else Color(0xFF9CA3AF)),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (playback.isPlaying) Color(0xFF22C55E) else Color(0xFF9CA3AF)),
                        )
                        Text(
                            text = playback.sourceDeviceName.ifBlank { "Perangkat Lain" },
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "${playback.title} — ${playback.artist}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onOpenRemoteSheet)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Lyrics, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("Lirik", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                "prev" to Icons.Rounded.SkipPrevious,
                "play_pause" to if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                "next" to Icons.Rounded.SkipNext,
            ).forEach { (act, icon) ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.09f))
                        .clickable { onRemoteAction(act) }
                        .padding(vertical = 6.dp),
                ) {
                    Icon(icon, contentDescription = act, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF22C55E))
                    .clickable(onClick = onTakeOverHere)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text("Putar di sini", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp)
            }
        }
    }
}

@Composable
fun SwaraConnectRemoteSheet(
    playback: SwaraCloudClient.SyncedPlayback,
    onRemoteAction: (String) -> Unit,
    onTakeOverHere: () -> Unit,
    onDismiss: () -> Unit,
) {
    val lines = remember(playback.mediaId, playback.lyrics) { parseSyncedLyrics(playback.lyrics) }
    val isTimed = remember(lines) { lines.any { it.timeMs >= 0L } }
    var livePosMs by remember(playback.mediaId, playback.positionMs, playback.updatedAt, playback.isPlaying) {
        mutableLongStateOf(playback.positionMs)
    }

    LaunchedEffect(playback.mediaId, playback.positionMs, playback.updatedAt, playback.isPlaying) {
        livePosMs = playback.positionMs
        while (playback.isPlaying) {
            delay(500L)
            livePosMs = (livePosMs + 500L).let {
                if (playback.durationMs > 0L) it.coerceAtMost(playback.durationMs) else it
            }
        }
    }

    val activeIndex = remember(lines, livePosMs, isTimed) {
        if (!isTimed || lines.isEmpty()) -1
        else lines.indexOfLast { it.timeMs in 0L..livePosMs }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0E1A14), Color(0xFF0A0D0C), Color(0xFF060606)),
                    ),
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Smartphone, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
                        Column {
                            Text(
                                text = if (playback.isPlaying) "Sedang diputar di ${playback.sourceDeviceName.ifBlank { "Perangkat Lain" }}"
                                else "Dijeda di ${playback.sourceDeviceName.ifBlank { "Perangkat Lain" }}",
                                color = Color(0xFF4ADE80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable(onClick = onDismiss),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Now Playing Card
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = playback.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = playback.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = playback.artist,
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Live Lyrics Panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.045f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    if (lines.isEmpty()) {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Rounded.Lyrics,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Lirik belum tersedia untuk lagu ini",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            itemsIndexed(lines) { idx, line ->
                                val isActive = !isTimed || idx == activeIndex
                                Text(
                                    text = line.text,
                                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.38f),
                                    fontSize = if (isTimed && idx == activeIndex) 21.sp else 17.sp,
                                    lineHeight = if (isTimed && idx == activeIndex) 28.sp else 24.sp,
                                    fontWeight = if (isTimed && idx == activeIndex) FontWeight.ExtraBold else FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Progress bar
                val progress = if (playback.durationMs > 0L) (livePosMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = Color(0xFF22C55E),
                    trackColor = Color.White.copy(alpha = 0.14f),
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDurationMs(livePosMs), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                    Text(
                        if (playback.durationMs > 0L) formatDurationMs(playback.durationMs) else "--:--",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Remote Controls (Prev, Play/Pause, Next)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { onRemoteAction("prev") },
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onRemoteAction("play_pause") },
                    ) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Putar / Jeda",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { onRemoteAction("next") },
                    ) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Take Over Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF22C55E))
                        .clickable {
                            onTakeOverHere()
                            onDismiss()
                        }
                        .padding(vertical = 13.dp),
                ) {
                    Text(
                        text = "Putar di Perangkat Ini",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
