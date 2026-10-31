package com.example.cloudaudioplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.presentation.components.FullScreenPlayer
import com.example.cloudaudioplayer.presentation.components.MiniPlayer
import com.example.cloudaudioplayer.presentation.main.MainViewModel
import com.example.cloudaudioplayer.presentation.main.TreeLevel
import com.example.cloudaudioplayer.presentation.screens.AlbumGridScreen
import com.example.cloudaudioplayer.presentation.screens.ArtistListScreen
import com.example.cloudaudioplayer.presentation.screens.CloudAuthDialog
import com.example.cloudaudioplayer.presentation.screens.CloudFolderPickerDialog
import com.example.cloudaudioplayer.presentation.screens.TrackListScreen
import com.example.cloudaudioplayer.presentation.theme.CloudAudioPlayerTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null && account.account != null) {
                    androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
                        .handleGoogleAccount(account.account!!)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }

        handleIntent(intent)

        setContent {
            CloudAudioPlayerTheme {
                MainAppScreen(googleSignInLauncher)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "cloudaudioplayer" && data.host == "oauth") {
            val fragment = data.fragment
            if (fragment != null) {
                for (param in fragment.split("&")) {
                    val parts = param.split("=")
                    if (parts.size == 2 && parts[0] == "access_token") {
                        try {
                            val token = java.net.URLDecoder.decode(parts[1], "UTF-8")
                            androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
                                .handleDropboxToken(token)
                        } catch (_: Exception) {}
                        break
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(
    googleSignInLauncher: ActivityResultLauncher<Intent>,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (playbackState.currentTrack != null && !uiState.isFullScreenPlayerOpen) {
                MiniPlayer(
                    playbackState = playbackState,
                    onPlayPauseToggle = { viewModel.togglePlayPause() },
                    onSkipNext = { viewModel.skipNext() },
                    onClickMiniPlayer = { viewModel.toggleFullScreenPlayer(true) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val level = uiState.currentLevel) {
                is TreeLevel.Artists -> {
                    ArtistListScreen(
                        artists = uiState.artists,
                        storageType = uiState.storageType,
                        isLoading = uiState.isLoading,
                        isSyncing = uiState.isSyncing,
                        onArtistClick = { artist -> viewModel.navigateToAlbums(artist) },
                        onFlatAlbumsClick = { viewModel.loadFlatAlbums() },
                        onSyncClick = { viewModel.syncLibrary() },
                        onSettingsClick = { viewModel.toggleAuthModal(true) }
                    )
                }

                is TreeLevel.Albums -> {
                    AlbumGridScreen(
                        artist = level.artist,
                        albums = uiState.albums,
                        isLoading = uiState.isLoading,
                        onAlbumClick = { album -> viewModel.navigateToTracks(level.artist, album) },
                        onAlbumPlayClick = { album -> viewModel.playAlbum(album) },
                        onBackClick = { viewModel.navigateBack() }
                    )
                }

                is TreeLevel.Tracks -> {
                    TrackListScreen(
                        artist = level.artist,
                        album = level.album,
                        tracks = uiState.tracks,
                        isLoading = uiState.isLoading,
                        currentTrackId = playbackState.currentTrack?.id,
                        onTrackClick = { track -> viewModel.playTrack(track) },
                        onBackClick = { viewModel.navigateBack() }
                    )
                }

                is TreeLevel.FlatAlbums -> {
                    AlbumGridScreen(
                        artist = MediaItem.Folder(id = "flat", name = "LIBRARY", path = ""),
                        albums = uiState.flatAlbums,
                        isLoading = uiState.isLoading,
                        onAlbumClick = { album -> viewModel.navigateToFlatAlbumTracks(album) },
                        onAlbumPlayClick = { album -> viewModel.playFlatAlbum(album) },
                        onBackClick = { viewModel.loadArtists() }
                    )
                }

                is TreeLevel.FlatAlbumTracks -> {
                    TrackListScreen(
                        artist = MediaItem.Folder(id = "flat", name = "Library", path = ""),
                        album = level.album,
                        tracks = uiState.tracks,
                        isLoading = uiState.isLoading,
                        currentTrackId = playbackState.currentTrack?.id,
                        onTrackClick = { track -> viewModel.playTrack(track) },
                        onBackClick = { viewModel.loadFlatAlbums() }
                    )
                }
            }

            if (uiState.isAuthModalOpen) {
                val activity = context as? android.app.Activity
                CloudAuthDialog(
                    currentStorageType = uiState.storageType,
                    currentGoogleAuth = uiState.googleAuthData,
                    currentGoogleRoot = uiState.googleRootFolder,
                    currentDropboxAuth = uiState.dropboxAuthData,
                    currentDropboxRoot = uiState.dropboxRootFolder,
                    onSave = { type, gToken, gRoot, dToken, dRoot ->
                        viewModel.setStorageConfigs(type, gToken, gRoot, dToken, dRoot)
                    },
                    onGoogleSignInClick = {
                        viewModel.startGoogleSignIn(googleSignInLauncher)
                    },
                    onDropboxSignInClick = {
                        if (activity != null) {
                            viewModel.startDropboxSignIn(activity)
                        }
                    },
                    onSignOutClick = {
                        viewModel.signOutActive()
                    },
                    onBrowseFoldersClick = {
                        viewModel.openFolderPicker()
                    },
                    onDismiss = { viewModel.toggleAuthModal(false) }
                )
            }

            if (uiState.isFolderPickerOpen) {
                CloudFolderPickerDialog(
                    subFolders = uiState.pickerSubFolders,
                    folderStack = uiState.pickerFolderStack,
                    storageType = uiState.storageType,
                    onFolderClick = { folder -> viewModel.pushPickerFolder(folder) },
                    onBackClick = { viewModel.popPickerFolder() },
                    onFolderSelected = { folderName -> viewModel.selectPickerRootFolder(folderName) },
                    onDismiss = { viewModel.closeFolderPicker() }
                )
            }

            if (uiState.isFullScreenPlayerOpen) {
                FullScreenPlayer(
                    playbackState = playbackState,
                    onPlayPauseToggle = { viewModel.togglePlayPause() },
                    onSkipPrevious = { viewModel.skipPrevious() },
                    onSkipNext = { viewModel.skipNext() },
                    onSeekTo = { pos -> viewModel.seekTo(pos) },
                    onDismiss = { viewModel.toggleFullScreenPlayer(false) }
                )
            }
        }
    }
}
