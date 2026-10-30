package com.example.cloudaudioplayer.domain.model

enum class AudioFormat(val extension: String, val mimeType: String) {
    MP3("mp3", "audio/mpeg"),
    WAV("wav", "audio/wav"),
    FLAC("flac", "audio/flac"),
    AAC("aac", "audio/aac"),
    M4A("m4a", "audio/mp4"),
    OGG("ogg", "audio/ogg"),
    UNKNOWN("", "audio/*");

    companion object {
        fun fromExtension(extOrFileName: String): AudioFormat {
            val cleanExt = extOrFileName.substringAfterLast('.', extOrFileName).lowercase()
            return entries.firstOrNull { it.extension == cleanExt } ?: UNKNOWN
        }

        fun isSupportedAudio(fileName: String): Boolean {
            val format = fromExtension(fileName)
            return format != UNKNOWN
        }
    }
}
