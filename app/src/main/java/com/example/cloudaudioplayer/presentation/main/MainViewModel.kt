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
import com.example.cloudaudioplayer.BuildConfig
import com.example.cloudaudioplayer.domain.repository.AudioPlayerRepository
import com.example.cloudaudioplayer.domain.repository.CloudStorageRepository
import com.example.cloudaudioplayer.domain.repository.MetadataRepository
import com.example.cloudaudioplayer.infrastructure.storage.AuthTokenHolder
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
    val googleAuthData: String = "",
    val googleRootFolder: String = "Music",
    val dropboxAuthData: String = "",
    val dropboxRootFolder: String = "Music",
    val dropboxAppKey: String = "",
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
) {
    val authData: String
        get() = if (storageType == StorageType.GOOGLE_DRIVE) googleAuthData else dropboxAuthData

    val rootFolder: String
        get() = if (storageType == StorageType.GOOGLE_DRIVE) googleRootFolder else dropboxRootFolder
}

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val getFolderContentsUseCase: GetFolderContentsUseCase,
    private val getFlatAlbumsUseCase: GetFlatAlbumsUseCase,
    private val streamAudioUseCase: StreamAudioUseCase,
    private val syncLibraryCacheUseCase: SyncLibraryCacheUseCase,
    private val cloudStorageRepository: CloudStorageRepository,
    private val audioPlayerRepository: AudioPlayerRepository,
    private val metadataRepository: MetadataRepository
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

        val oldToken = prefs.getString("auth_data", "") ?: ""
        val oldRoot = prefs.getString("root_folder", "Music") ?: "Music"

        val googleToken = prefs.getString("google_auth_data", if (savedType == StorageType.GOOGLE_DRIVE) oldToken else "") ?: ""
        val googleRoot = prefs.getString("google_root_folder", if (savedType == StorageType.GOOGLE_DRIVE) oldRoot else "Music") ?: "Music"
        val dropboxToken = prefs.getString("dropbox_auth_data", if (savedType == StorageType.DROPBOX) oldToken else "") ?: ""
        val dropboxRoot = prefs.getString("dropbox_root_folder", if (savedType == StorageType.DROPBOX) oldRoot else "Music") ?: "Music"

        val savedAppKey = prefs.getString("dropbox_app_key", "") ?: ""

        val activeToken = if (savedType == StorageType.GOOGLE_DRIVE) googleToken else dropboxToken
        AuthTokenHolder.currentToken = activeToken

        _uiState.update {
            it.copy(
                storageType = savedType,
                googleAuthData = googleToken,
                googleRootFolder = googleRoot,
                dropboxAuthData = dropboxToken,
                dropboxRootFolder = dropboxRoot,
                dropboxAppKey = savedAppKey
            )
        }
    }

    private fun saveConfig() {
        val state = _uiState.value
        prefs.edit().apply {
            putString("storage_type", state.storageType.name)
            putString("google_auth_data", state.googleAuthData)
            putString("google_root_folder", state.googleRootFolder)
            putString("dropbox_auth_data", state.dropboxAuthData)
            putString("dropbox_root_folder", state.dropboxRootFolder)
            putString("dropbox_app_key", state.dropboxAppKey)
            apply()
        }
    }

    fun setStorageConfigs(
        type: StorageType,
        googleToken: String,
        googleRoot: String,
        dropboxToken: String,
        dropboxRoot: String
    ) {
        _uiState.update {
            it.copy(
                storageType = type,
                googleAuthData = googleToken,
                googleRootFolder = googleRoot,
                dropboxAuthData = dropboxToken,
                dropboxRootFolder = dropboxRoot,
                isAuthModalOpen = false
            )
        }
        AuthTokenHolder.currentToken = if (type == StorageType.GOOGLE_DRIVE) googleToken else dropboxToken
        saveConfig()
        loadArtists(forceRefresh = true)
    }

    fun setStorageConfig(type: StorageType, token: String, rootFolder: String) {
        _uiState.update { state ->
            val gAuth = if (type == StorageType.GOOGLE_DRIVE) token else state.googleAuthData
            val gRoot = if (type == StorageType.GOOGLE_DRIVE) rootFolder else state.googleRootFolder
            val dAuth = if (type == StorageType.DROPBOX) token else state.dropboxAuthData
            val dRoot = if (type == StorageType.DROPBOX) rootFolder else state.dropboxRootFolder
            state.copy(
                storageType = type,
                googleAuthData = gAuth,
                googleRootFolder = gRoot,
                dropboxAuthData = dAuth,
                dropboxRootFolder = dRoot,
                isAuthModalOpen = false
            )
        }
        AuthTokenHolder.currentToken = token
        saveConfig()
        loadArtists(forceRefresh = true)
    }

    fun startDropboxSignIn(activity: android.app.Activity) {
        val keyToUse = BuildConfig.DROPBOX_APP_KEY
        if (keyToUse.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please configure DROPBOX_APP_KEY in local.properties") }
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

    fun signOutProvider(type: StorageType) {
        _uiState.update { state ->
            if (type == StorageType.GOOGLE_DRIVE) {
                state.copy(
                    googleAuthData = "",
                    artists = emptyList(),
                    albums = emptyList(),
                    flatAlbums = emptyList(),
                    tracks = emptyList(),
                    errorMessage = "Signed out of Google Drive"
                )
            } else {
                state.copy(
                    dropboxAuthData = "",
                    artists = emptyList(),
                    albums = emptyList(),
                    flatAlbums = emptyList(),
                    tracks = emptyList(),
                    errorMessage = "Signed out of Dropbox"
                )
            }
        }

        if (type == StorageType.GOOGLE_DRIVE) {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(getApplication<Application>(), gso).signOut()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error signing out of GoogleSignInClient: ${e.message}")
            }
        }

        AuthTokenHolder.currentToken = _uiState.value.authData
        saveConfig()
        viewModelScope.launch {
            metadataRepository.clearCache()
        }
        loadArtists(forceRefresh = true)
    }

    fun signOutActive() {
        signOutProvider(_uiState.value.storageType)
    }

    fun handleDropboxToken(token: String) {
        AuthTokenHolder.currentToken = token
        _uiState.update {
            it.copy(
                storageType = StorageType.DROPBOX,
                dropboxAuthData = token,
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
        _uiState.update { state ->
            if (state.storageType == StorageType.GOOGLE_DRIVE) {
                state.copy(googleRootFolder = resolvedRoot, isFolderPickerOpen = false)
            } else {
                state.copy(dropboxRootFolder = resolvedRoot, isFolderPickerOpen = false)
            }
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
                        googleAuthData = token,
                        isAuthModalOpen = false,
                        errorMessage = "Google Drive Sign-In successful!"
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
                val cachedArtists = metadataRepository.getAllCachedArtists()
                if (cachedArtists.isNotEmpty() && !forceRefresh) {
                    _uiState.update {
                        it.copy(
                            currentLevel = TreeLevel.Artists,
                            artists = cachedArtists,
                            isLoading = false
                        )
                    }
                    return@launch
                }

                val hasGoogle = state.googleAuthData.isNotBlank()
                val hasDropbox = state.dropboxAuthData.isNotBlank()

                if ((cachedArtists.isEmpty() || forceRefresh) && (hasGoogle || hasDropbox)) {
                    metadataRepository.clearCache()
                    if (hasGoogle) {
                        AuthTokenHolder.currentToken = state.googleAuthData
                        syncLibraryCacheUseCase(StorageType.GOOGLE_DRIVE, state.googleAuthData, state.googleRootFolder, false)
                    }
                    if (hasDropbox) {
                        AuthTokenHolder.currentToken = state.dropboxAuthData
                        syncLibraryCacheUseCase(StorageType.DROPBOX, state.dropboxAuthData, state.dropboxRootFolder, false)
                    }
                }

                val artistFolders = metadataRepository.getAllCachedArtists()
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
                val hasGoogle = state.googleAuthData.isNotBlank()
                val hasDropbox = state.dropboxAuthData.isNotBlank()

                if (!hasGoogle && !hasDropbox) {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            errorMessage = "Please sign in to Google Drive or Dropbox first"
                        )
                    }
                    return@launch
                }

                metadataRepository.clearCache()

                if (hasGoogle) {
                    AuthTokenHolder.currentToken = state.googleAuthData
                    syncLibraryCacheUseCase(
                        storageType = StorageType.GOOGLE_DRIVE,
                        authData = state.googleAuthData,
                        rootFolder = state.googleRootFolder,
                        clearCacheFirst = false
                    )
                }

                if (hasDropbox) {
                    AuthTokenHolder.currentToken = state.dropboxAuthData
                    syncLibraryCacheUseCase(
                        storageType = StorageType.DROPBOX,
                        authData = state.dropboxAuthData,
                        rootFolder = state.dropboxRootFolder,
                        clearCacheFirst = false
                    )
                }

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
