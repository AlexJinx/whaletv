package com.jing.whaletv.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
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
    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting playlist sync. attempt=${runAttemptCount + 1}")
            container().channelRepository.syncPlaylists()
            Log.i(TAG, "Playlist sync finished.")
            Result.success()
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val shouldRetry = runAttemptCount < 3
            Log.e(TAG, "Playlist sync failed. retry=$shouldRetry reason=${error.message}", error)
            if (shouldRetry) Result.retry() else Result.failure()
        }
    }
}

class EpgSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting EPG sync. attempt=${runAttemptCount + 1}")
            container().channelRepository.syncEpg()
            Log.i(TAG, "EPG sync finished.")
            Result.success()
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val shouldRetry = runAttemptCount < 2
            Log.e(TAG, "EPG sync failed. retry=$shouldRetry reason=${error.message}", error)
            if (shouldRetry) Result.retry() else Result.failure()
        }
    }
}

class FullPlaylistBackfillWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting full playlist backfill. attempt=${runAttemptCount + 1}")
            val didBackfill = container().channelRepository.backfillAllPlaylistsIfNeeded()
            if (didBackfill) {
                container().channelRepository.syncEpg()
                Log.i(TAG, "Full playlist backfill finished.")
            } else {
                Log.i(TAG, "Full playlist backfill skipped for all-channel scope.")
            }
            Result.success()
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val shouldRetry = runAttemptCount < 2
            Log.e(TAG, "Full playlist backfill failed. retry=$shouldRetry reason=${error.message}", error)
            if (shouldRetry) Result.retry() else Result.failure()
        }
    }
}

object SyncScheduler {
    fun schedulePeriodic(context: Context, intervalHours: Int) {
        val workManager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val interval = intervalHours.coerceIn(1, 72).toLong()

        val playlistRequest = PeriodicWorkRequestBuilder<PlaylistSyncWorker>(interval, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()
        val epgRequest = PeriodicWorkRequestBuilder<EpgSyncWorker>(interval, TimeUnit.HOURS)
            .setConstraints(constraints)
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
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
            .build()
        val epgRequest = OneTimeWorkRequestBuilder<EpgSyncWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()
        val fullBackfillRequest = OneTimeWorkRequestBuilder<FullPlaylistBackfillWorker>()
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

    private const val PLAYLIST_PERIODIC = "playlist_periodic_sync"
    private const val EPG_PERIODIC = "epg_periodic_sync"
    private const val PLAYLIST_IMMEDIATE = "playlist_immediate_sync"
    private const val PLAYLIST_FULL_BACKFILL = "playlist_full_backfill_sync"
}

private const val TAG = "WhaleTvSync"

private fun CoroutineWorker.container() = (applicationContext as WhaleTvApp).container
