// Halaman Donasi Khusus Swara — QRIS ViperCell (zoomable + Simpan ke Galeri, tanpa tombol Salin NMID)

package com.mochyy.mochymusic.ui.screens.donate

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.PopupSheetState
import com.mochyy.mochymusic.ui.screens.update.VersionRing
import java.io.File
import java.io.FileOutputStream

private val TitleOverlap = 18.dp

/** Menampilkan halaman Donasi QRIS ViperCell khusus Swara. */
fun PopupSheetState.showDonateSheet() {
    val popup = this
    show(
        fullBleed = true,
        buttonBar = {
            DonatePillButton(
                text = "Tutup",
                onClick = popup::dismiss,
                quiet = true,
            )
        },
    ) {
        DonateSheetContent()
    }
}

@Composable
private fun DonateSheetContent() {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    var showZoomDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val characterWidth = characterWidth(maxWidth, maxHeight)

        VersionRing(
            text = "SWARA MUSIC",
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .layout { measurable, constraints ->
                        val loose =
                            constraints.copy(
                                minWidth = 0,
                                minHeight = 0,
                                maxWidth = Constraints.Infinity,
                                maxHeight = Constraints.Infinity,
                            )
                        val placeable = measurable.measure(loose)
                        layout(0, 0) { placeable.place(x = (-198).dp.roundToPx(), y = (-109).dp.roundToPx()) }
                    }.graphicsLayer { translationY = -scroll.value.toFloat() },
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(CharacterTop))
            DonateCharacter(
                width = characterWidth,
                overlapBelow = TitleOverlap,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = "Dukung Swara",
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.2).sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Setiap dukunganmu membantu biaya server Swara Cloud & pengembangan fitur baru oleh Teves.",
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                color = DonateSoftText,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            // Kartu QRIS ViperCell — ketuk untuk perbesar
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .clickable { showZoomDialog = true }
                        .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.qris_vipercell),
                    contentDescription = "QRIS ViperCell — Dukung Swara",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ketuk gambar QRIS untuk memperbesar",
                fontSize = 12.sp,
                color = DonateSoftText,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            DonatePillButton(
                text = "Simpan QRIS ke Galeri",
                onClick = {
                    val saved = saveQrisToGallery(context)
                    Toast.makeText(
                        context,
                        if (saved) "QRIS ViperCell tersimpan ke Galeri" else "Gagal menyimpan QRIS",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Terima kasih banyak sudah mendukung Swara! Bebas iklan dan gratis selamanya.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DonateSoftText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showZoomDialog) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Dialog(
            onDismissRequest = { showZoomDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.94f))
                        .clickable { showZoomDialog = false }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.qris_vipercell),
                    contentDescription = "QRIS ViperCell Fullscreen",
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                            ),
                )
            }
        }
    }
}

private fun saveQrisToGallery(context: Context): Boolean = runCatching {
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.qris_vipercell) ?: return false
    val fileName = "QRIS_Swara_ViperCell_${System.currentTimeMillis()}.jpg"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Swara")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
    } else {
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Swara")
        if (!dir.exists()) dir.mkdirs()
        FileOutputStream(File(dir, fileName)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
    }
    true
}.getOrDefault(false)
