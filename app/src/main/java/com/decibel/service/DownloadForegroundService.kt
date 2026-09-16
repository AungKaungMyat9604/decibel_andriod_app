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
import com.decibel.MainActivity
import com.decibel.R
import com.decibel.DecibelApp
import com.decibel.data.DownloadJob
import com.decibel.data.DownloadJobState
import com.decibel.data.DownloadProgressHub
import com.decibel.youtube.VideoFormatOption
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

        val webpageUrl = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val formatUrl = intent.getStringExtra(EXTRA_FORMAT_URL) ?: return START_NOT_STICKY
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Video"
        val uploader = intent.getStringExtra(EXTRA_UPLOADER) ?: "Unknown"
        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: webpageUrl.hashCode().toString()
        val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
        val thumb = intent.getStringExtra(EXTRA_THUMB)
        val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "unknown"
        val format = intent.getStringExtra(EXTRA_FORMAT) ?: "mp4"
        val size = intent.getLongExtra(EXTRA_SIZE, -1L).takeIf { it > 0 }
        val overwrite = intent.getBooleanExtra(EXTRA_OVERWRITE, false)
        val jobId = intent.getStringExtra(EXTRA_JOB_ID) ?: DownloadProgressHub.newJobId()
        val entryId = "${videoId}_video"

        if (!foregroundStarted.getAndSet(true)) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification("Starting download…", 0, indeterminate = true),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            )
        }

        if (workers.containsKey(jobId)) {
            return START_STICKY
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

        val app = application as DecibelApp
        val worker = scope.launch(Dispatchers.IO) {
            semaphore.withPermit {
                val lookup = VideoLookup(
                    id = videoId,
                    title = title,
                    uploader = uploader,
                    durationSeconds = duration,
                    thumbnailUrl = thumb,
                    webpageUrl = webpageUrl,
                    videoFormats = emptyList(),
                    playbackUrl = formatUrl,
                )
                val option = VideoFormatOption(
                    quality = quality,
                    resolution = quality,
                    format = format,
                    url = formatUrl,
                    approxSizeBytes = size,
                )
                runCatching {
                    app.downloadRepository.downloadVideo(lookup, option, jobId, overwrite)
                }
            }
            workers.remove(jobId)
        }
        workers[jobId] = worker
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
                buildNotification(
                    "Downloading ${top.title} — ${top.percent}%$extra",
                    top.percent,
                    indeterminate = top.percent <= 0,
                )
            }
            active.isNotEmpty() -> buildNotification(
                "Queued ${active.size} download(s)…",
                0,
                indeterminate = true,
            )
            jobs.any { it.state == DownloadJobState.Failed && it.state == jobs.lastOrNull()?.state } -> {
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
                    "Downloaded ${ok?.title ?: "file"}",
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
            .setContentTitle("Decibel download")
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
                description = "Background video downloads"
                setSound(null, null)
            },
        )
    }

    companion object {
        const val CHANNEL_ID = "decibel_downloads"
        const val NOTIFICATION_ID = 42
        const val MAX_CONCURRENT = 3
        const val ACTION_CANCEL = "com.decibel.CANCEL_DOWNLOAD"
        const val EXTRA_URL = "url"
        const val EXTRA_FORMAT_URL = "format_url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_UPLOADER = "uploader"
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_THUMB = "thumb"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_FORMAT = "format"
        const val EXTRA_SIZE = "size"
        const val EXTRA_OVERWRITE = "overwrite"
        const val EXTRA_JOB_ID = "job_id"

        fun start(
            context: Context,
            lookup: VideoLookup,
            format: VideoFormatOption,
            overwrite: Boolean = false,
            jobId: String = DownloadProgressHub.newJobId(),
        ) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                putExtra(EXTRA_URL, lookup.webpageUrl)
                putExtra(EXTRA_FORMAT_URL, format.url)
                putExtra(EXTRA_TITLE, lookup.title)
                putExtra(EXTRA_UPLOADER, lookup.uploader)
                putExtra(EXTRA_VIDEO_ID, lookup.id)
                putExtra(EXTRA_DURATION, lookup.durationSeconds)
                putExtra(EXTRA_THUMB, lookup.thumbnailUrl)
                putExtra(EXTRA_QUALITY, format.resolution)
                putExtra(EXTRA_FORMAT, format.format)
                putExtra(EXTRA_SIZE, format.approxSizeBytes ?: -1L)
                putExtra(EXTRA_OVERWRITE, overwrite)
                putExtra(EXTRA_JOB_ID, jobId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
