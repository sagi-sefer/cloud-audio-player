# Cloud Audio Player - Architecture Documentation

## 1. Overview & Intention
**Cloud Audio Player** is a personal Android application designed to stream digitized CD collections stored in designated cloud storage folders (**Google Drive** or **Dropbox**). 

The app is built specifically with driving/automotive use cases in mind: featuring high-contrast dark theme UI, large touch targets, bi-directional text support (`TextDirection.ContentOrLtr`) for Hebrew and English metadata, a persistent bottom Mini Player, and an expandable Full-Screen Driving Player.

## 2. Architectural Pattern: Onion (Clean) Architecture
The project strictly adheres to **Clean Architecture (Onion Architecture)** and **Separation of Concerns (SoC)**, divided into four primary packages under `com.example.cloudaudioplayer`:

```
com.example.cloudaudioplayer/
├── domain/            # Pure Kotlin business models & repository interfaces (Zero Android dependencies)
├── application/       # Use Cases orchestrating business logic
├── infrastructure/    # Adapters (Google Drive, Dropbox, Media3 ExoPlayer, Room DB, Hilt DI)
└── presentation/      # Jetpack Compose UI, ViewModels, Themes, and Screen flows
```

---

### Layer Breakdown

#### A. `domain` (Pure Kotlin Layer)
* **Models**:
  * `MediaItem`: Sealed class representing `Folder` (Artists & Albums with `coverArtUrl`) and `AudioTrack` (supporting MP3, WAV, FLAC, AAC, M4A, OGG).
  * `StorageType`: `GOOGLE_DRIVE`, `DROPBOX`.
  * `PlaybackState`: Real-time playback status data class.
  * `AudioFormat`: Supported audio file extensions and mime types.
* **Interfaces (Repositories)**:
  * `CloudStorageRepository`: Cloud querying, stream URL resolution, album cover art discovery.
  * `AudioPlayerRepository`: Playback controls (`play`, `playPlaylist`, `pause`, `resume`, `seekTo`, `skipToNext`, `skipToPrevious`, `stop`).
  * `MetadataRepository`: Local library caching and retrieval (`getAllCachedArtists`, `getCachedAlbumsForArtist`, `getAllCachedAlbums`, `getCachedTracksForAlbum`).

#### B. `application` (Use Cases Layer)
* `GetFolderContentsUseCase`: Fetches folder hierarchies and tracks from cloud repositories with local Room cache fallback.
* `GetFlatAlbumsUseCase`: Fetches all cached albums across artists for flat grid display.
* `StreamAudioUseCase`: Resolves secure cloud stream URLs (injecting Bearer tokens for Google Drive or temporary links for Dropbox) and initiates player queue playback.
* `SyncLibraryCacheUseCase`: Deep-syncs cloud folder trees (`Artist -> Album -> Tracks`) and album covers into local Room database storage.

#### C. `infrastructure` (Adapters & External Drivers Layer)
* **Cloud Storage Adapters**:
  * `GoogleDriveStorageAdapter`: Integrates with Google Drive REST API v3, supports case-insensitive cover art discovery (`cover.jpg`, `cover.png`, `folder.jpg`) and `Authorization: Bearer <token>` injection.
  * `DropboxStorageAdapter`: Integrates with Dropbox SDK, fetches temporary direct stream URLs via `getTemporaryLink`.
* **Audio Player Adapter**:
  * `AudioPlayerService` & `Media3PlayerAdapter`: Wraps AndroidX Media3 `MediaSessionService` + `ExoPlayer` with custom OkHttp HTTP data source factories for authenticated cloud streaming.
* **Database (Room)**:
  * `MediaItemEntity`, `AudioCacheDao` (with `getAllAlbums` query for flat album catalog), `AppDatabase`, `RoomAudioCacheAdapter`.
* **Dependency Injection (Hilt)**:
  * `DatabaseModule`, `StorageModule`, `PlayerModule`, `NetworkModule`.

#### D. `presentation` (UI & Jetpack Compose Layer)
* **Theme**: Material 3 Dark Theme optimized for driving (`CloudAudioPlayerTheme`, `Color.kt`, `Type.kt`).
* **Components**:
  * `BidiText`: Text wrapper enforcing `TextDirection.ContentOrLtr` for clean English and Hebrew rendering.
  * `DrivingControls`, `MiniPlayer`, `FullScreenPlayer`: Car-friendly playback UI.
* **Screens & Navigation**:
  * Dual View Mode Navigation from Main Screen Tabs:
    1. **Top Header**: App title (`CD COLLECTION`), cloud provider badge (`Drive`/`Dropbox`), `Sync` action button, and `Settings` action button.
    2. **Navigation Tabs**: Next-level tab row (`ARTISTS` tab for hierarchical browse via `ArtistListScreen`, `ALBUMS` tab for global flat album grid).
    3. **Hierarchy**: `Main Screen (Artists / Albums Tabs)` -> `AlbumGridScreen` -> `TrackListScreen`.
  * `CloudAuthDialog` & `CloudFolderPickerDialog`: Cloud account authentication and folder browser picker.
  * `MainViewModel`: Coordinates state, tree navigation, view modes, authentication, and playback.
