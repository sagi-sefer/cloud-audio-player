# Cloud Audio Player - Development Notes & Guide

## 1. Cloud Authentication & Token Setup
* **Google Drive**:
  * Users can paste an OAuth Bearer Token generated via the [Google OAuth 2.0 Playground](https://developers.google.com/oauthplayground/) with the `https://www.googleapis.com/auth/drive.readonly` scope.
  * Alternatively, users can browse cloud folders directly using the built-in **Cloud Folder Picker**.
* **Dropbox**:
  * Developers can configure their Dropbox App Key once in `local.properties` (`DROPBOX_APP_KEY=...`), which is securely injected at compile-time via Gradle `BuildConfig.DROPBOX_APP_KEY`. This allows end users to sign in directly from inside the app using the **"Sign in with Dropbox"** button out-of-the-box without needing to create or enter an app client ID.
  * Launches an in-app OAuth authorization flow (supporting Google/Gmail account login) and automatically captures the access token via a deep link (`cloudaudioplayer://oauth`).
  * Alternatively, users can paste a Dropbox Access Token manually in the settings dialog.
* **Authentication State & Sign-Out**:
  * Auth tokens, storage types, and root folder paths are persisted across app sessions via `SharedPreferences`.
  * `CloudAuthDialog` provides dynamic Sign-In / Sign-Out buttons that toggle based on the user's active login state. Signing out clears stored credentials and flushes local Room database cache.

## 2. Key Technical Decisions & Solved Issues
* **Coil Authentication for Drive Images**:
  * Google Drive media URLs (`https://www.googleapis.com/drive/v3/files/{id}?alt=media`) require an `Authorization: Bearer <token>` header.
  * Solved via a custom `OkHttpClient` interceptor (`NetworkModule` & `AuthTokenHolder`) that injects the active bearer token into all `googleapis.com` image requests made by Coil.
* **Case-Insensitive Cover Art Discovery**:
  * Album cover art (`cover.jpg`, `cover.png`, `folder.jpg`, `album.jpg`) is searched case-insensitively across Google Drive and Dropbox folders.
* **Local Catalog & Dual View Modes**:
  * Cloud storage maintains a two-level hierarchy (`Artist Folder -> Album Folder -> Audio Files`). At sync time (`SyncLibraryCacheUseCase`), the app parses folder/file names to build a robust local Room catalog.
  * The app provides two display options: (1) Navigate by Artist ➔ Albums, and (2) Flat Albums List (album grid across all artists), allowing instant offline navigation with cover art.
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
