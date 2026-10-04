package com.decibel

import android.app.Application
import com.decibel.data.AppearancePrefs
import com.decibel.data.DownloadRepository
import com.decibel.data.LibraryPrefs
import com.decibel.data.LibraryStore
import com.decibel.data.StorageSettings
import com.decibel.data.ThumbnailStore
import com.decibel.player.EqController
import com.decibel.player.PlaybackController
import com.decibel.player.PlaybackWaveCapture
import com.decibel.youtube.ExtractorBootstrap

class DecibelApp : Application() {
    lateinit var storageSettings: StorageSettings
        private set
    lateinit var appearancePrefs: AppearancePrefs
        private set
    lateinit var libraryPrefs: LibraryPrefs
        private set
    lateinit var thumbnailStore: ThumbnailStore
        private set
    lateinit var libraryStore: LibraryStore
        private set
    lateinit var downloadRepository: DownloadRepository
        private set
    lateinit var eqController: EqController
        private set
    lateinit var playbackController: PlaybackController
        private set
    lateinit var waveCapture: PlaybackWaveCapture
        private set

    override fun onCreate() {
        super.onCreate()
        ExtractorBootstrap.init()
        storageSettings = StorageSettings(this)
        appearancePrefs = AppearancePrefs(this)
        libraryPrefs = LibraryPrefs(this)
        thumbnailStore = ThumbnailStore(this)
        libraryStore = LibraryStore(this, storageSettings, thumbnailStore)
        downloadRepository = DownloadRepository(this, libraryStore)
        eqController = EqController(this)
        waveCapture = PlaybackWaveCapture()
        playbackController = PlaybackController(this, eqController, waveCapture)
    }

    override fun onTerminate() {
        waveCapture.setActive(false)
        playbackController.release()
        eqController.release()
        super.onTerminate()
    }
}
