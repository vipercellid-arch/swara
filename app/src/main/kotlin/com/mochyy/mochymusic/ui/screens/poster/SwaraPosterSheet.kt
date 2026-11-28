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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    EFFECT("Efek"),
}

private enum class CardVisualStyle(val label: String) {
    GLASS("Kaca Gelap"),
    DARK("Hitam Pekat"),
    LIGHT("Terang"),
}

private val GlowPalette = listOf(
    0xFFFFFFFF.toInt(),
    0xFF9FC6E2.toInt(),
    0xFFFCA5A5.toInt(),
    0xFFFDE68A.toInt(),
    0xFF86EFAC.toInt(),
    0xFFC4B5FD.toInt(),
    0xFFF9A8D4.toInt(),
)

/** Menampilkan editor Poster Lagu Swara (pilih 1–5 lagu dari playlist, lalu kustomisasi dan simpan/bagikan). */
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
        SwaraPosterFlow(
            allSongs = items,
            onClose = popup::dismiss,
        )
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
                            if (isSelected) {
                                selectedIds.remove(song.id)
                            } else if (selectedIds.size < 5) {
                                selectedIds.add(song.id)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF23232A)),
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
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable(onClick = onBack),
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
    var photoScale by remember { mutableFloatStateOf(1f) }
    var photoOffsetX by remember { mutableFloatStateOf(0f) }
    var photoOffsetY by remember { mutableFloatStateOf(0f) }
    var photoRotation by remember { mutableFloatStateOf(0f) }
    var photoMirrored by remember { mutableStateOf(false) }

    var bgDarken by remember { mutableFloatStateOf(0.22f) }
    var bgBlur by remember { mutableFloatStateOf(0f) }
    var cardGlow by remember { mutableFloatStateOf(0.40f) }
    var glowColorInt by remember { mutableIntStateOf(GlowPalette.first()) }
    var cardScale by remember { mutableFloatStateOf(1f) }
    var cardStyle by remember { mutableStateOf(CardVisualStyle.GLASS) }
    var saving by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val bmp = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                }.getOrNull()
                if (bmp != null) {
                    withContext(Dispatchers.Main) { photoBitmap = bmp }
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
        // Top Header Bar
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
                                    context = context,
                                    songs = songs,
                                    photo = photoBitmap,
                                    photoScale = photoScale,
                                    photoOffsetX = photoOffsetX,
                                    photoOffsetY = photoOffsetY,
                                    photoRotation = photoRotation,
                                    photoMirrored = photoMirrored,
                                    bgDarken = bgDarken,
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
                                    context = context,
                                    songs = songs,
                                    photo = photoBitmap,
                                    photoScale = photoScale,
                                    photoOffsetX = photoOffsetX,
                                    photoOffsetY = photoOffsetY,
                                    photoRotation = photoRotation,
                                    photoMirrored = photoMirrored,
                                    bgDarken = bgDarken,
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

        Spacer(Modifier.height(14.dp))

        // Live 9:16 Poster Preview
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .aspectRatio(9f / 16f)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A0A0E))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp)),
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap!!.asImageBitmap(),
                    contentDescription = "Latar Poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (bgBlur > 0.5f) Modifier.blur((bgBlur * 16f).dp) else Modifier)
                        .graphicsLayer {
                            scaleX = photoScale * if (photoMirrored) -1f else 1f
                            scaleY = photoScale
                            translationX = photoOffsetX * 180f
                            translationY = photoOffsetY * 240f
                            rotationZ = photoRotation
                        },
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(Color(glowColorInt).copy(alpha = 0.25f), Color(0xFF07070A)),
                            ),
                        ),
                )
            }

            // Darken scrim
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgDarken)))

            // Floating mini song cards arranged stylishly
            val layouts = remember(songs.size) { cardSlotsFor(songs.size) }
            songs.forEachIndexed { idx, song ->
                val slot = layouts[idx]
                PosterMiniCardPreview(
                    song = song,
                    style = cardStyle,
                    glowColor = Color(glowColorInt),
                    glowStrength = cardGlow,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(0.42f * cardScale)
                        .graphicsLayer {
                            translationX = slot.xFrac * 250.dp.toPx()
                            translationY = slot.yFrac * 430.dp.toPx()
                            rotationZ = slot.rotDeg
                        },
                )
            }

            // Official Swara Watermark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mochy_mark),
                    contentDescription = "Swara",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(13.dp),
                )
                Text("Swara", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Control Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosterPillBtn(if (photoBitmap == null) "Pilih foto" else "Ganti foto") { picker.launch("image/*") }
                    if (photoBitmap != null) {
                        PosterPillBtn("Hapus foto") { photoBitmap = null }
                        PosterPillBtn("Putar") { photoRotation = (photoRotation + 90f) % 360f }
                        PosterPillBtn("Cermin") { photoMirrored = !photoMirrored }
                    }
                }
                Spacer(Modifier.height(10.dp))
                PosterSliderRow("Ukuran foto", photoScale, 0.5f..2.2f, "${(photoScale * 100).toInt()}%") { photoScale = it }
                PosterSliderRow("Geser kiri-kanan", photoOffsetX, -1f..1f, "${(photoOffsetX * 100).toInt()}%") { photoOffsetX = it }
                PosterSliderRow("Geser atas-bawah", photoOffsetY, -1f..1f, "${(photoOffsetY * 100).toInt()}%") { photoOffsetY = it }
            }
            PosterTab.CARD -> {
                PosterSliderRow("Ukuran kartu", cardScale, 0.75f..1.25f, "${(cardScale * 100).toInt()}%") { cardScale = it }
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
            PosterTab.EFFECT -> {
                PosterSliderRow("Gelap latar", bgDarken, 0f..0.85f, "${(bgDarken * 100).toInt()}%") { bgDarken = it }
                PosterSliderRow("Kabur latar", bgBlur, 0f..1f, "${(bgBlur * 100).toInt()}%") { bgBlur = it }
            }
        }

        Spacer(Modifier.height(14.dp))
    }
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
        CardVisualStyle.GLASS -> Color(0xCC121218)
        CardVisualStyle.DARK -> Color(0xF008080B)
        CardVisualStyle.LIGHT -> Color(0xEEF5F5F7)
    }
    val fg = if (style == CardVisualStyle.LIGHT) Color.Black else Color.White
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, glowColor.copy(alpha = 0.18f + glowStrength * 0.45f), RoundedCornerShape(14.dp))
            .padding(7.dp),
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF22222A)),
        )
        Spacer(Modifier.height(6.dp))
        Text(song.title, color = fg, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, color = fg.copy(alpha = 0.65f), fontSize = 7.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(fg.copy(alpha = 0.2f))) {
            Box(Modifier.fillMaxWidth(0.45f).height(2.dp).background(fg))
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("⏮", color = fg.copy(alpha = 0.8f), fontSize = 7.sp)
            Text("▶", color = fg, fontSize = 8.sp)
            Text("⏭", color = fg.copy(alpha = 0.8f), fontSize = 7.sp)
        }
    }
}

