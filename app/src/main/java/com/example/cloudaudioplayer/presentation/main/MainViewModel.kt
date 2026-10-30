package com.example.cloudaudioplayer.presentation.main

import android.app.Application
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudaudioplayer.application.GetFlatAlbumsUseCase
import com.example.cloudaudioplayer.application.GetFolderContentsUseCase
import com.example.cloudaudioplayer.application.StreamAudioUseCase
import com.example.cloudaudioplayer.application.SyncLibraryCacheUseCase
import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.PlaybackState
import com.example.cloudaudioplayer.domain.model.StorageType
import com.example.cloudaudioplayer.domain.repository.AudioPlayerRepository
import com.example.cloudaudioplayer.domain.repository.CloudStorageRepository
import com.example.cloudaudioplayer.infrastructure.storage.AuthTokenHolder
import com.example.cloudaudioplayer.infrastructure.storage.DropboxConfig
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class TreeLevel {
    object Artists : TreeLevel()
    data class Albums(val artist: MediaItem.Folder) : TreeLevel()
    data class Tracks(val artist: MediaItem.Folder, val album: MediaItem.Folder) : TreeLevel()
    object FlatAlbums : TreeLevel()
    data class FlatAlbumTracks(val album: MediaItem.Folder) : TreeLevel()
}

