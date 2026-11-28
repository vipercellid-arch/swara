// LibraryPlaylistsScreen.kt

package com.mochyy.mochymusic.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.ui.component.CreatePlaylistDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.importer.ImportSource
import com.mochyy.mochymusic.ui.component.LocalMenuState
import com.mochyy.mochymusic.ui.menu.PlaylistMenu
import com.mochyy.mochymusic.viewmodels.LibraryHomeViewModel
import com.mochyy.mochymusic.viewmodels.LibraryShelfKind

/**
 * The library's playlists, or with a [shelf] the Local or Cached tab's: only the playlists that
 * have one of that tab's songs in them. With [source], only the playlists imported from that service.
 */
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    shelf: LibraryShelfKind? = null,
    source: ImportSource? = null,
) {
    if (shelf == null && source == null) {
        LibraryPlaylistsPage(navController)
        return
    }
    val viewModel: LibraryHomeViewModel = hiltViewModel()
    val all by (shelf?.let(viewModel::shelf)?.playlists ?: viewModel.playlists).collectAsStateWithLifecycle()
    val playlistSources by viewModel.playlistSources.collectAsState()
    val playlists = remember(all, playlistSources, source) {
        if (source == null) all else all.filter { playlistSources[it.id] == source }
    }
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalContext.current.resources

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false })
    }
    Box(modifier = modifier.fillMaxSize()) {
    LibraryEntityListScreen(
        title = stringResource(R.string.lib_playlist),
        subtitle = when {
            source != null -> stringResource(R.string.lib_playlists_imported_subtitle, source.label)
            shelf == LibraryShelfKind.Local -> stringResource(R.string.lib_playlists_local_subtitle)
            shelf == LibraryShelfKind.Cached -> stringResource(R.string.lib_playlists_cached_subtitle)
            else -> stringResource(R.string.lib_playlists_all_subtitle)
        },
        searchPlaceholder = stringResource(R.string.lib_playlists_search),
        items = playlists,
        idOf = { it.id },
        nameOf = { it.playlist.name },
        subtitleOf = { resources.getQuantityString(R.plurals.lib_song_count, it.songCount, it.songCount) },
        thumbnailOf = { it.thumbnails.firstOrNull() },
        // the same key Home's cards use, so the playlist opens growing out of its cover
        sharedElementKeyOf = { "playlist-${it.id}" },
        onClick = { playlist -> navController.navigate("local_playlist/${playlist.id}") },
        onLongClick = { playlist ->
            menuState.show {
                PlaylistMenu(
                    playlist = playlist,
                    coroutineScope = coroutineScope,
                    onDismiss = menuState::dismiss,
                    navController = navController,
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        pureBlack = pureBlack,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 104.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable { showCreatePlaylistDialog = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(painter = painterResource(R.drawable.add), contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Buat Playlist", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
    }
}
