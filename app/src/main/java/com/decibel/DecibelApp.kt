package com.decibel

import android.app.Application
import com.decibel.data.DownloadRepository
import com.decibel.data.LibraryStore
import com.decibel.data.StorageSettings
import com.decibel.data.ThumbnailStore
import com.decibel.player.PlaybackController
import com.decibel.youtube.ExtractorBootstrap

class DecibelApp : Application() {
    lateinit var storageSettings: StorageSettings
        private set
    lateinit var thumbnailStore: ThumbnailStore
        private set
    lateinit var libraryStore: LibraryStore
        private set
    lateinit var downloadRepository: DownloadRepository
        private set
    lateinit var playbackController: PlaybackController
        private set

    override fun onCreate() {
        super.onCreate()
        ExtractorBootstrap.init()
        storageSettings = StorageSettings(this)
        thumbnailStore = ThumbnailStore(this)
        libraryStore = LibraryStore(this, storageSettings, thumbnailStore)
        downloadRepository = DownloadRepository(this, libraryStore)
        playbackController = PlaybackController(this)
    }

    override fun onTerminate() {
        playbackController.release()
        super.onTerminate()
    }
}
