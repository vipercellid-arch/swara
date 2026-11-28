package com.mochyy.mochymusic.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate

object SwaraAppState {
    private const val PREFS = "swara_local_prefs"
    private const val KEY_PERF_MODE = "performance_mode"

    private val _recapTrigger = MutableStateFlow(0)
    val recapTrigger: StateFlow<Int> = _recapTrigger

    private val _deviceIntegrationTrigger = MutableStateFlow(0)
    val deviceIntegrationTrigger: StateFlow<Int> = _deviceIntegrationTrigger

    fun triggerOpenRecap() {
        _recapTrigger.value = _recapTrigger.value + 1
    }

    fun consumeOpenRecap() {
        _recapTrigger.value = 0
    }

    fun openDeviceIntegration() {
        _deviceIntegrationTrigger.value = _deviceIntegrationTrigger.value + 1
    }

    fun consumeDeviceIntegration() {
        _deviceIntegrationTrigger.value = 0
    }

    /** Swara Wrap khusus Mingguan (setiap Minggu) dan Bulanan (tanggal 1 setiap bulan). */
    fun isSwaraWrapActive(): Boolean {
        val today = LocalDate.now()
        return today.dayOfMonth == 1 || today.dayOfWeek == DayOfWeek.SUNDAY
    }

    fun swaraWrapPeriodLabel(): String {
        val today = LocalDate.now()
        return if (today.dayOfMonth == 1) "Swara Wrap Bulanan" else "Swara Wrap Mingguan"
    }

    /** Mode Performa lokal: "auto" (Otomatis), "eco" (Anti-Panas), "max" (Maksimal). */
    fun getPerformanceMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PERF_MODE, "auto") ?: "auto"

    fun setPerformanceMode(context: Context, mode: String) {
        val clean = when (mode) {
            "eco", "max", "auto" -> mode
            else -> "auto"
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PERF_MODE, clean)
            .apply()
    }

    fun isEcoModeActive(context: Context): Boolean = getPerformanceMode(context) == "eco"
}
