# Cloud Audio Player - Development Notes & Guide

## 1. Cloud Authentication & Token Setup
* **Google Drive**:
  * Configured via Android Google Sign-In SDK. In the Google Cloud Console, create an OAuth client ID with application type **Android**, package name `com.example.cloudaudioplayer`, and your debug/release SHA-1 fingerprint (for local debug, your SHA-1 is `BE:6A:3E:72:0D:4C:25:55:18:E1:62:8E:E0:B1:EB:05:35:88:D1:3C`).
  * Users sign in seamlessly using the in-app **"Sign in with Google Account"** button with zero manual token handling.
  * Users can browse cloud folders directly using the built-in **Cloud Folder Picker**.
* **Dropbox**:
  * Developers can configure their Dropbox App Key once in `local.properties` (`DROPBOX_APP_KEY=...`), which is securely injected at compile-time via Gradle `BuildConfig.DROPBOX_APP_KEY`. This allows end users to sign in directly from inside the app using the **"Sign in with Dropbox"** button out-of-the-box without needing to create or enter an app client ID.
  * Launches an in-app OAuth authorization flow and automatically captures the access token via a deep link (`cloudaudioplayer://oauth`). (Manual token entry for Dropbox has been removed for a streamlined user experience).
* **Authentication State & Sign-Out**:
  * Auth tokens, storage types, and root folder paths are persisted independently for both Google Drive and Dropbox across app sessions via `SharedPreferences`, enabling both cloud providers to be configured and connected in parallel.
  * `CloudAuthDialog` maintains separate configuration states for Google Drive and Dropbox, preventing cross-contamination of root folders or auth tokens when switching between providers.
  * `CloudAuthDialog` provides dynamic Sign-In / Sign-Out buttons that toggle based on each provider's active login state. Signing out clears the active provider's credentials and flushes local Room database cache.

## 2. Key Technical Decisions & Solved Issues
* **Coil Authentication for Drive Images**:
  * Google Drive media URLs (`https://www.googleapis.com/drive/v3/files/{id}?alt=media`) require an `Authorization: Bearer <token>` header.
  * Solved via a custom `OkHttpClient` interceptor (`NetworkModule` & `AuthTokenHolder`) that injects the active bearer token into all `googleapis.com` image requests made by Coil.
* **Case-Insensitive Cover Art Discovery**:
  * Album cover art (`cover.jpg`, `cover.png`, `folder.jpg`, `album.jpg`) is searched case-insensitively across Google Drive and Dropbox folders.
* **Parallel Multi-Cloud Library Sync**:
  * Tapping the **Sync** action in `MainViewModel` concurrently pulls catalog information (artists, albums, tracks, and cover art) from **both** Google Drive and Dropbox (when both accounts are connected in parallel) and merges them into a unified local Room database catalog.
  * Both cloud music collections are displayed together side-by-side on the library grid and artist/album navigation views.
* **Playlist & Queue Management**:
  * `Media3PlayerAdapter` preserves active playlist queues, enabling correct previous/next track navigation in both the Mini Player and Full-Screen Player.
* **Continuous Album & Playlist Playback**:
  * `Media3PlayerAdapter` listens to `Player.STATE_ENDED` events to automatically advance and start playing the next track in the album queue, stopping playback cleanly at the end of the final track without looping.
* **Track Ordering**:
  * Both Google Drive and Dropbox storage adapters sort files and folders alphabetically by name (`01 - ...`, `02 - ...`) to maintain correct CD track order.

## 3. Future Development Roadmap
1. **Offline Caching**: Enhance Room caching to allow full offline playback of downloaded tracks.
2. **Android Auto / Car App Library**: Adapt UI for native Android Auto dashboard integration.
3. **Search & Filter**: Add search bar to quickly find artists, albums, or tracks across the cloud library.
