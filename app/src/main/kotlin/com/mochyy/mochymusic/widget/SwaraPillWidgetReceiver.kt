package com.mochyy.mochymusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mochyy.mochymusic.MainActivity
import com.mochyy.mochymusic.R

/** Widget Baru Swara v4.8.2 — Gaya Minimalis Modern untuk Layar Beranda Android */
class SwaraPillWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.swara_pill_widget)
            views.setOnClickPendingIntent(R.id.swara_pill_root, pi)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
