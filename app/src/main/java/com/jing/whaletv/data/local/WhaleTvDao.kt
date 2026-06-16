package com.jing.whaletv.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Transaction
    @Query(
        "SELECT * FROM channels WHERE isAvailable = 1 " +
            "AND id IN (SELECT DISTINCT channelId FROM streams " +
            "WHERE healthStatus = :healthyStatus OR healthStatus = :unknownStatus) " +
            "ORDER BY priority ASC, name ASC",
    )
    fun observePlayableChannelsWithStreams(
        healthyStatus: String,
        unknownStatus: String,
    ): Flow<List<ChannelWithStreams>>

    @Query("SELECT * FROM channels")
    suspend fun getAllChannels(): List<ChannelEntity>

    @Query("SELECT * FROM streams")
    suspend fun getAllStreams(): List<StreamEntity>

    @Query(
        "SELECT streams.* FROM streams INNER JOIN channels ON streams.channelId = channels.id " +
            "WHERE channels.isAvailable = 1 " +
            "AND (streams.healthStatus = :healthyStatus OR streams.healthStatus = :unknownStatus) " +
            "ORDER BY channels.isFavorite DESC, channels.lastWatchedAt DESC, channels.priority ASC, streams.sortOrder ASC " +
            "LIMIT :limit",
    )
    suspend fun getStartupProbeStreams(
        healthyStatus: String,
        unknownStatus: String,
        limit: Int,
    ): List<StreamEntity>

    @Query(
        "SELECT COUNT(*) FROM channels WHERE isAvailable = 1 " +
            "AND id IN (SELECT DISTINCT channelId FROM streams " +
            "WHERE healthStatus = :healthyStatus OR healthStatus = :unknownStatus)",
    )
    suspend fun countPlayableChannels(
        healthyStatus: String,
        unknownStatus: String,
    ): Int

    @Query("SELECT COUNT(*) FROM channels")
    fun observeChannelCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM channels WHERE isAvailable = 1 " +
            "AND id IN (SELECT DISTINCT channelId FROM streams " +
            "WHERE healthStatus = :healthyStatus OR healthStatus = :unknownStatus)",
    )
    fun observePlayableChannelCount(
        healthyStatus: String,
        unknownStatus: String,
    ): Flow<Int>

    @Query("SELECT COUNT(*) FROM streams")
    fun observeStreamCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM streams WHERE healthStatus = :unhealthyStatus")
    fun observeUnhealthyStreamCount(unhealthyStatus: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM channels WHERE isFavorite = 1")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM channels WHERE lastWatchedAt IS NOT NULL")
    fun observeHistoryCount(): Flow<Int>

    @Upsert
    suspend fun upsertChannels(channels: List<ChannelEntity>)

    @Upsert
    suspend fun upsertStreams(streams: List<StreamEntity>)

    @Query("DELETE FROM streams WHERE channelId IN (:channelIds)")
    suspend fun deleteStreamsForChannels(channelIds: List<String>)

    @Query("DELETE FROM channels WHERE id IN (:channelIds)")
    suspend fun deleteChannels(channelIds: List<String>)

    @Query("SELECT * FROM streams WHERE channelId = :channelId AND url = :url LIMIT 1")
    suspend fun getStream(channelId: String, url: String): StreamEntity?

    @Query(
        "UPDATE streams SET healthStatus = :healthStatus, failureCount = :failureCount, " +
            "lastFailureAt = :failedAt WHERE channelId = :channelId AND url = :url",
    )
    suspend fun updateStreamFailure(
        channelId: String,
        url: String,
        healthStatus: String,
        failureCount: Int,
        failedAt: Long,
    )

    @Query(
        "UPDATE streams SET healthStatus = :healthStatus, failureCount = 0, " +
            "lastSuccessAt = :successAt WHERE channelId = :channelId AND url = :url",
    )
    suspend fun updateStreamSuccess(channelId: String, url: String, healthStatus: String, successAt: Long)

    @Query("UPDATE channels SET isAvailable = :isAvailable WHERE id = :channelId")
    suspend fun setChannelAvailability(channelId: String, isAvailable: Boolean)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun setChannelFavorite(channelId: String, isFavorite: Boolean)

    @Query("UPDATE channels SET lastWatchedAt = :watchedAt WHERE id = :channelId")
    suspend fun setChannelLastWatchedAt(channelId: String, watchedAt: Long)

    @Query("UPDATE streams SET healthStatus = :unknownStatus WHERE healthStatus = :unhealthyStatus")
    suspend fun resetUnhealthyStreams(unhealthyStatus: String, unknownStatus: String)

    @Query(
        "UPDATE streams SET healthStatus = :unknownStatus, failureCount = 0, " +
            "lastFailureAt = NULL, lastSuccessAt = NULL",
    )
    suspend fun resetAllStreamHealth(unknownStatus: String)

    @Query("UPDATE channels SET isAvailable = 1 WHERE id IN (SELECT DISTINCT channelId FROM streams)")
    suspend fun markChannelsWithStreamsAvailable()

    @Query("UPDATE channels SET lastWatchedAt = NULL")
    suspend fun clearWatchHistory()
}

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs ORDER BY channelId ASC, startAt ASC, endAt ASC")
    fun observeAllPrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT COUNT(*) FROM programs")
    fun observeProgramCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT channelId) FROM programs")
    fun observeProgramChannelCount(): Flow<Int>

    @Query("SELECT channelId FROM programs GROUP BY channelId ORDER BY channelId ASC LIMIT :limit")
    fun observeProgramChannelSamples(limit: Int): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM programs")
    suspend fun countPrograms(): Int

    @Upsert
    suspend fun upsertPrograms(programs: List<ProgramEntity>)

    @Query("DELETE FROM programs WHERE endAt < :cutoff")
    suspend fun deleteProgramsEndedBefore(cutoff: Long)

    @Query("DELETE FROM programs")
    suspend fun deleteAllPrograms()
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state")
    fun observeAll(): Flow<List<SyncStateEntity>>

    @Query("SELECT value FROM sync_state WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Upsert
    suspend fun setValue(state: SyncStateEntity)
}
