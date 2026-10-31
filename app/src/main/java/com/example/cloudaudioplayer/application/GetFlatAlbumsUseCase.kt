package com.example.cloudaudioplayer.application

import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.repository.MetadataRepository
import javax.inject.Inject

class GetFlatAlbumsUseCase @Inject constructor(
    private val metadataRepository: MetadataRepository
) {
    suspend operator fun invoke(): List<MediaItem.Folder> {
        return metadataRepository.getAllCachedAlbums()
    }
}
