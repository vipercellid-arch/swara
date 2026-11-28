package com.mochyy.mochymusic.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mochyy.mochymusic.MainActivity
import com.mochyy.mochymusic.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * System-wide Android WindowManager Overlay untuk Swara Dynamic Island & Notch.
 * Tampil stabil di atas Home Screen & aplikasi lain (WhatsApp, TikTok, Chrome, dll.)
 * dengan FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS + SHORT_EDGES agar pas di kamera punch-hole.
 */
class SwaraDynamicIslandOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var rootHitWrapper: FrameLayout? = null
    private var pillContainer: LinearLayout? = null
    private var compactRow: LinearLayout? = null
    private var expandedCol: LinearLayout? = null

    private var compactArt: ImageView? = null
    private var compactTitle: TextView? = null
    private var compactWave: View? = null

    private var expArt: ImageView? = null
    private var expTitle: TextView? = null
    private var expArtist: TextView? = null
    private var expLyric: TextView? = null
    private var expPlayBtn: ImageView? = null

    private var layoutParams: WindowManager.LayoutParams? = null
    private var expanded = false
    private var cachedArtUrl: String? = null
    private var cachedArtBmp: Bitmap? = null
    private var observeJob: Job? = null

    private fun dp(value: Float): Int = (value * context.resources.displayMetrics.density).toInt()

    fun start() {
        if (observeJob != null) return
        observeJob = scope.launch {
            combine(
                SwaraDynamicIslandState.config,
                SwaraDynamicIslandState.liveTrack,
            ) { cfg, track -> cfg to track }.collect { (cfg, track) ->
                val canOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
                val shouldShow = cfg.enabled && cfg.overlayOutside && canOverlay && track.title.isNotBlank()
                if (!shouldShow) {
                    detach()
                } else {
                    ensureAttached(cfg)
                    applyState(cfg, track)
                }
            }
        }
    }

    fun stop() {
        observeJob?.cancel()
        observeJob = null
        mainHandler.post { detach() }
        scope.cancel()
    }

    private fun ensureAttached(cfg: DynamicIslandConfig) {
        if (rootHitWrapper != null) return

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = dp(cfg.offsetXDp)
            y = dp(cfg.offsetYDp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        layoutParams = lp

        // Generous hit wrapper (min 46dp tall) so 1 light tap always opens/closes without fighting status bar
        val hitWrapper = FrameLayout(context).apply {
            setPadding(dp(10f), 0, dp(10f), dp(12f))
            minimumHeight = dp(46f)
            setOnClickListener { toggleExpanded() }
        }

        val pill = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            elevation = dp(12f).toFloat()
            clipToOutline = true
        }

        // Compact row
        val cRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
        }
        val cArt = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            setBackgroundColor(Color.parseColor("#22222A"))
        }
        val cTitle = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(8f), 0, dp(8f), 0)
        }
        val cWave = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4ADE80"))
            }
        }
        cRow.addView(cArt, LinearLayout.LayoutParams(dp(22f), dp(22f)))
        cRow.addView(cTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        cRow.addView(cWave, LinearLayout.LayoutParams(dp(8f), dp(8f)))

        // Expanded column
        val eCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        }
        val eHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener {
                expanded = false
                updateLayoutVisibility(SwaraDynamicIslandState.config.value)
                runCatching {
                    val launch = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(launch)
                }
            }
        }
        val eArt = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(12f).toFloat())
                }
            }
            setBackgroundColor(Color.parseColor("#22222A"))
        }
        val eTextCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12f), 0, dp(8f), 0)
        }
        val eTitle = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val eArtist = TextView(context).apply {
            setTextColor(Color.argb(175, 255, 255, 255))
            textSize = 12f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        eTextCol.addView(eTitle)
        eTextCol.addView(eArtist)
        eHeader.addView(eArt, LinearLayout.LayoutParams(dp(46f), dp(46f)))
        eHeader.addView(eTextCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val eLyric = TextView(context).apply {
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 11.5f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            visibility = View.GONE
            setPadding(0, dp(8f), 0, 0)
        }

        val eControls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12f), 0, 0)
        }
        val prevBtn = ImageView(context).apply {
            setImageResource(R.drawable.skip_previous)
            setColorFilter(Color.WHITE)
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            setOnClickListener { SwaraDynamicIslandState.playbackActionHandler?.invoke(IslandPlaybackAction.PREV) }
        }
        val playBtn = ImageView(context).apply {
            setImageResource(R.drawable.play)
            setColorFilter(Color.BLACK)
            setPadding(dp(10f), dp(10f), dp(10f), dp(10f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
            setOnClickListener { SwaraDynamicIslandState.playbackActionHandler?.invoke(IslandPlaybackAction.TOGGLE_PLAY) }
        }
        val nextBtn = ImageView(context).apply {
            setImageResource(R.drawable.skip_next)
            setColorFilter(Color.WHITE)
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            setOnClickListener { SwaraDynamicIslandState.playbackActionHandler?.invoke(IslandPlaybackAction.NEXT) }
        }
        eControls.addView(prevBtn, LinearLayout.LayoutParams(dp(48f), dp(42f)))
        eControls.addView(View(context), LinearLayout.LayoutParams(dp(18f), 1))
        eControls.addView(playBtn, LinearLayout.LayoutParams(dp(42f), dp(42f)))
        eControls.addView(View(context), LinearLayout.LayoutParams(dp(18f), 1))
        eControls.addView(nextBtn, LinearLayout.LayoutParams(dp(48f), dp(42f)))

        eCol.addView(eHeader, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        eCol.addView(eLyric, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        eCol.addView(eControls, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        pill.addView(cRow)
        pill.addView(eCol)
        hitWrapper.addView(pill, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL))

        runCatching {
            wm.addView(hitWrapper, lp)
            rootHitWrapper = hitWrapper
            pillContainer = pill
            compactRow = cRow
            expandedCol = eCol
            compactArt = cArt
            compactTitle = cTitle
            compactWave = cWave
            expArt = eArt
            expTitle = eTitle
            expArtist = eArtist
            expLyric = eLyric
            expPlayBtn = playBtn
            SwaraDynamicIslandState.setSystemOverlayActive(true)
        }
    }

    private fun toggleExpanded() {
        val wrapper = rootHitWrapper ?: return
        val transition = AutoTransition().apply {
            duration = 240L
            interpolator = DecelerateInterpolator(1.6f)
        }
        TransitionManager.beginDelayedTransition(wrapper, transition)
        expanded = !expanded
        updateLayoutVisibility(SwaraDynamicIslandState.config.value)
    }

    private fun applyState(cfg: DynamicIslandConfig, track: IslandLiveTrack) {
        val wrapper = rootHitWrapper ?: return
        val lp = layoutParams ?: return
        lp.x = dp(cfg.offsetXDp)
        lp.y = dp(cfg.offsetYDp)
        runCatching { wm.updateViewLayout(wrapper, lp) }

        val compactText = if (cfg.showLyrics && track.currentLyric.isNotBlank()) track.currentLyric else track.title
        compactTitle?.text = compactText
        compactWave?.visibility = if (cfg.showVisualizer) View.VISIBLE else View.GONE
        compactWave?.alpha = if (track.isPlaying) 1f else 0.35f

        expTitle?.text = track.title
        expArtist?.text = track.artist
        if (cfg.showLyrics && track.currentLyric.isNotBlank()) {
            expLyric?.visibility = View.VISIBLE
            expLyric?.text = "♪ ${track.currentLyric}"
        } else {
            expLyric?.visibility = View.GONE
        }
        expPlayBtn?.setImageResource(if (track.isPlaying) R.drawable.pause else R.drawable.play)

        updateLayoutVisibility(cfg)
        loadArtIfNeeded(track.thumbnailUrl)
    }

    private fun updateLayoutVisibility(cfg: DynamicIslandConfig) {
        val pill = pillContainer ?: return
        val isClassic = cfg.style == IslandStyle.CLASSIC
        val corner = if (expanded) dp(26f).toFloat() else dp(18f).toFloat()
        val radii = if (isClassic) {
            floatArrayOf(0f, 0f, 0f, 0f, corner, corner, corner, corner)
        } else {
            val r = if (expanded) dp(28f).toFloat() else dp(99f).toFloat()
            floatArrayOf(r, r, r, r, r, r, r, r)
        }
        pill.background = GradientDrawable().apply {
            setColor(Color.parseColor("#FA060609"))
            cornerRadii = radii
            setStroke(dp(1f), Color.argb(38, 255, 255, 255))
        }

        val targetWidth = if (expanded) {
            dp(312f * cfg.widthScale.coerceIn(0.85f, 1.20f))
        } else {
            dp(190f * cfg.widthScale)
        }
        val plp = pill.layoutParams as? FrameLayout.LayoutParams
        if (plp != null && plp.width != targetWidth) {
            plp.width = targetWidth
            pill.layoutParams = plp
        }

        compactRow?.visibility = if (expanded) View.GONE else View.VISIBLE
        expandedCol?.visibility = if (expanded) View.VISIBLE else View.GONE
    }

    private fun loadArtIfNeeded(url: String?) {
        if (url.isNullOrBlank()) {
            cachedArtUrl = null
            cachedArtBmp = null
            compactArt?.setImageDrawable(null)
            expArt?.setImageDrawable(null)
            return
        }
        if (url == cachedArtUrl && cachedArtBmp != null) {
            compactArt?.setImageBitmap(cachedArtBmp)
            expArt?.setImageBitmap(cachedArtBmp)
            return
        }
        cachedArtUrl = url
        scope.launch {
            val bmp = withContext(Dispatchers.IO) {
                runCatching {
                    val conn = URL(url).openConnection().apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                    }
                    conn.getInputStream().use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            }
            if (bmp != null && cachedArtUrl == url) {
                cachedArtBmp = bmp
                compactArt?.setImageBitmap(bmp)
                expArt?.setImageBitmap(bmp)
            }
        }
    }

    private fun detach() {
        val wrapper = rootHitWrapper ?: return
        runCatching { wm.removeView(wrapper) }
        rootHitWrapper = null
        pillContainer = null
        compactRow = null
        expandedCol = null
        compactArt = null
        compactTitle = null
        compactWave = null
        expArt = null
        expTitle = null
        expArtist = null
        expLyric = null
        expPlayBtn = null
        layoutParams = null
        expanded = false
        SwaraDynamicIslandState.setSystemOverlayActive(false)
    }
}
