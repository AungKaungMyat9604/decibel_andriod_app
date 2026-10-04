package com.decibel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.decibel.DecibelApp
import com.decibel.MainActivity
import com.decibel.R
import com.decibel.data.DownloadJob
import com.decibel.data.DownloadJobState
import com.decibel.data.DownloadProgressHub
import com.decibel.data.SavedVideo
import com.decibel.youtube.DownloadKind
import com.decibel.youtube.MediaDownloadOption
import com.decibel.youtube.VideoLookup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class DownloadForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val workers = ConcurrentHashMap<String, Job>()
    private val semaphore = Semaphore(MAX_CONCURRENT)
    private var progressJob: Job? = null
    private val foregroundStarted = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        progressJob = scope.launch {
            DownloadProgressHub.jobs.collectLatest { jobs ->
                updateNotification(jobs)
                if (jobs.none { it.isActive } && workers.isEmpty() && foregroundStarted.get()) {
                    delay(2_500)
                    if (DownloadProgressHub.jobs.value.none { it.isActive } && workers.isEmpty()) {
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            workers.values.forEach { it.cancel() }
            workers.clear()
            stopSelf()
            return START_NOT_STICKY
        }

        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_DOWNLOAD
        if (!foregroundStarted.getAndSet(true)) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification("Starting…", 0, indeterminate = true),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            )
        }

        val jobId = intent?.getStringExtra(EXTRA_JOB_ID) ?: DownloadProgressHub.newJobId()
        if (workers.containsKey(jobId)) {
            return START_STICKY
        }

        val app = application as DecibelApp

        when (mode) {
            MODE_CONVERT -> {
                val sourceId = intent?.getStringExtra(EXTRA_SOURCE_ID) ?: return START_NOT_STICKY
                val source = app.libraryStore.getById(sourceId) ?: return START_NOT_STICKY
                DownloadProgressHub.enqueue(
                    DownloadJob(
                        jobId = jobId,
                        entryId = sourceId,
                        title = source.title,
                        state = DownloadJobState.Queued,
                    ),
                )
                val worker = scope.launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        runCatching {
                            app.downloadRepository.convertToMp3(source, jobId, overwrite = true)
                        }
                    }
                    workers.remove(jobId)
                }
                workers[jobId] = worker
            }
            else -> {
                val webpageUrl = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val optionId = intent.getStringExtra(EXTRA_OPTION_ID) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Video"
                val uploader = intent.getStringExtra(EXTRA_UPLOADER) ?: "Unknown"
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: webpageUrl.hashCode().toString()
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                val thumb = intent.getStringExtra(EXTRA_THUMB)
                val kindName = intent.getStringExtra(EXTRA_KIND) ?: DownloadKind.PROGRESSIVE.name
                val kind = runCatching { DownloadKind.valueOf(kindName) }.getOrDefault(DownloadKind.PROGRESSIVE)
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "Download"
                val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
                val audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL)
                val videoExt = intent.getStringExtra(EXTRA_VIDEO_EXT) ?: "mp4"
                val audioExt = intent.getStringExtra(EXTRA_AUDIO_EXT) ?: "m4a"
                val height = intent.getIntExtra(EXTRA_HEIGHT, 0)
                val size = intent.getLongExtra(EXTRA_SIZE, -1L).takeIf { it > 0 }
                val overwrite = intent.getBooleanExtra(EXTRA_OVERWRITE, false)
                val entryId = when (kind) {
                    DownloadKind.AUDIO_MP3 -> "${videoId}_audio"
                    else -> "${videoId}_video"
                }

                DownloadProgressHub.enqueue(
                    DownloadJob(
                        jobId = jobId,
                        entryId = entryId,
                        title = title,
                        state = DownloadJobState.Queued,
                        totalBytes = size,
                    ),
                )

                val seedLookup = VideoLookup(
                    id = videoId,
                    title = title,
                    uploader = uploader,
                    durationSeconds = duration,
                    thumbnailUrl = thumb,
                    webpageUrl = webpageUrl,
                    videoFormats = emptyList(),
                    downloadOptions = emptyList(),
                    playbackUrl = videoUrl ?: audioUrl.orEmpty(),
                )
                val seedOption = MediaDownloadOption(
                    id = optionId,
                    kind = kind,
                    label = label,
                    videoUrl = videoUrl,
                    audioUrl = audioUrl,
                    videoExt = videoExt,
                    audioExt = audioExt,
                    height = height,
                    approxSizeBytes = size,
                )

                val worker = scope.launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        runCatching {
                            app.downloadRepository.downloadOption(
                                seedLookup,
                                seedOption,
                                jobId,
                                overwrite,
                            )
                        }
                    }
                    workers.remove(jobId)
                }
                workers[jobId] = worker
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        workers.values.forEach { it.cancel() }
        workers.clear()
        progressJob?.cancel()
        scope.cancel()
        foregroundStarted.set(false)
        super.onDestroy()
    }

    private fun updateNotification(jobs: List<DownloadJob>) {
        val active = jobs.filter { it.isActive }
        val running = active.filter { it.state == DownloadJobState.Running }
        val notification = when {
            running.isNotEmpty() -> {
                val top = running.maxByOrNull { it.percent } ?: running.first()
                val extra = if (active.size > 1) " (+${active.size - 1} more)" else ""
                val phase = top.message?.let { " — $it" }.orEmpty()
                buildNotification(
                    "${top.title}$phase — ${top.percent}%$extra",
                    top.percent,
                    indeterminate = top.percent <= 0,
                )
            }
            active.isNotEmpty() -> buildNotification(
                "Queued ${active.size} job(s)…",
                0,
                indeterminate = true,
            )
            jobs.any { it.state == DownloadJobState.Failed } -> {
                val failed = jobs.lastOrNull { it.state == DownloadJobState.Failed }
                buildNotification(
                    failed?.message ?: "Download failed",
                    0,
                    ongoing = false,
                )
            }
            jobs.any { it.state == DownloadJobState.Success } -> {
                val ok = jobs.lastOrNull { it.state == DownloadJobState.Success }
                buildNotification(
                    "Saved ${ok?.title ?: "file"}",
                    100,
                    ongoing = false,
                )
            }
            else -> buildNotification("Downloads", 0, indeterminate = true, ongoing = false)
        }
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        content: String,
        percent: Int,
        indeterminate: Boolean = false,
        ongoing: Boolean = true,
    ): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Decibel")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing && percent < 100)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(100, percent.coerceIn(0, 100), indeterminate)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Background downloads and conversions"
                setSound(null, null)
            },
        )
    }

    companion object {
        const val CHANNEL_ID = "decibel_downloads"
        const val NOTIFICATION_ID = 42
        const val MAX_CONCURRENT = 2
        const val ACTION_CANCEL = "com.decibel.CANCEL_DOWNLOAD"
        const val MODE_DOWNLOAD = "download"
        const val MODE_CONVERT = "convert"

        const val EXTRA_MODE = "mode"
        const val EXTRA_URL = "url"
        const val EXTRA_OPTION_ID = "option_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_UPLOADER = "uploader"
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_THUMB = "thumb"
        const val EXTRA_KIND = "kind"
        const val EXTRA_LABEL = "label"
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_AUDIO_URL = "audio_url"
        const val EXTRA_VIDEO_EXT = "video_ext"
        const val EXTRA_AUDIO_EXT = "audio_ext"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_SIZE = "size"
        const val EXTRA_OVERWRITE = "overwrite"
        const val EXTRA_JOB_ID = "job_id"
        const val EXTRA_SOURCE_ID = "source_id"

        fun startDownload(
            context: Context,
            lookup: VideoLookup,
            option: MediaDownloadOption,
            overwrite: Boolean = false,
            jobId: String = DownloadProgressHub.newJobId(),
        ) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                putExtra(EXTRA_MODE, MODE_DOWNLOAD)
                putExtra(EXTRA_URL, lookup.webpageUrl)
                putExtra(EXTRA_OPTION_ID, option.id)
                putExtra(EXTRA_TITLE, lookup.title)
                putExtra(EXTRA_UPLOADER, lookup.uploader)
                putExtra(EXTRA_VIDEO_ID, lookup.id)
                putExtra(EXTRA_DURATION, lookup.durationSeconds)
                putExtra(EXTRA_THUMB, lookup.thumbnailUrl)
                putExtra(EXTRA_KIND, option.kind.name)
                putExtra(EXTRA_LABEL, option.label)
                putExtra(EXTRA_VIDEO_URL, option.videoUrl)
                putExtra(EXTRA_AUDIO_URL, option.audioUrl)
                putExtra(EXTRA_VIDEO_EXT, option.videoExt)
                putExtra(EXTRA_AUDIO_EXT, option.audioExt)
                putExtra(EXTRA_HEIGHT, option.height)
                putExtra(EXTRA_SIZE, option.approxSizeBytes ?: -1L)
                putExtra(EXTRA_OVERWRITE, overwrite)
                putExtra(EXTRA_JOB_ID, jobId)
            }
            startService(context, intent)
        }

        fun startConvert(
            context: Context,
            source: SavedVideo,
            jobId: String = DownloadProgressHub.newJobId(),
        ) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                putExtra(EXTRA_MODE, MODE_CONVERT)
                putExtra(EXTRA_SOURCE_ID, source.id)
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_TITLE, source.title)
            }
            startService(context, intent)
        }

        private fun startService(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
