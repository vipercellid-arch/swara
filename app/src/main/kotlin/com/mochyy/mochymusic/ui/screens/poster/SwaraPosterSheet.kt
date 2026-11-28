package com.mochyy.mochymusic.ui.screens.poster

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.db.entities.PlaylistSong
import com.mochyy.mochymusic.ui.component.PopupSheetState
import com.mochyy.mochymusic.ui.component.TopToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

data class PosterSongItem(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
)

private enum class PosterTab(val label: String) {
    PHOTO("Foto"),
    CARD("Kartu"),
    STYLE("Gaya Kartu"),
    EMOJI("Emoji"),
    EFFECT("Efek"),
}

private enum class CardVisualStyle(val label: String) {
    GLASS("Kaca Gelap"),
    DARK("Hitam Pekat"),
    LIGHT("Terang"),
}

private data class CardSlot(val xFrac: Float, val yFrac: Float, val rotDeg: Float)

private data class PosterEmojiSticker(
    val id: Long,
    val emoji: String,
    val xFrac: Float = 0.44f,
    val yFrac: Float = 0.42f,
    val scale: Float = 1.0f,
)

private val GlowPalette = listOf(
    0xFFFFFFFF.toInt(),
    0xFF9FC6E2.toInt(),
    0xFFFCA5A5.toInt(),
    0xFFFDE68A.toInt(),
    0xFF86EFAC.toInt(),
    0xFFC4B5FD.toInt(),
    0xFFF9A8D4.toInt(),
)

private val QuickEmojis = listOf("🎧", "🎵", "🔥", "✨", "💿", "🖤", "🤍", "🌙", "⚡", "🦋", "💫", "🎤")

fun PopupSheetState.showSwaraPosterSheetFromSongs(songs: List<com.mochyy.mochymusic.db.entities.Song>) {
    val popup = this
    val items = songs.map { s ->
        PosterSongItem(
            id = s.id,
            title = s.song.title,
            artist = s.artists.joinToString(", ") { it.name }.ifBlank { "Swara" },
            thumbnailUrl = s.song.thumbnailUrl,
        )
    }
    show(fullBleed = true) {
        SwaraPosterFlow(allSongs = items, onClose = popup::dismiss)
    }
}

fun PopupSheetState.showSwaraPosterSheet(songs: List<PlaylistSong>) {
    val popup = this
    val items = songs.map { ps ->
        PosterSongItem(
            id = ps.song.id,
            title = ps.song.song.title,
            artist = ps.song.artists.joinToString(", ") { it.name }.ifBlank { "Swara" },
            thumbnailUrl = ps.song.song.thumbnailUrl,
        )
    }
    show(fullBleed = true) {
        SwaraPosterFlow(allSongs = items, onClose = popup::dismiss)
    }
}

@Composable
private fun SwaraPosterFlow(
    allSongs: List<PosterSongItem>,
    onClose: () -> Unit,
) {
    val selectedIds = remember { mutableStateListOf<String>() }
    var editing by remember { mutableStateOf(false) }

    if (!editing) {
        SwaraPosterPickerStep(
            allSongs = allSongs,
            selectedIds = selectedIds,
            onBack = onClose,
            onNext = { if (selectedIds.isNotEmpty()) editing = true },
        )
    } else {
        val picked = remember(allSongs, selectedIds.toList()) {
            selectedIds.mapNotNull { id -> allSongs.firstOrNull { it.id == id } }
        }
        SwaraPosterEditorStep(
            songs = picked,
            onBack = { editing = false },
            onClose = onClose,
        )
    }
}

