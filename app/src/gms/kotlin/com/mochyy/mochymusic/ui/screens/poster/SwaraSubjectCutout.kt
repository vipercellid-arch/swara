package com.mochyy.mochymusic.ui.screens.poster

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Memisahkan subjek orang dari foto menggunakan ML Kit Subject Segmentation (khusus build GMS HP/Tablet)
 * agar orang di foto tampil rapi di depan kartu lagu pada Poster Lagu Swara.
 */
object SwaraSubjectCutout {
    suspend fun segmentForeground(source: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        runCatching {
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()
            val segmenter = SubjectSegmentation.getClient(options)
            val input = InputImage.fromBitmap(source, 0)
            suspendCancellableCoroutine<Bitmap?> { cont ->
                segmenter.process(input)
                    .addOnSuccessListener { result ->
                        if (cont.isActive) cont.resume(result.foregroundBitmap)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        }.getOrNull()
    }
}
