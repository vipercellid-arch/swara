package com.mochyy.mochymusic.ui.screens.poster

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class SubjectCutoutData(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val maskWidth: Int,
    val maskHeight: Int,
    val confidences: FloatArray,
    val hasSubject: Boolean,
)

/**
 * Menggunakan ML Kit Selfie Segmentation BUNDLED (model AI tertanam 100% offline di APK)
 * sehingga pemotongan subjek orang bekerja instan tanpa gagal unduh modul Play Services.
 */
object SwaraSubjectCutout {
    suspend fun analyzeSubject(source: Bitmap): SubjectCutoutData? = withContext(Dispatchers.Default) {
        runCatching {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
            val segmenter = Segmentation.getClient(options)
            val input = InputImage.fromBitmap(source, 0)
            val mask = suspendCancellableCoroutine { cont ->
                segmenter.process(input)
                    .addOnSuccessListener { result ->
                        if (cont.isActive) cont.resume(result)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            } ?: return@runCatching null

            val mw = mask.width
            val mh = mask.height
            val buf = mask.buffer
            buf.rewind()
            val conf = FloatArray(mw * mh)
            var subjectPixels = 0
            for (i in 0 until mw * mh) {
                val c = buf.float
                conf[i] = c
                if (c >= 0.45f) subjectPixels++
            }
            val coverage = subjectPixels.toFloat() / (mw * mh).coerceAtLeast(1)
            SubjectCutoutData(
                sourceWidth = source.width,
                sourceHeight = source.height,
                maskWidth = mw,
                maskHeight = mh,
                confidences = conf,
                hasSubject = coverage in 0.015f..0.96f,
            )
        }.getOrNull()
    }

    /**
     * Membangun bitmap subjek depan transparan dengan feathering halus di tepi
     * berdasarkan [threshold] (0.20 = sensitif untuk siluet gelap, 0.75 = ketat).
     */
    suspend fun buildCutoutBitmap(
        source: Bitmap,
        data: SubjectCutoutData,
        threshold: Float = 0.42f,
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (!data.hasSubject) return@withContext null
        runCatching {
            val w = source.width
            val h = source.height
            val srcPixels = IntArray(w * h)
            source.getPixels(srcPixels, 0, w, 0, 0, w, h)
            val outPixels = IntArray(w * h)

            val mw = data.maskWidth
            val mh = data.maskHeight
            val conf = data.confidences
            val low = (threshold - 0.14f).coerceIn(0.05f, 0.90f)
            val high = (threshold + 0.14f).coerceIn(low + 0.05f, 0.98f)
            val span = (high - low).coerceAtLeast(0.05f)

            for (y in 0 until h) {
                val my = ((y.toLong() * mh) / h).toInt().coerceIn(0, mh - 1)
                val rowOff = y * w
                val mRowOff = my * mw
                for (x in 0 until w) {
                    val mx = ((x.toLong() * mw) / w).toInt().coerceIn(0, mw - 1)
                    val c = conf[mRowOff + mx]
                    val alphaFactor = ((c - low) / span).coerceIn(0f, 1f)
                    if (alphaFactor > 0.01f) {
                        val px = srcPixels[rowOff + x]
                        val origA = Color.alpha(px)
                        val newA = (origA * alphaFactor).toInt().coerceIn(0, 255)
                        outPixels[rowOff + x] = (newA shl 24) or (px and 0x00FFFFFF)
                    } else {
                        outPixels[rowOff + x] = 0
                    }
                }
            }
            Bitmap.createBitmap(outPixels, w, h, Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }
}