@Composable
private fun SwaraPosterPickerStep(
    allSongs: List<PosterSongItem>,
    selectedIds: MutableList<String>,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(620.dp)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Pilih lagu untuk poster", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable {
                        selectedIds.clear()
                        selectedIds.addAll(allSongs.take(5).map { it.id })
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text("Pilih 5 pertama", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("${selectedIds.size} dari 5 dipilih", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(allSongs, key = { it.id }) { song ->
                val isSelected = song.id in selectedIds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.14f) else Color(0xFF141418))
                        .clickable {
                            if (isSelected) selectedIds.remove(song.id)
                            else if (selectedIds.size < 5) selectedIds.add(song.id)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF23232A)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .border(1.5.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(painterResource(R.drawable.check), contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).clickable(onClick = onBack),
            ) {
                Text("Batal", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(if (selectedIds.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.25f))
                    .clickable(enabled = selectedIds.isNotEmpty(), onClick = onNext),
            ) {
                Text("Lanjut", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SwaraPosterEditorStep(
    songs: List<PosterSongItem>,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(PosterTab.PHOTO) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cutoutData by remember { mutableStateOf<SubjectCutoutData?>(null) }
    var cutoutBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var segmenting by remember { mutableStateOf(false) }
    var subjectInFront by remember { mutableStateOf(true) }
    var subjectThreshold by remember { mutableFloatStateOf(0.42f) }

    var photoScale by remember { mutableFloatStateOf(1f) }
    var photoOffsetX by remember { mutableFloatStateOf(0f) }
    var photoOffsetY by remember { mutableFloatStateOf(0f) }
    var photoRotation by remember { mutableFloatStateOf(0f) }
    var photoMirrored by remember { mutableStateOf(false) }

    var bgDarken by remember { mutableFloatStateOf(0.20f) }
    var bgBlur by remember { mutableFloatStateOf(0f) }
    var sparkleEnabled by remember { mutableStateOf(true) }
    var sparkleIntensity by remember { mutableFloatStateOf(0.85f) }
    var cardGlow by remember { mutableFloatStateOf(0.45f) }
    var glowColorInt by remember { mutableIntStateOf(GlowPalette.first()) }
    var cardScale by remember { mutableFloatStateOf(1f) }
    var cardStyle by remember { mutableStateOf(CardVisualStyle.GLASS) }
    var saving by remember { mutableStateOf(false) }

    val cardSlots = remember(songs.size) {
        mutableStateListOf<CardSlot>().apply { addAll(cardSlotsFor(songs.size)) }
    }
    val emojis = remember { mutableStateListOf<PosterEmojiSticker>() }
    var customEmojiText by remember { mutableStateOf("") }
    var emojiSizeScale by remember { mutableFloatStateOf(1.0f) }
    var previewSize by remember { mutableStateOf(IntSize(1, 1)) }

    // Rebuild foreground cutout bitmap cleanly whenever sensitivity slider changes
    LaunchedEffect(photoBitmap, cutoutData, subjectThreshold) {
        val bmp = photoBitmap
        val data = cutoutData
        if (bmp != null && data != null && data.hasSubject) {
            cutoutBitmap = SwaraSubjectCutout.buildCutoutBitmap(bmp, data, subjectThreshold)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val bmp = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                        val decoded = BitmapFactory.decodeStream(stream, null, opts)
                        // Downscale huge camera photos to max 1440px so segmentation is instant & memory-safe
                        if (decoded != null && (decoded.width > 1440 || decoded.height > 1440)) {
                            val ratio = minOf(1440f / decoded.width, 1440f / decoded.height)
                            Bitmap.createScaledBitmap(
                                decoded,
                                (decoded.width * ratio).toInt().coerceAtLeast(1),
                                (decoded.height * ratio).toInt().coerceAtLeast(1),
                                true,
                            )
                        } else {
                            decoded
                        }
                    }
                }.getOrNull()
                if (bmp != null) {
                    withContext(Dispatchers.Main) {
                        photoBitmap = bmp
                        cutoutData = null
                        cutoutBitmap = null
                        segmenting = true
                    }
                    val data = SwaraSubjectCutout.analyzeSubject(bmp)
                    val fg = if (data != null && data.hasSubject) {
                        SwaraSubjectCutout.buildCutoutBitmap(bmp, data, subjectThreshold)
                    } else null
                    withContext(Dispatchers.Main) {
                        cutoutData = data
                        cutoutBitmap = fg
                        segmenting = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onBack)) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = "Kembali", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Poster Lagu", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(enabled = !saving) {
                            saving = true
                            scope.launch {
                                val bmp = renderPosterBitmap(
                                    songs = songs,
                                    slots = cardSlots.toList(),
                                    emojis = emojis.toList(),
                                    photo = photoBitmap,
                                    cutout = if (subjectInFront) cutoutBitmap else null,
                                    photoScale = photoScale,
                                    photoOffsetX = photoOffsetX,
                                    photoOffsetY = photoOffsetY,
                                    photoRotation = photoRotation,
                                    photoMirrored = photoMirrored,
                                    bgDarken = bgDarken,
                                    sparkleEnabled = sparkleEnabled,
                                    sparkleIntensity = sparkleIntensity,
                                    cardGlow = cardGlow,
                                    glowColorInt = glowColorInt,
                                    cardScale = cardScale,
                                    cardStyle = cardStyle,
                                )
                                sharePosterBitmap(context, bmp)
                                saving = false
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("Bagikan", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(enabled = !saving) {
                            saving = true
                            scope.launch {
                                val bmp = renderPosterBitmap(
                                    songs = songs,
                                    slots = cardSlots.toList(),
                                    emojis = emojis.toList(),
                                    photo = photoBitmap,
                                    cutout = if (subjectInFront) cutoutBitmap else null,
                                    photoScale = photoScale,
                                    photoOffsetX = photoOffsetX,
                                    photoOffsetY = photoOffsetY,
                                    photoRotation = photoRotation,
                                    photoMirrored = photoMirrored,
                                    bgDarken = bgDarken,
                                    sparkleEnabled = sparkleEnabled,
                                    sparkleIntensity = sparkleIntensity,
                                    cardGlow = cardGlow,
                                    glowColorInt = glowColorInt,
                                    cardScale = cardScale,
                                    cardStyle = cardStyle,
                                )
                                val ok = savePosterToGallery(context, bmp)
                                saving = false
                                TopToaster.show(
                                    text = if (ok) "Poster tersimpan ke Galeri" else "Gagal menyimpan poster",
                                    iconRes = R.drawable.ic_mochy_mark,
                                )
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(if (saving) "Menyimpan…" else "Simpan", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        val statusText = when {
            segmenting -> "Memisahkan subjek orang dari latar…"
            photoBitmap != null && cutoutBitmap != null && subjectInFront -> "Subjek terdeteksi · tampil di depan kartu · seret kartu sesuka hati"
            photoBitmap != null && cutoutData?.hasSubject == false -> "Subjek orang tidak terdeteksi · seret kartu lagu di pratinjau"
            else -> "Pilih foto & seret kartu lagu atau emoji langsung di pratinjau"
        }
        Text(
            text = statusText,
            color = if (cutoutBitmap != null && subjectInFront) Color(0xFF86EFAC) else Color.White.copy(alpha = 0.62f),
            fontSize = 11.5.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))

        // Live Interactive 9:16 Poster Preview
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(9f / 16f)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A0A0E))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                .onSizeChanged { previewSize = it },
        ) {
            // Layer 1: Background Photo
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap!!.asImageBitmap(),
                    contentDescription = "Latar Poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (bgBlur > 0.05f) Modifier.blur((bgBlur * 16f).dp) else Modifier)
                        .graphicsLayer {
                            scaleX = photoScale * if (photoMirrored) -1f else 1f
                            scaleY = photoScale
                            translationX = photoOffsetX * previewSize.width * 0.35f
                            translationY = photoOffsetY * previewSize.height * 0.35f
                            rotationZ = photoRotation
                        },
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(Color(glowColorInt).copy(alpha = 0.25f), Color(0xFF07070A)))),
                )
            }

            // Darken scrim
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgDarken)))

            // Layer 2: Draggable Song Cards (BEHIND foreground person when cutout active!)
            songs.forEachIndexed { idx, song ->
                val slot = cardSlots.getOrElse(idx) { CardSlot(0.25f, 0.25f, 0f) }
                PosterMiniCardPreview(
                    song = song,
                    style = cardStyle,
                    glowColor = Color(glowColorInt),
                    glowStrength = cardGlow,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(0.41f * cardScale)
                        .graphicsLayer {
                            translationX = slot.xFrac * previewSize.width
                            translationY = slot.yFrac * previewSize.height
                            rotationZ = slot.rotDeg
                        }
                        .pointerInput(idx, previewSize) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val w = previewSize.width.coerceAtLeast(1).toFloat()
                                val h = previewSize.height.coerceAtLeast(1).toFloat()
                                val cur = cardSlots[idx]
                                cardSlots[idx] = cur.copy(
                                    xFrac = (cur.xFrac + dragAmount.x / w).coerceIn(-0.12f, 0.72f),
                                    yFrac = (cur.yFrac + dragAmount.y / h).coerceIn(-0.05f, 0.78f),
                                )
                            }
                        },
                )
            }

            // Layer 2.5: 4-Point Sparkle / Lens Flare Glints on card corners & background
            if (sparkleEnabled && sparkleIntensity > 0.05f) {
                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                    val cardW = size.width * 0.41f * cardScale
                    val cardH = cardW * 1.48f
                    cardSlots.forEachIndexed { i, slot ->
                        val left = slot.xFrac * size.width
                        val top = slot.yFrac * size.height
                        val sparkleX = if (i % 2 == 0) left + cardW * 0.90f else left + cardW * 0.12f
                        val sparkleY = top + cardH * 0.06f
                        drawComposeSparkle(Offset(sparkleX, sparkleY), size.width * 0.095f * sparkleIntensity, sparkleIntensity)
                        if (i % 2 == 0) {
                            drawComposeSparkle(Offset(left + cardW * 0.08f, top + cardH * 0.05f), size.width * 0.065f * sparkleIntensity, sparkleIntensity * 0.85f)
                        }
                    }
                    // Ambient floating sparkles near subject silhouette
                    drawComposeSparkle(Offset(size.width * 0.28f, size.height * 0.46f), size.width * 0.105f * sparkleIntensity, sparkleIntensity * 0.9f)
                    drawComposeSparkle(Offset(size.width * 0.69f, size.height * 0.44f), size.width * 0.115f * sparkleIntensity, sparkleIntensity * 0.95f)
                }
            }

            // Layer 3: Foreground Person Cutout (ON TOP of song cards so person is NEVER covered!)
            if (subjectInFront && cutoutBitmap != null) {
                Image(
                    bitmap = cutoutBitmap!!.asImageBitmap(),
                    contentDescription = "Subjek Depan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = photoScale * if (photoMirrored) -1f else 1f
                            scaleY = photoScale
                            translationX = photoOffsetX * previewSize.width * 0.35f
                            translationY = photoOffsetY * previewSize.height * 0.35f
                            rotationZ = photoRotation
                        },
                )
            }

            // Layer 4: Draggable Emoji Stickers
            emojis.forEachIndexed { eIdx, sticker ->
                Text(
                    text = sticker.emoji,
                    fontSize = (30f * sticker.scale).sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .graphicsLayer {
                            translationX = sticker.xFrac * previewSize.width
                            translationY = sticker.yFrac * previewSize.height
                        }
                        .pointerInput(sticker.id, previewSize) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val w = previewSize.width.coerceAtLeast(1).toFloat()
                                val h = previewSize.height.coerceAtLeast(1).toFloat()
                                val cur = emojis[eIdx]
                                emojis[eIdx] = cur.copy(
                                    xFrac = (cur.xFrac + dragAmount.x / w).coerceIn(0f, 0.88f),
                                    yFrac = (cur.yFrac + dragAmount.y / h).coerceIn(0f, 0.88f),
                                )
                            }
                        },
                )
            }

            // Layer 5: Official Swara Watermark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mochy_mark),
                    contentDescription = "Swara",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(13.dp),
                )
                Text("Swara", color = Color.White.copy(alpha = 0.92f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Control Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            PosterTab.entries.forEach { tab ->
                val sel = tab == activeTab
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (sel) Color.White else Color(0xFF18181E))
                        .clickable { activeTab = tab }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                ) {
                    Text(tab.label, color = if (sel) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when (activeTab) {
            PosterTab.PHOTO -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    PosterPillBtn(if (photoBitmap == null) "Pilih foto" else "Ganti foto") { picker.launch("image/*") }
                    if (photoBitmap != null) {
                        PosterPillBtn(if (subjectInFront) "Subjek di depan: ON" else "Subjek di depan: OFF") { subjectInFront = !subjectInFront }
                        PosterPillBtn("Putar") { photoRotation = (photoRotation + 90f) % 360f }
                        PosterPillBtn("Cermin") { photoMirrored = !photoMirrored }
                        PosterPillBtn("Hapus foto") { photoBitmap = null; cutoutData = null; cutoutBitmap = null }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (photoBitmap != null && cutoutData?.hasSubject == true && subjectInFront) {
                    PosterSliderRow("Sensitivitas subjek (siluet/gelap)", 1f - subjectThreshold, 0.25f..0.80f, "${((1f - subjectThreshold) * 100).toInt()}%") { v ->
                        subjectThreshold = (1f - v).coerceIn(0.20f, 0.75f)
                    }
                }
                PosterSliderRow("Ukuran foto", photoScale, 0.5f..2.2f, "${(photoScale * 100).toInt()}%") { photoScale = it }
                PosterSliderRow("Geser kiri-kanan", photoOffsetX, -1f..1f, "${(photoOffsetX * 100).toInt()}%") { photoOffsetX = it }
                PosterSliderRow("Geser atas-bawah", photoOffsetY, -1f..1f, "${(photoOffsetY * 100).toInt()}%") { photoOffsetY = it }
            }
            PosterTab.CARD -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosterPillBtn("Reset posisi kartu") {
                        cardSlots.clear()
                        cardSlots.addAll(cardSlotsFor(songs.size))
                    }
                }
                Spacer(Modifier.height(8.dp))
                PosterSliderRow("Ukuran kartu", cardScale, 0.72f..1.25f, "${(cardScale * 100).toInt()}%") { cardScale = it }
                PosterSliderRow("Cahaya kartu", cardGlow, 0f..1f, "${(cardGlow * 100).toInt()}%") { cardGlow = it }
                Spacer(Modifier.height(8.dp))
                Text("Warna cahaya", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlowPalette.forEach { argb ->
                        val picked = argb == glowColorInt
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(if (picked) 2.5.dp else 1.dp, if (picked) Color.White else Color.White.copy(alpha = 0.25f), CircleShape)
                                .clickable { glowColorInt = argb },
                        )
                    }
                }
            }
            PosterTab.STYLE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardVisualStyle.entries.forEach { st ->
                        val sel = st == cardStyle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (sel) Color.White else Color(0xFF18181E))
                                .clickable { cardStyle = st }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                        ) {
                            Text(st.label, color = if (sel) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            PosterTab.EMOJI -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    QuickEmojis.forEach { em ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF1D1D24))
                                .clickable {
                                    emojis.add(PosterEmojiSticker(id = System.nanoTime(), emoji = em, scale = emojiSizeScale))
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(em, fontSize = 18.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customEmojiText,
                        onValueChange = { if (it.length <= 4) customEmojiText = it },
                        placeholder = { Text("Ketik emoji…", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    PosterPillBtn("Tambah") {
                        val clean = customEmojiText.trim()
                        if (clean.isNotEmpty()) {
                            emojis.add(PosterEmojiSticker(id = System.nanoTime(), emoji = clean, scale = emojiSizeScale))
                            customEmojiText = ""
                        }
                    }
                    if (emojis.isNotEmpty()) {
                        PosterPillBtn("Hapus semua") { emojis.clear() }
                    }
                }
                Spacer(Modifier.height(8.dp))
                PosterSliderRow("Ukuran emoji", emojiSizeScale, 0.6f..2.0f, "${(emojiSizeScale * 100).toInt()}%") { v ->
                    emojiSizeScale = v
                    for (i in emojis.indices) emojis[i] = emojis[i].copy(scale = v)
                }
            }
            PosterTab.EFFECT -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosterPillBtn(if (sparkleEnabled) "✨ Kilau Bintang: ON" else "✨ Kilau Bintang: OFF") {
                        sparkleEnabled = !sparkleEnabled
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (sparkleEnabled) {
                    PosterSliderRow("Intensitas kilau bintang", sparkleIntensity, 0.2f..1.4f, "${(sparkleIntensity * 100).toInt()}%") { sparkleIntensity = it }
                }
                PosterSliderRow("Gelap latar", bgDarken, 0f..0.85f, "${(bgDarken * 100).toInt()}%") { bgDarken = it }
                PosterSliderRow("Kabur latar", bgBlur, 0f..1f, "${(bgBlur * 100).toInt()}%") { bgBlur = it }
            }
        }

        Spacer(Modifier.height(14.dp))
    }
}

