package com.mochyy.mochymusic.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Klien Swara Cloud v4.8.6 (http://103.119.237.122:3600 / https://swaraapp.my.id)
 * Fokus: Autentikasi Google Native & Email, Sinkronisasi Profil/Avatar/Statistik,
 * dan Swara Connect Real-Time (sinkronisasi pemutaran lintas perangkat gaya Spotify Connect).
 */
object SwaraCloudClient {
    const val BASE_URL = "http://103.119.237.122:3600"
    const val PUBLIC_WEB_URL = "https://swaraapp.my.id"
    const val VIPERCELL_GOOGLE_CLIENT_ID =
        "710483059781-l7qj30ph11kv82ucgmdf31o2anqcj1ni.apps.googleusercontent.com"
    const val PREFS = "swara_cloud_session"

    data class AuthResult(
        val ok: Boolean,
        val token: String = "",
        val username: String = "",
        val displayName: String = "",
        val avatarUrl: String = "",
        val isNew: Boolean = false,
        val code: String = "",
        val email: String = "",
        val error: String = "",
    )

    data class SimpleResult(val ok: Boolean, val message: String = "", val extra: String = "")

    data class SyncedSong(
        val id: String,
        val title: String,
        val artist: String = "",
        val thumbnailUrl: String = "",
        val duration: Int = -1,
        val liked: Boolean = true,
    )

    data class AdminBanner(
        val slot: String,
        val imageUrl: String,
        val title: String = "",
        val subtitle: String = "",
        val linkUrl: String = "",
    )

    data class SyncedPlaylist(
        val id: String,
        val name: String,
        val songs: List<SyncedSong> = emptyList(),
    )

    data class SyncStatsResult(
        val ok: Boolean,
        val totalListeningSec: Long = 0L,
        val totalPlays: Int = 0,
        val updatedAt: Long = 0L,
        val displayName: String = "",
        val avatarUrl: String = "",
        val liked: List<SyncedSong> = emptyList(),
        val topSongs: List<SyncedSong> = emptyList(),
        val playlists: List<SyncedPlaylist> = emptyList(),
        val error: String = "",
    )

    private val _recapTrigger = MutableStateFlow(0)
    val recapTrigger: StateFlow<Int> = _recapTrigger

    fun triggerOpenRecap() {
        _recapTrigger.value = _recapTrigger.value + 1
    }

    fun handleRecapDeepLink(uri: Uri?): Boolean {
        if (uri == null) return false
        val isCustom = uri.scheme == "swara" && uri.host == "recap"
        val isWeb = (uri.scheme == "https" || uri.scheme == "http") &&
            uri.host?.contains("swaraapp.my.id", ignoreCase = true) == true &&
            uri.path?.startsWith("/recap") == true
        if (!isCustom && !isWeb) return false
        triggerOpenRecap()
        return true
    }

    data class ConnectedDevice(
        val deviceId: String,
        val deviceName: String,
        val deviceType: String,
        val online: Boolean,
        val lastSeen: Long,
    )

    data class SyncedPlayback(
        val sourceDeviceId: String,
        val sourceDeviceName: String,
        val mediaId: String,
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val positionMs: Long,
        val updatedAt: Long = 0L,
        val coverUrl: String = "",
        val durationMs: Long = 0L,
        val lyrics: String = "",
    )

    data class RemoteCommand(
        val id: String,
        val targetDeviceId: String,
        val action: String,
        val fromDevice: String,
    )

    data class MultiDeviceState(
        val ok: Boolean,
        val devices: List<ConnectedDevice> = emptyList(),
        val playback: SyncedPlayback? = null,
        val commands: List<RemoteCommand> = emptyList(),
        val error: String = "",
    )

    data class ServerVersionInfo(
        val latestVersion: String,
        val versionCode: Int,
        val tag: String,
        val title: String,
        val downloadUrl: String,
        val apkUrl: String,
        val changelog: String,
        val apkSizeBytes: Long = 0L,
    )

    data class ServerPlaylistImport(
        val ok: Boolean,
        val name: String = "",
        val tracks: List<Pair<String, String>> = emptyList(),
        val error: String = "",
    )

    data class SavedSession(
        val token: String,
        val username: String,
        val displayName: String,
        val avatarUrl: String = "",
        val isNewAccount: Boolean = false,
        val lastSyncEpochSec: Long = 0L,
    ) {
        val isLoggedIn: Boolean get() = token.isNotBlank()
    }

    private fun findActivity(context: Context): Activity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    suspend fun signInWithGoogleNative(context: Context): AuthResult {
        val activity = findActivity(context)
            ?: return AuthResult(ok = false, error = "Tidak dapat membuka dialog Google")
        return try {
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(VIPERCELL_GOOGLE_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                verifyGoogleIdToken(googleCred.idToken)
            } else {
                AuthResult(ok = false, error = "Kredensial Google tidak dikenali")
            }
        } catch (_: GetCredentialCancellationException) {
            AuthResult(ok = false, error = "Dibatalkan")
        } catch (_: NoCredentialException) {
            AuthResult(ok = false, error = "Akun Google tidak tersedia di perangkat")
        } catch (e: Exception) {
            AuthResult(ok = false, error = e.localizedMessage ?: "Login Google gagal")
        }
    }

    suspend fun verifyGoogleIdToken(idToken: String): AuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("credential", idToken).put("idToken", idToken)
        parseAuth(postJson("$BASE_URL/api/auth/google-idtoken", payload))
    }

    suspend fun uploadAvatar(context: Context, token: String, rawUriOrPath: String): SimpleResult =
        withContext(Dispatchers.IO) {
            if (token.isBlank() || rawUriOrPath.isBlank() || rawUriOrPath.startsWith("http")) {
                return@withContext SimpleResult(false, "Lewati")
            }
            val bmp = runCatching {
                val stream = if (rawUriOrPath.startsWith("content://")) {
                    context.contentResolver.openInputStream(Uri.parse(rawUriOrPath))
                } else {
                    val clean = rawUriOrPath.removePrefix("file://")
                    File(clean).takeIf { it.exists() }?.inputStream()
                }
                stream?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull() ?: return@withContext SimpleResult(false, "Gagal membaca gambar")

            val scaled = Bitmap.createScaledBitmap(bmp, 320, 320, true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            val resp = postJson("$BASE_URL/api/user/avatar", JSONObject().put("imageBase64", b64), token)
                ?: return@withContext SimpleResult(false, "Server tidak terjangkau")
            if (resp.optBoolean("ok", false)) {
                val avUrl = resp.optString("avatarUrl")
                if (avUrl.isNotBlank()) {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("avatar_url", avUrl).apply()
                }
                SimpleResult(true, "Tersinkron", extra = avUrl)
            } else {
                SimpleResult(false, resp.optString("error", "Gagal unggah foto"))
            }
        }

    fun getPerformanceMode(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("perf_mode", "auto") ?: "auto"
    }

    fun setPerformanceMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("perf_mode", mode).apply()
    }

    fun isEcoModeActive(context: Context): Boolean {
        return when (getPerformanceMode(context)) {
            "eco" -> true
            "max" -> false
            else -> {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val mi = ActivityManager.MemoryInfo()
                am?.getMemoryInfo(mi)
                val totalGb = mi.totalMem / (1024L * 1024L * 1024L)
                totalGb <= 6L
            }
        }
    }

    fun handleAuthDeepLink(context: Context, uri: Uri?): Boolean {
        if (uri == null || uri.scheme != "swara" || uri.host != "auth") return false
        val token = uri.getQueryParameter("token").orEmpty()
        val email = uri.getQueryParameter("email").orEmpty()
        val name = uri.getQueryParameter("name").orEmpty().ifBlank { email.substringBefore("@") }
        val avatar = uri.getQueryParameter("avatar").orEmpty()
        val isNewRaw = uri.getQueryParameter("isNew").orEmpty()
        val isNew = isNewRaw == "1" || isNewRaw.equals("true", ignoreCase = true)
        if (token.isBlank()) return false
        saveSession(context, token, email, name, avatar, isNew)
        return true
    }

    fun getOrCreateDeviceId(context: Context): String {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = sp.getString("device_id", null)
        if (!existing.isNullOrBlank()) return existing
        val created = "swara_${UUID.randomUUID().toString().take(10)}"
        sp.edit().putString("device_id", created).apply()
        return created
    }

    fun getDeviceName(): String {
        val brand = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty()
        return if (model.startsWith(brand, ignoreCase = true)) model else "$brand $model".trim()
    }

    fun getSession(context: Context): SavedSession {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return SavedSession(
            token = sp.getString("token", "").orEmpty(),
            username = sp.getString("username", "").orEmpty(),
            displayName = sp.getString("display_name", "").orEmpty(),
            avatarUrl = sp.getString("avatar_url", "").orEmpty(),
            isNewAccount = sp.getBoolean("is_new_account", false),
            lastSyncEpochSec = sp.getLong("last_sync", 0L),
        )
    }

    fun saveSession(
        context: Context,
        token: String,
        username: String,
        displayName: String,
        avatarUrl: String = "",
        isNewAccount: Boolean = false,
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("token", token)
            .putString("username", username)
            .putString("display_name", displayName)
            .putString("avatar_url", avatarUrl)
            .putBoolean("is_new_account", isNewAccount)
            .apply()
    }

    fun getCloudListeningSec(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("cloud_listening_sec", 0L)
    }

    fun saveCloudListeningSec(context: Context, sec: Long) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val best = maxOf(sec, sp.getLong("cloud_listening_sec", 0L))
        sp.edit().putLong("cloud_listening_sec", best).apply()
    }

    fun getCloudPlays(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("cloud_plays", 0)
    }

    fun saveCloudPlays(context: Context, plays: Int) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val best = maxOf(plays, sp.getInt("cloud_plays", 0))
        sp.edit().putInt("cloud_plays", best).apply()
    }

    fun clearSession(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val devId = sp.getString("device_id", null)
        val perf = sp.getString("perf_mode", "auto")
        sp.edit().clear().apply()
        sp.edit().putString("device_id", devId).putString("perf_mode", perf).apply()
    }

    suspend fun register(emailOrUsername: String, password: String, displayName: String): AuthResult =
        withContext(Dispatchers.IO) {
            val clean = emailOrUsername.trim().lowercase()
            val payload = JSONObject().apply {
                put("email", clean)
                put("username", clean)
                put("password", password)
                put("displayName", displayName.trim().ifBlank { clean.substringBefore("@") })
            }
            parseAuth(postJson("$BASE_URL/api/auth/register", payload))
        }

    suspend fun requestRegisterOtp(email: String, password: String, displayName: String): SimpleResult =
        withContext(Dispatchers.IO) {
            val clean = email.trim().lowercase()
            val payload = JSONObject().apply {
                put("email", clean)
                put("password", password)
                put("displayName", displayName.trim().take(8))
            }
            val resp = postJson("$BASE_URL/api/auth/register-request", payload)
                ?: return@withContext SimpleResult(false, "Gagal terhubung")
            SimpleResult(resp.optBoolean("ok", false), resp.optString("message", resp.optString("error", "Gagal")))
        }

    suspend fun verifyRegisterOtp(email: String, otp: String): AuthResult =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("otp", otp.trim())
            }
            parseAuth(postJson("$BASE_URL/api/auth/register-verify", payload))
        }

    suspend fun fetchHomeBanners(): List<AdminBanner> = withContext(Dispatchers.IO) {
        val resp = getJson("$BASE_URL/api/public/highlights") ?: return@withContext emptyList()
        if (!resp.optBoolean("ok", false)) return@withContext emptyList()
        val arr = resp.optJSONArray("banners") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val img = o.optString("imageUrl").trim()
                if (img.isNotBlank()) {
                    add(
                        AdminBanner(
                            slot = o.optString("slot"),
                            imageUrl = img,
                            title = o.optString("title"),
                            subtitle = o.optString("subtitle"),
                            linkUrl = o.optString("linkUrl"),
                        ),
                    )
                }
            }
        }
    }

    suspend fun login(emailOrUsername: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val clean = emailOrUsername.trim().lowercase()
            val payload = JSONObject().apply {
                put("email", clean)
                put("username", clean)
                put("password", password)
            }
            parseAuth(postJson("$BASE_URL/api/auth/login", payload))
        }

    suspend fun requestPasswordOtp(email: String): SimpleResult = withContext(Dispatchers.IO) {
        val resp = postJson("$BASE_URL/api/auth/forgot-password", JSONObject().put("email", email.trim().lowercase()))
            ?: return@withContext SimpleResult(false, "Server tidak terjangkau")
        SimpleResult(resp.optBoolean("ok", false), resp.optString("message", resp.optString("error", "Gagal kirim OTP")))
    }

    suspend fun resetPasswordWithOtp(email: String, otp: String, newPassword: String): SimpleResult =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("otp", otp.trim())
                put("newPassword", newPassword)
            }
            val resp = postJson("$BASE_URL/api/auth/reset-password", payload)
                ?: return@withContext SimpleResult(false, "Server tidak terjangkau")
            SimpleResult(resp.optBoolean("ok", false), resp.optString("message", resp.optString("error", "Gagal ubah sandi")))
        }

    // ---- SWARA CONNECT (Sinkronisasi Lintas Perangkat Real-Time) ----

    suspend fun heartbeatDevice(context: Context, token: String): MultiDeviceState = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext MultiDeviceState(ok = false, error = "Belum login")
        val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
        val payload = JSONObject().apply {
            put("deviceId", getOrCreateDeviceId(context))
            put("deviceName", getDeviceName())
            put("deviceType", if (isTablet) "Tablet" else "Ponsel")
        }
        parseMultiDevice(postJson("$BASE_URL/api/devices/heartbeat", payload, token))
    }

    suspend fun pushPlaybackState(
        context: Context,
        token: String,
        mediaId: String,
        title: String,
        artist: String,
        isPlaying: Boolean,
        positionMs: Long,
        coverUrl: String = "",
        durationMs: Long = 0L,
        lyrics: String = "",
    ): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank() || mediaId.isBlank()) return@withContext false
        val payload = JSONObject().apply {
            put("deviceId", getOrCreateDeviceId(context))
            put("deviceName", getDeviceName())
            put("mediaId", mediaId)
            put("title", title)
            put("artist", artist)
            put("isPlaying", isPlaying)
            put("positionMs", positionMs)
            put("coverUrl", coverUrl)
            put("durationMs", durationMs)
            put("lyrics", lyrics.take(28_000))
        }
        postJson("$BASE_URL/api/devices/playback", payload, token)?.optBoolean("ok", false) == true
    }

    suspend fun fetchDeviceState(context: Context, token: String): MultiDeviceState = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext MultiDeviceState(ok = false)
        val devId = URLEncoder.encode(getOrCreateDeviceId(context), "UTF-8")
        parseMultiDevice(getJson("$BASE_URL/api/devices/state?deviceId=$devId", token))
    }

    suspend fun sendRemoteCommand(
        context: Context,
        token: String,
        targetDeviceId: String,
        action: String,
    ): SimpleResult = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext SimpleResult(false, "Belum masuk akun")
        val payload = JSONObject().apply {
            put("sourceDeviceId", getOrCreateDeviceId(context))
            put("fromDevice", getDeviceName())
            put("targetDeviceId", targetDeviceId)
            put("action", action)
        }
        val resp = postJson("$BASE_URL/api/devices/command", payload, token)
            ?: return@withContext SimpleResult(false, "Server tidak terjangkau")
        if (resp.optBoolean("ok", false)) SimpleResult(true, "Terkirim")
        else SimpleResult(false, resp.optString("error", "Gagal"))
    }

    suspend fun pushStats(
        token: String,
        totalListeningSec: Long,
        totalPlays: Int,
        likedJson: JSONArray = JSONArray(),
        playlistsJson: JSONArray = JSONArray(),
        topSongsJson: JSONArray = JSONArray(),
    ): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext false
        val payload = JSONObject().apply {
            put("totalListeningSec", totalListeningSec)
            put("totalPlays", totalPlays)
            put("liked", likedJson)
            put("playlists", playlistsJson)
            put("topSongs", topSongsJson)
        }
        postJson("$BASE_URL/api/sync/stats", payload, token)?.optBoolean("ok", false) == true
    }

    suspend fun pushListenDelta(
        token: String,
        deltaSec: Long,
        deltaPlays: Int = 0,
    ): SyncStatsResult = withContext(Dispatchers.IO) {
        if (token.isBlank() || (deltaSec <= 0L && deltaPlays <= 0)) return@withContext SyncStatsResult(ok = false)
        val payload = JSONObject().apply {
            put("deltaSec", deltaSec.coerceIn(0L, 3600L))
            put("deltaPlays", deltaPlays.coerceIn(0, 50))
        }
        val resp = postJson("$BASE_URL/api/sync/listen-delta", payload, token)
            ?: return@withContext SyncStatsResult(ok = false, error = "Server tidak terjangkau")
        if (!resp.optBoolean("ok", false)) return@withContext SyncStatsResult(ok = false, error = resp.optString("error"))
        SyncStatsResult(
            ok = true,
            totalListeningSec = resp.optLong("totalListeningSec", 0L),
            totalPlays = resp.optInt("totalPlays", 0),
        )
    }

    suspend fun pullStats(token: String): SyncStatsResult = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext SyncStatsResult(ok = false, error = "Belum masuk akun")
        val resp = getJson("$BASE_URL/api/sync/restore", token)
            ?: return@withContext SyncStatsResult(ok = false, error = "Server tidak terjangkau")
        if (!resp.optBoolean("ok", false)) return@withContext SyncStatsResult(ok = false, error = resp.optString("error"))
        fun parseSongs(arr: JSONArray?, defaultLiked: Boolean): List<SyncedSong> {
            if (arr == null) return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id").trim()
                    val title = o.optString("title").trim()
                    if (id.isNotBlank() && title.isNotBlank()) {
                        add(
                            SyncedSong(
                                id = id,
                                title = title,
                                artist = o.optString("artist"),
                                thumbnailUrl = o.optString("thumbnailUrl"),
                                duration = o.optInt("duration", -1),
                                liked = o.optBoolean("liked", defaultLiked),
                            ),
                        )
                    }
                }
            }
        }
        SyncStatsResult(
            ok = true,
            totalListeningSec = resp.optLong("totalListeningSec", 0L),
            totalPlays = resp.optInt("totalPlays", 0),
            updatedAt = resp.optLong("updatedAt", 0L),
            displayName = resp.optString("displayName"),
            avatarUrl = resp.optString("avatarUrl"),
            liked = parseSongs(resp.optJSONArray("liked"), true),
            topSongs = parseSongs(resp.optJSONArray("topSongs"), false),
            playlists = buildList {
                val pArr = resp.optJSONArray("playlists") ?: JSONArray()
                for (i in 0 until pArr.length()) {
                    val po = pArr.optJSONObject(i) ?: continue
                    val pid = po.optString("id").trim()
                    val pname = po.optString("name").trim()
                    if (pid.isNotBlank() && pname.isNotBlank()) {
                        add(SyncedPlaylist(pid, pname, parseSongs(po.optJSONArray("songs"), false)))
                    }
                }
            },
        )
    }

    suspend fun fetchServerVersion(): ServerVersionInfo? = withContext(Dispatchers.IO) {
        val resp = getJson("$BASE_URL/api/app/version") ?: return@withContext null
        if (!resp.optBoolean("ok", false)) return@withContext null
        val ver = resp.optString("latestVersion").ifBlank { return@withContext null }
        ServerVersionInfo(
            latestVersion = ver,
            versionCode = resp.optInt("versionCode", 0),
            tag = resp.optString("tag", "v$ver"),
            title = resp.optString("title", "Swara v$ver"),
            downloadUrl = resp.optString("downloadUrl", "$BASE_URL/api/app/version"),
            apkUrl = resp.optString("apkUrl", ""),
            changelog = resp.optString("changelog", ""),
            apkSizeBytes = resp.optLong("apkSizeBytes", 0L),
        )
    }

    suspend fun importPlaylistViaServer(url: String): ServerPlaylistImport = withContext(Dispatchers.IO) {
        val resp = postJson("$BASE_URL/api/import/playlist", JSONObject().put("url", url.trim()))
            ?: return@withContext ServerPlaylistImport(ok = false, error = "Server tidak terjangkau")
        if (!resp.optBoolean("ok", false)) return@withContext ServerPlaylistImport(ok = false, error = resp.optString("error"))
        val arr = resp.optJSONArray("tracks") ?: JSONArray()
        val tracks = buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val t = obj.optString("title").trim()
                if (t.isNotBlank()) add(t to obj.optString("artist").trim())
            }
        }
        ServerPlaylistImport(ok = tracks.isNotEmpty(), name = resp.optString("name", "Playlist Impor"), tracks = tracks)
    }

    private fun parseMultiDevice(resp: JSONObject?): MultiDeviceState {
        if (resp == null) return MultiDeviceState(ok = false, error = "Server tidak terjangkau")
        if (!resp.optBoolean("ok", false)) return MultiDeviceState(ok = false, error = resp.optString("error"))
        val devArr = resp.optJSONArray("devices") ?: JSONArray()
        val devices = buildList {
            for (i in 0 until devArr.length()) {
                val d = devArr.optJSONObject(i) ?: continue
                add(ConnectedDevice(d.optString("deviceId"), d.optString("deviceName", "Perangkat"), d.optString("deviceType", "Android"), d.optBoolean("online", false), d.optLong("lastSeen", 0L)))
            }
        }
        val pbObj = resp.optJSONObject("playback")
        val playback = pbObj?.let {
            val mid = it.optString("mediaId")
            if (mid.isBlank()) null else SyncedPlayback(
                sourceDeviceId = it.optString("sourceDeviceId"),
                sourceDeviceName = it.optString("sourceDeviceName"),
                mediaId = mid,
                title = it.optString("title"),
                artist = it.optString("artist"),
                isPlaying = it.optBoolean("isPlaying", false),
                positionMs = it.optLong("positionMs", 0L),
                updatedAt = it.optLong("updatedAt", 0L),
                coverUrl = it.optString("coverUrl"),
                durationMs = it.optLong("durationMs", 0L),
                lyrics = it.optString("lyrics"),
            )
        }
        val cmdArr = resp.optJSONArray("commands") ?: JSONArray()
        val commands = buildList {
            for (i in 0 until cmdArr.length()) {
                val c = cmdArr.optJSONObject(i) ?: continue
                add(RemoteCommand(c.optString("id"), c.optString("targetDeviceId"), c.optString("action"), c.optString("fromDevice")))
            }
        }
        return MultiDeviceState(ok = true, devices = devices, playback = playback, commands = commands)
    }

    private fun parseAuth(resp: JSONObject?): AuthResult {
        if (resp == null) return AuthResult(ok = false, error = "Server tidak terjangkau")
        return AuthResult(
            ok = resp.optBoolean("ok", false),
            token = resp.optString("token"),
            username = resp.optString("username"),
            displayName = resp.optString("displayName"),
            avatarUrl = resp.optString("avatarUrl"),
            isNew = resp.optBoolean("isNew", false),
            code = resp.optString("code"),
            email = resp.optString("email"),
            error = resp.optString("error"),
        )
    }

    private fun getJson(urlStr: String, token: String = ""): JSONObject? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 10_000
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val txt = stream?.bufferedReader()?.use { it.readText() } ?: return@runCatching null
        JSONObject(txt)
    }.getOrNull()

    private fun postJson(urlStr: String, body: JSONObject, token: String = ""): JSONObject? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 12_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val txt = stream?.bufferedReader()?.use { it.readText() } ?: return@runCatching null
        JSONObject(txt)
    }.getOrNull()
}
