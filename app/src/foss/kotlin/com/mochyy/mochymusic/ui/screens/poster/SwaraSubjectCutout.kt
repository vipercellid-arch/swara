package com.mochyy.mochymusic.ui.screens.poster

import android.graphics.Bitmap

data class SubjectCutoutData(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val maskWidth: Int,
    val maskHeight: Int,
    val confidences: FloatArray,
    val hasSubject: Boolean,
)

/** Stub non-GMS untuk flavor FOSS. */
object SwaraSubjectCutout {
    suspend fun analyzeSubject(source: Bitmap): SubjectCutoutData? = null
    suspend fun buildCutoutBitmap(source: Bitmap, data: SubjectCutoutData, threshold: Float = 0.42f): Bitmap? = null
}