private data class CardSlot(val xFrac: Float, val yFrac: Float, val rotDeg: Float)

private fun cardSlotsFor(count: Int): List<CardSlot> = when (count) {
    1 -> listOf(CardSlot(0.28f, 0.25f, -3f))
    2 -> listOf(CardSlot(0.06f, 0.14f, -5f), CardSlot(0.50f, 0.36f, 4f))
    3 -> listOf(CardSlot(0.05f, 0.10f, -5f), CardSlot(0.51f, 0.22f, 4f), CardSlot(0.24f, 0.50f, -2f))
    4 -> listOf(CardSlot(0.05f, 0.08f, -5f), CardSlot(0.52f, 0.12f, 4f), CardSlot(0.06f, 0.48f, 3f), CardSlot(0.51f, 0.52f, -4f))
    else -> listOf(CardSlot(0.04f, 0.06f, -5f), CardSlot(0.53f, 0.09f, 4f), CardSlot(0.27f, 0.32f, -2f), CardSlot(0.05f, 0.56f, 3f), CardSlot(0.52f, 0.58f, -4f))
}

private suspend fun renderPosterBitmap(
    context: Context,
    songs: List<PosterSongItem>,
    photo: Bitmap?,
    photoScale: Float,
    photoOffsetX: Float,
    photoOffsetY: Float,
    photoRotation: Float,
    photoMirrored: Boolean,
    bgDarken: Float,
    cardGlow: Float,
    glowColorInt: Int,
    cardScale: Float,
    cardStyle: CardVisualStyle,
): Bitmap = withContext(Dispatchers.IO) {
    val w = 1080
    val h = 1920
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)

    // Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFF14141C.toInt(), 0xFF060609.toInt(), Shader.TileMode.CLAMP)
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    if (photo != null) {
        val m = Matrix()
        val baseScale = maxOf(w.toFloat() / photo.width, h.toFloat() / photo.height) * photoScale
        m.postTranslate(-photo.width / 2f, -photo.height / 2f)
        m.postScale(if (photoMirrored) -baseScale else baseScale, baseScale)
        m.postRotate(photoRotation)
        m.postTranslate(w / 2f + photoOffsetX * 360f, h / 2f + photoOffsetY * 480f)
        canvas.drawBitmap(photo, m, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    // Darken overlay
    canvas.drawColor(android.graphics.Color.argb((bgDarken * 255f).toInt().coerceIn(0, 255), 0, 0, 0))

    // Draw each song card
    val slots = cardSlotsFor(songs.size)
    val cardW = w * 0.41f * cardScale
    val cardH = cardW * 1.46f
    val pad = cardW * 0.075f
    val bgCol = when (cardStyle) {
        CardVisualStyle.GLASS -> 0xD8121218.toInt()
        CardVisualStyle.DARK -> 0xF508080B.toInt()
        CardVisualStyle.LIGHT -> 0xF2F5F5F7.toInt()
    }
    val fgCol = if (cardStyle == CardVisualStyle.LIGHT) 0xFF09090B.toInt() else 0xFFFFFFFF.toInt()
    val subCol = if (cardStyle == CardVisualStyle.LIGHT) 0xAA09090B.toInt() else 0xAAFFFFFF.toInt()

    songs.forEachIndexed { idx, song ->
        val slot = slots[idx]
        val cx = slot.xFrac * w + cardW / 2f
        val cy = slot.yFrac * h + cardH / 2f
        val artBmp = song.thumbnailUrl?.let { url ->
            runCatching { BitmapFactory.decodeStream(URL(url).openStream()) }.getOrNull()
        }

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(slot.rotDeg)
        val rect = RectF(-cardW / 2f, -cardH / 2f, cardW / 2f, cardH / 2f)
        val corner = 34f

        // Glow border
        if (cardGlow > 0.02f) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 6f + cardGlow * 12f
                color = android.graphics.Color.argb((cardGlow * 140f).toInt().coerceIn(0, 255), android.graphics.Color.red(glowColorInt), android.graphics.Color.green(glowColorInt), android.graphics.Color.blue(glowColorInt))
            }
            canvas.drawRoundRect(rect, corner, corner, glowPaint)
        }

        // Card fill
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgCol }
        canvas.drawRoundRect(rect, corner, corner, fillPaint)

        // Cover art
        val artSize = cardW - pad * 2
        val artRect = RectF(rect.left + pad, rect.top + pad, rect.left + pad + artSize, rect.top + pad + artSize)
        if (artBmp != null) {
            val clipPath = android.graphics.Path().apply { addRoundRect(artRect, 22f, 22f, android.graphics.Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(artBmp, null, artRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val ph = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF262630.toInt() }
            canvas.drawRoundRect(artRect, 22f, 22f, ph)
        }

        // Title & Artist
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgCol
            textSize = cardW * 0.078f
            isFakeBoldText = true
        }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subCol
            textSize = cardW * 0.062f
        }
        val tText = ellipsizeText(song.title, titlePaint, artSize)
        val aText = ellipsizeText(song.artist, artistPaint, artSize)
        val ty = artRect.bottom + pad * 1.15f
        canvas.drawText(tText, artRect.left, ty, titlePaint)
        canvas.drawText(aText, artRect.left, ty + cardW * 0.088f, artistPaint)

        // Progress bar
        val barY = ty + cardW * 0.14f
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = subCol; alpha = 60 }
        val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgCol }
        canvas.drawRoundRect(RectF(artRect.left, barY, artRect.right, barY + 6f), 3f, 3f, trackPaint)
        canvas.drawRoundRect(RectF(artRect.left, barY, artRect.left + artSize * 0.45f, barY + 6f), 3f, 3f, progPaint)
        canvas.restore()
    }

    // Swara watermark at bottom
    val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6FFFFFF.toInt()
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
    while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
        end--
    }
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