data class MainUiState(
    val currentLevel: TreeLevel = TreeLevel.Artists,
    val storageType: StorageType = StorageType.GOOGLE_DRIVE,
    val authData: String = "",
    val dropboxAppKey: String = "",
    val rootFolder: String = "Music",
    val artists: List<MediaItem.Folder> = emptyList(),
    val albums: List<MediaItem.Folder> = emptyList(),
    val flatAlbums: List<MediaItem.Folder> = emptyList(),
    val tracks: List<MediaItem.AudioTrack> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val isAuthModalOpen: Boolean = false,
    val isFolderPickerOpen: Boolean = false,
    val isFullScreenPlayerOpen: Boolean = false,
    val pickerSubFolders: List<MediaItem.Folder> = emptyList(),
    val pickerFolderStack: List<Pair<String, String>> = listOf(Pair("root", "Cloud Root"))
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val getFolderContentsUseCase: GetFolderContentsUseCase,
    private val getFlatAlbumsUseCase: GetFlatAlbumsUseCase,
    private val streamAudioUseCase: StreamAudioUseCase,
    private val syncLibraryCacheUseCase: SyncLibraryCacheUseCase,
    private val cloudStorageRepository: CloudStorageRepository,
    private val audioPlayerRepository: AudioPlayerRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("cloud_audio_prefs", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = audioPlayerRepository.playbackState

    init {
        loadSavedConfig()
        loadArtists()
    }

    private fun loadSavedConfig() {
        val savedTypeStr = prefs.getString("storage_type", StorageType.GOOGLE_DRIVE.name) ?: StorageType.GOOGLE_DRIVE.name
        val savedType = try { StorageType.valueOf(savedTypeStr) } catch (_: Exception) { StorageType.GOOGLE_DRIVE }
        val savedToken = prefs.getString("auth_data", "") ?: ""
        val savedRoot = prefs.getString("root_folder", "Music") ?: "Music"
        val savedAppKey = prefs.getString("dropbox_app_key", "") ?: ""

        AuthTokenHolder.currentToken = savedToken
        _uiState.update {
            it.copy(
                storageType = savedType,
                authData = savedToken,
                rootFolder = savedRoot,
                dropboxAppKey = savedAppKey
            )
        }
    }

    private fun saveConfig() {
        val state = _uiState.value
        prefs.edit().apply {
            putString("storage_type", state.storageType.name)
            putString("auth_data", state.authData)
            putString("root_folder", state.rootFolder)
            putString("dropbox_app_key", state.dropboxAppKey)
            apply()
        }
    }

    fun setStorageConfig(type: StorageType, token: String, rootFolder: String) {
        AuthTokenHolder.currentToken = token
        _uiState.update {
            it.copy(
                storageType = type,
                authData = token,
                rootFolder = rootFolder,
                isAuthModalOpen = false
            )
        }
        saveConfig()
        loadArtists(forceRefresh = true)
    }

    fun startDropboxSignIn(activity: android.app.Activity, appKey: String) {
        val keyToUse = if (appKey.isNotBlank()) appKey.trim() else DropboxConfig.DEFAULT_APP_KEY
        if (keyToUse.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your Dropbox App Key or configure DropboxConfig.DEFAULT_APP_KEY") }
            return
        }
        _uiState.update { it.copy(dropboxAppKey = keyToUse) }
        saveConfig()
        val authUrl = "https://www.dropbox.com/oauth2/authorize?client_id=$keyToUse&response_type=token&redirect_uri=cloudaudioplayer://oauth&scope=files.metadata.read%20files.content.read"
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(authUrl))
            activity.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Failed to launch Dropbox sign-in: ${e.message}") }
        }
    }

    fun handleDropboxToken(token: String) {
        AuthTokenHolder.currentToken = token
        _uiState.update {
            it.copy(
                storageType = StorageType.DROPBOX,
                authData = token,
                isAuthModalOpen = false,
                errorMessage = "Dropbox Sign-In successful!"
            )
        }
        saveConfig()
        loadArtists(forceRefresh = true)
    }

    fun toggleAuthModal(open: Boolean) {
        _uiState.update { it.copy(isAuthModalOpen = open) }
    }

    fun toggleFullScreenPlayer(open: Boolean) {
        _uiState.update { it.copy(isFullScreenPlayerOpen = open) }
    }

    fun openFolderPicker() {
        _uiState.update {
            it.copy(
                isFolderPickerOpen = true,
                pickerFolderStack = listOf(Pair(if (it.storageType == StorageType.GOOGLE_DRIVE) "root" else "", "Cloud Root"))
            )
        }
        loadPickerFolders(if (_uiState.value.storageType == StorageType.GOOGLE_DRIVE) "root" else "")
    }

    fun closeFolderPicker() {
        _uiState.update { it.copy(isFolderPickerOpen = false) }
    }

    fun loadPickerFolders(folderId: String) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val contents = if (folderId == "root" || folderId.isEmpty()) {
                    cloudStorageRepository.getRootFolders(state.storageType, state.authData, "")
                } else {
                    cloudStorageRepository.getFolderContents(folderId, state.storageType, state.authData)
                        .filterIsInstance<MediaItem.Folder>()
                }
                _uiState.update { it.copy(pickerSubFolders = contents) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load folders: ${e.localizedMessage}") }
            }
        }
    }

    fun pushPickerFolder(folder: MediaItem.Folder) {
        _uiState.update { state ->
            val newStack = state.pickerFolderStack + Pair(folder.id, folder.name)
            state.copy(pickerFolderStack = newStack)
        }
        loadPickerFolders(folder.id)
    }

    fun popPickerFolder() {
        _uiState.update { state ->
            if (state.pickerFolderStack.size > 1) {
                val newStack = state.pickerFolderStack.dropLast(1)
                val targetId = newStack.last().first
                loadPickerFolders(targetId)
                state.copy(pickerFolderStack = newStack)
            } else state
        }
    }

    fun selectPickerRootFolder(folderPath: String) {
        val resolvedRoot = if (folderPath == "Cloud Root" || folderPath == "root" || folderPath.isEmpty()) {
            ""
        } else {
            folderPath
        }
        _uiState.update {
            it.copy(
                rootFolder = resolvedRoot,
                isFolderPickerOpen = false
            )
        }
        saveConfig()
        loadArtists(forceRefresh = true)
    }

    fun startGoogleSignIn(launcher: ActivityResultLauncher<Intent>) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.readonly"))
            .build()
        val client = GoogleSignIn.getClient(getApplication(), gso)
        launcher.launch(client.signInIntent)
    }

    fun handleGoogleAccount(account: android.accounts.Account) {
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    GoogleAuthUtil.getToken(
                        getApplication(),
                        account,
                        "oauth2:https://www.googleapis.com/auth/drive.readonly"
                    )
                }
                AuthTokenHolder.currentToken = token
                _uiState.update {
                    it.copy(
                        storageType = StorageType.GOOGLE_DRIVE,
                        authData = token,
                        isAuthModalOpen = false
                    )
                }
                saveConfig()
                loadArtists(forceRefresh = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Google token error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun loadArtists(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val state = _uiState.value
                AuthTokenHolder.currentToken = state.authData
                val items = getFolderContentsUseCase(
                    folderId = null,
                    storageType = state.storageType,
                    authData = state.authData,
                    rootFolder = state.rootFolder,
                    forceRefresh = forceRefresh
                )
                val artistFolders = items.filterIsInstance<MediaItem.Folder>()
                _uiState.update {
                    it.copy(
                        currentLevel = TreeLevel.Artists,
                        artists = artistFolders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load artists"
                    )
                }
            }
        }
    }

    fun navigateToAlbums(artist: MediaItem.Folder, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentLevel = TreeLevel.Albums(artist),
                    isLoading = true,
                    errorMessage = null
                )
            }
            try {
                val state = _uiState.value
                AuthTokenHolder.currentToken = state.authData
                val items = getFolderContentsUseCase(
                    folderId = artist.id,
                    storageType = state.storageType,
                    authData = state.authData,
                    rootFolder = state.rootFolder,
                    forceRefresh = forceRefresh
                )
                val albumFolders = items.filterIsInstance<MediaItem.Folder>()
                _uiState.update {
                    it.copy(
                        albums = albumFolders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load albums"
                    )
                }
            }
        }
    }

    fun navigateToTracks(
        artist: MediaItem.Folder,
        album: MediaItem.Folder,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentLevel = TreeLevel.Tracks(artist, album),
                    isLoading = true,
                    errorMessage = null
                )
            }
            try {
                val state = _uiState.value
                AuthTokenHolder.currentToken = state.authData
                val items = getFolderContentsUseCase(
                    folderId = album.id,
                    storageType = state.storageType,
                    authData = state.authData,
                    rootFolder = state.rootFolder,
                    forceRefresh = forceRefresh
                )
                val audioTracks = items.filterIsInstance<MediaItem.AudioTrack>().map { track ->
                    if (track.artist == "Artist" || track.artist.isEmpty()) {
                        track.copy(artist = artist.name, album = album.name)
                    } else track
                }
                _uiState.update {
                    it.copy(
                        tracks = audioTracks,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load tracks"
                    )
                }
            }
        }
    }

    fun playAlbum(album: MediaItem.Folder) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                AuthTokenHolder.currentToken = state.authData
                val items = getFolderContentsUseCase(
                    folderId = album.id,
                    storageType = state.storageType,
                    authData = state.authData,
                    rootFolder = state.rootFolder,
                    forceRefresh = false
                )
                val audioTracks = items.filterIsInstance<MediaItem.AudioTrack>()
                if (audioTracks.isNotEmpty()) {
                    _uiState.update { it.copy(tracks = audioTracks) }
                    streamAudioUseCase(
                        track = audioTracks.first(),
                        playlist = audioTracks,
                        authData = state.authData
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to play album: ${e.localizedMessage}") }
            }
        }
    }

    fun loadFlatAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(currentLevel = TreeLevel.FlatAlbums, isLoading = true, errorMessage = null) }
            try {
                val flatAlbums = getFlatAlbumsUseCase()
                _uiState.update {
                    it.copy(
                        flatAlbums = flatAlbums,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load flat albums"
                    )
                }
            }
        }
    }

    fun navigateToFlatAlbumTracks(album: MediaItem.Folder, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentLevel = TreeLevel.FlatAlbumTracks(album),
                    isLoading = true,
                    errorMessage = null
                )
            }
            try {
                val state = _uiState.value
                AuthTokenHolder.currentToken = state.authData
                val items = getFolderContentsUseCase(
                    folderId = album.id,
                    storageType = state.storageType,
                    authData = state.authData,
                    rootFolder = state.rootFolder,
                    forceRefresh = forceRefresh
                )
                val audioTracks = items.filterIsInstance<MediaItem.AudioTrack>().map { track ->
                    if (track.album.isEmpty()) track.copy(album = album.name) else track
                }
                _uiState.update {
                    it.copy(
                        tracks = audioTracks,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load tracks"
                    )
                }
            }
        }
    }

    fun playFlatAlbum(album: MediaItem.Folder) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                AuthTokenHolder.currentToken = state.authData
                val items = getFolderContentsUseCase(
                    folderId = album.id,
                    storageType = state.storageType,
                    authData = state.authData,
                    rootFolder = state.rootFolder,
                    forceRefresh = false
                )
                val audioTracks = items.filterIsInstance<MediaItem.AudioTrack>()
                if (audioTracks.isNotEmpty()) {
                    _uiState.update { it.copy(tracks = audioTracks) }
                    streamAudioUseCase(
                        track = audioTracks.first(),
                        playlist = audioTracks,
                        authData = state.authData
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to play album: ${e.localizedMessage}") }
            }
        }
    }

    fun navigateBack() {
        when (val level = _uiState.value.currentLevel) {
            is TreeLevel.Tracks -> navigateToAlbums(level.artist)
            is TreeLevel.Albums -> loadArtists()
            is TreeLevel.FlatAlbumTracks -> loadFlatAlbums()
            is TreeLevel.FlatAlbums -> loadArtists()
            is TreeLevel.Artists -> {}
        }
    }

    fun playTrack(track: MediaItem.AudioTrack) {
        viewModelScope.launch {
            val state = _uiState.value
            AuthTokenHolder.currentToken = state.authData
            streamAudioUseCase(
                track = track,
                playlist = state.tracks,
                authData = state.authData
            )
        }
    }

    fun togglePlayPause() {
        val current = playbackState.value
        if (current.isPlaying) {
            audioPlayerRepository.pause()
        } else {
            audioPlayerRepository.resume()
        }
    }

    fun skipNext() {
        audioPlayerRepository.skipToNext()
    }

    fun skipPrevious() {
        audioPlayerRepository.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        audioPlayerRepository.seekTo(positionMs)
    }

    fun syncLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            try {
                val state = _uiState.value
                AuthTokenHolder.currentToken = state.authData
                syncLibraryCacheUseCase(state.storageType, state.authData, state.rootFolder)
                _uiState.update { it.copy(isSyncing = false) }
                if (state.currentLevel is TreeLevel.FlatAlbums) {
                    loadFlatAlbums()
                } else {
                    loadArtists(forceRefresh = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = e.localizedMessage ?: "Library sync failed"
                    )
                }
            }
        }
    }
}
