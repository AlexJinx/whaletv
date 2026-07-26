package com.jing.whaletv.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jing.whaletv.WhaleTvApp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class PlaylistSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runSyncAttempt(taskName = "Playlist sync", maxAttempts = 3) {
        container().channelRepository.syncPlaylists()
    }
}

class EpgSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runSyncAttempt(taskName = "EPG sync", maxAttempts = 2) {
        container().channelRepository.syncEpg()
    }
}

class FullPlaylistBackfillWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runSyncAttempt(taskName = "Full playlist backfill", maxAttempts = 2) {
        val didBackfill = container().channelRepository.backfillAllPlaylistsIfNeeded()
        if (didBackfill) {
            container().channelRepository.syncEpg()
        } else {
            Log.i(TAG, "Full playlist backfill skipped for all-channel scope.")
        }
    }
}

/** 三个同步 Worker 的共享执行骨架：日志 + 取消透传 + 重试上限。 */
private suspend fun CoroutineWorker.runSyncAttempt(
    taskName: String,
    maxAttempts: Int,
    block: suspend () -> Unit,
): ListenableWorker.Result {
    return try {
        Log.i(TAG, "Starting $taskName. attempt=${runAttemptCount + 1}")
        block()
        Log.i(TAG, "$taskName finished.")
        ListenableWorker.Result.success()
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        val shouldRetry = runAttemptCount < maxAttempts
        Log.e(TAG, "$taskName failed. retry=$shouldRetry reason=${error.message}", error)
        if (shouldRetry) ListenableWorker.Result.retry() else ListenableWorker.Result.failure()
    }
}

object SyncScheduler {
    fun schedulePeriodic(context: Context, intervalHours: Int) {
        val workManager = WorkManager.getInstance(context)
        val interval = intervalHours.coerceIn(1, 72).toLong()

        val playlistRequest = PeriodicWorkRequestBuilder<PlaylistSyncWorker>(interval, TimeUnit.HOURS)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()
        val epgRequest = PeriodicWorkRequestBuilder<EpgSyncWorker>(interval, TimeUnit.HOURS)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PLAYLIST_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            playlistRequest,
        )
        workManager.enqueueUniquePeriodicWork(
            EPG_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            epgRequest,
        )
    }

    fun enqueueImmediate(context: Context) {
        val playlistRequest = OneTimeWorkRequestBuilder<PlaylistSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
            .build()
        val epgRequest = OneTimeWorkRequestBuilder<EpgSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()
        val fullBackfillRequest = OneTimeWorkRequestBuilder<FullPlaylistBackfillWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 8, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork(
                PLAYLIST_IMMEDIATE,
                ExistingWorkPolicy.KEEP,
                playlistRequest,
            )
            .then(epgRequest)
            .then(fullBackfillRequest)
            .enqueue()
    }

    fun enqueueFullBackfill(context: Context) {
        val request = OneTimeWorkRequestBuilder<FullPlaylistBackfillWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 8, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                PLAYLIST_FULL_BACKFILL,
                ExistingWorkPolicy.KEEP,
                request,
            )
    }

    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PLAYLIST_PERIODIC)
        WorkManager.getInstance(context).cancelUniqueWork(EPG_PERIODIC)
    }

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private const val PLAYLIST_PERIODIC = "playlist_periodic_sync"
    private const val EPG_PERIODIC = "epg_periodic_sync"
    private const val PLAYLIST_IMMEDIATE = "playlist_immediate_sync"
    private const val PLAYLIST_FULL_BACKFILL = "playlist_full_backfill_sync"
}

private const val TAG = "WhaleTvSync"

private fun CoroutineWorker.container() = (applicationContext as WhaleTvApp).container