private fun DrawScope.drawComposeSparkle(center: Offset, radius: Float, alpha: Float) {
    val clamped = alpha.coerceIn(0f, 1f)
    // Soft radial halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f * clamped), Color.White.copy(alpha = 0.18f * clamped), Color.Transparent),
            center = center,
            radius = radius * 0.72f,
        ),
        radius = radius * 0.72f,
        center = center,
    )
    // 4-point star flare
    val inner = radius * 0.12f
    val path = ComposePath().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(path, color = Color.White.copy(alpha = 0.95f * clamped))
}

@Composable
private fun PosterPillBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF1D1D24))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PosterSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun PosterMiniCardPreview(
    song: PosterSongItem,
    style: CardVisualStyle,
    glowColor: Color,
    glowStrength: Float,
    modifier: Modifier = Modifier,
) {
    val bg = when (style) {
        CardVisualStyle.GLASS -> Color(0x9E242834)
        CardVisualStyle.DARK -> Color(0xEB08080C)
        CardVisualStyle.LIGHT -> Color(0xEEF5F5F7)
    }
    val fg = if (style == CardVisualStyle.LIGHT) Color.Black else Color.White
    val borderBrush = Brush.linearGradient(
        listOf(
            glowColor.copy(alpha = 0.65f + glowStrength * 0.3f),
            glowColor.copy(alpha = 0.16f),
            glowColor.copy(alpha = 0.50f),
        ),
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(bg)
            .border(1.2.dp, borderBrush, RoundedCornerShape(15.dp))
            .padding(7.dp),
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF22222A)),
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = fg, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = fg.copy(alpha = 0.7f), fontSize = 7.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("♡", color = fg.copy(alpha = 0.85f), fontSize = 9.sp)
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(2.5.dp).clip(CircleShape).background(fg.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxWidth(0.52f).height(2.5.dp).background(fg))
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("⏮", color = fg.copy(alpha = 0.85f), fontSize = 7.5.sp)
            Text("⏸", color = fg, fontSize = 8.5.sp)
            Text("⏭", color = fg.copy(alpha = 0.85f), fontSize = 7.5.sp)
        }
    }
}

private fun cardSlotsFor(count: Int): List<CardSlot> = when (count) {
    1 -> listOf(CardSlot(0.28f, 0.22f, -3f))
    2 -> listOf(CardSlot(0.06f, 0.12f, -5f), CardSlot(0.51f, 0.34f, 4f))
    3 -> listOf(CardSlot(0.05f, 0.09f, -5f), CardSlot(0.52f, 0.18f, 4f), CardSlot(0.24f, 0.50f, -2f))
    4 -> listOf(CardSlot(0.06f, 0.09f, -4f), CardSlot(0.52f, 0.10f, 4f), CardSlot(0.05f, 0.50f, 3f), CardSlot(0.52f, 0.50f, -4f))
    else -> listOf(CardSlot(0.04f, 0.06f, -5f), CardSlot(0.53f, 0.09f, 4f), CardSlot(0.27f, 0.30f, -2f), CardSlot(0.05f, 0.56f, 3f), CardSlot(0.52f, 0.58f, -4f))
}

private fun drawTransformedBitmap(
    canvas: Canvas,
    bmp: Bitmap,
    w: Int,
    h: Int,
    photoScale: Float,
    photoOffsetX: Float,
    photoOffsetY: Float,
    photoRotation: Float,
    photoMirrored: Boolean,
) {
    val m = Matrix()
    val baseScale = maxOf(w.toFloat() / bmp.width, h.toFloat() / bmp.height) * photoScale
    m.postTranslate(-bmp.width / 2f, -bmp.height / 2f)
    m.postScale(if (photoMirrored) -baseScale else baseScale, baseScale)
    m.postRotate(photoRotation)
    m.postTranslate(w / 2f + photoOffsetX * w * 0.35f, h / 2f + photoOffsetY * h * 0.35f)
    canvas.drawBitmap(bmp, m, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

private fun drawCanvasSparkle(canvas: Canvas, cx: Float, cy: Float, radius: Float, alpha: Float) {
    val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx,
            cy,
            radius * 0.72f,
            intArrayOf(
                android.graphics.Color.argb((a * 0.85f).toInt(), 255, 255, 255),
                android.graphics.Color.argb((a * 0.22f).toInt(), 255, 255, 255),
                android.graphics.Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawCircle(cx, cy, radius * 0.72f, haloPaint)

    val inner = radius * 0.11f
    val path = Path().apply {
        moveTo(cx, cy - radius)
        lineTo(cx + inner, cy - inner)
        lineTo(cx + radius, cy)
        lineTo(cx + inner, cy + inner)
        lineTo(cx, cy + radius)
        lineTo(cx - inner, cy + inner)
        lineTo(cx - radius, cy)
        lineTo(cx - inner, cy - inner)
        close()
    }
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(a, 255, 255, 255) })
}

private suspend fun renderPosterBitmap(
    songs: List<PosterSongItem>,
    slots: List<CardSlot>,
    emojis: List<PosterEmojiSticker>,
    photo: Bitmap?,
    cutout: Bitmap?,
    photoScale: Float,
    photoOffsetX: Float,
    photoOffsetY: Float,
    photoRotation: Float,
    photoMirrored: Boolean,
    bgDarken: Float,
    sparkleEnabled: Boolean,
    sparkleIntensity: Float,
    cardGlow: Float,
    glowColorInt: Int,
    cardScale: Float,
    cardStyle: CardVisualStyle,
): Bitmap = withContext(Dispatchers.IO) {
    val w = 1080
    val h = 1920
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFF14141C.toInt(), 0xFF060609.toInt(), Shader.TileMode.CLAMP)
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    if (photo != null) {
        drawTransformedBitmap(canvas, photo, w, h, photoScale, photoOffsetX, photoOffsetY, photoRotation, photoMirrored)
    }
    canvas.drawColor(android.graphics.Color.argb((bgDarken * 255f).toInt().coerceIn(0, 255), 0, 0, 0))

    val cardW = w * 0.41f * cardScale
    val cardH = cardW * 1.48f
    val pad = cardW * 0.075f
    val bgCol = when (cardStyle) {
        CardVisualStyle.GLASS -> 0xB4242834.toInt()
        CardVisualStyle.DARK -> 0xF208080C.toInt()
        CardVisualStyle.LIGHT -> 0xF2F5F5F7.toInt()
    }
    val fgCol = if (cardStyle == CardVisualStyle.LIGHT) 0xFF09090B.toInt() else 0xFFFFFFFF.toInt()
    val subCol = if (cardStyle == CardVisualStyle.LIGHT) 0xAA09090B.toInt() else 0xBBFFFFFF.toInt()

    songs.forEachIndexed { idx, song ->
        val slot = slots.getOrElse(idx) { CardSlot(0.25f, 0.25f, 0f) }
        val cx = slot.xFrac * w + cardW / 2f
        val cy = slot.yFrac * h + cardH / 2f
        val artBmp = song.thumbnailUrl?.let { url ->
            runCatching { BitmapFactory.decodeStream(URL(url).openStream()) }.getOrNull()
        }

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(slot.rotDeg)
        val rect = RectF(-cardW / 2f, -cardH / 2f, cardW / 2f, cardH / 2f)
        val corner = 36f

        canvas.drawRoundRect(rect, corner, corner, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgCol })

        // Glossy bevel border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f + cardGlow * 4f
            shader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                android.graphics.Color.argb((160 + cardGlow * 85).toInt().coerceIn(0, 255), android.graphics.Color.red(glowColorInt), android.graphics.Color.green(glowColorInt), android.graphics.Color.blue(glowColorInt)),
                android.graphics.Color.argb(65, 255, 255, 255),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, corner, corner, borderPaint)

        val artSize = cardW - pad * 2
        val artRect = RectF(rect.left + pad, rect.top + pad, rect.left + pad + artSize, rect.top + pad + artSize)
        if (artBmp != null) {
            val clipPath = Path().apply { addRoundRect(artRect, 24f, 24f, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(artBmp, null, artRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            canvas.drawRoundRect(artRect, 24f, 24f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF262630.toInt() })
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgCol; textSize = cardW * 0.076f; isFakeBoldText = true }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = subCol; textSize = cardW * 0.060f }
        val ty = artRect.bottom + pad * 1.15f
        canvas.drawText(ellipsizeText(song.title, titlePaint, artSize * 0.85f), artRect.left, ty, titlePaint)
        canvas.drawText(ellipsizeText(song.artist, artistPaint, artSize * 0.85f), artRect.left, ty + cardW * 0.085f, artistPaint)
        canvas.drawText("♡", artRect.right - cardW * 0.08f, ty, titlePaint)

        val barY = ty + cardW * 0.145f
        canvas.drawRoundRect(RectF(artRect.left, barY, artRect.right, barY + 6f), 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = subCol; alpha = 75 })
        canvas.drawRoundRect(RectF(artRect.left, barY, artRect.left + artSize * 0.52f, barY + 6f), 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgCol })
        canvas.drawCircle(artRect.left + artSize * 0.52f, barY + 3f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgCol })
        canvas.restore()
    }

    // 4-Point Sparkle / Lens Flare Glints
    if (sparkleEnabled && sparkleIntensity > 0.05f) {
        slots.forEachIndexed { i, slot ->
            val left = slot.xFrac * w
            val top = slot.yFrac * h
            val sx = if (i % 2 == 0) left + cardW * 0.90f else left + cardW * 0.12f
            val sy = top + cardH * 0.06f
            drawCanvasSparkle(canvas, sx, sy, w * 0.095f * sparkleIntensity, sparkleIntensity)
            if (i % 2 == 0) {
                drawCanvasSparkle(canvas, left + cardW * 0.08f, top + cardH * 0.05f, w * 0.065f * sparkleIntensity, sparkleIntensity * 0.85f)
            }
        }
        drawCanvasSparkle(canvas, w * 0.28f, h * 0.46f, w * 0.105f * sparkleIntensity, sparkleIntensity * 0.9f)
        drawCanvasSparkle(canvas, w * 0.69f, h * 0.44f, w * 0.115f * sparkleIntensity, sparkleIntensity * 0.95f)
    }

    // Foreground person cutout ON TOP of song cards & sparkles
    if (cutout != null) {
        drawTransformedBitmap(canvas, cutout, w, h, photoScale, photoOffsetX, photoOffsetY, photoRotation, photoMirrored)
    }

    // Emoji stickers
    emojis.forEach { st ->
        val ep = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 110f * st.scale }
        canvas.drawText(st.emoji, st.xFrac * w, st.yFrac * h + 110f * st.scale, ep)
    }

    val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xEBFFFFFF.toInt()
        textSize = 36f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Swara", w / 2f, h - 56f, wmPaint)
    out
}

private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var end = text.length
    while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
    return text.substring(0, end) + "…"
}

private suspend fun savePosterToGallery(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val fileName = "Swara_Poster_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Swara")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
            context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Swara")
            if (!dir.exists()) dir.mkdirs()
            FileOutputStream(File(dir, fileName)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        true
    }.getOrDefault(false)
}

private suspend fun sharePosterBitmap(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
    runCatching {
        val dir = File(context.cacheDir, "posters").apply { mkdirs() }
        val file = File(dir, "swara_poster_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(intent, "Bagikan Poster Swara"))
        }
    }
}
