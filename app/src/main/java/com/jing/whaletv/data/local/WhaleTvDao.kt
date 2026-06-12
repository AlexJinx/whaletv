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

    @Query("UPDATE streams SET healthStatus = :unknownStatus WHERE healthStatus = :unhealthyStatus")
    suspend fun resetUnhealthyStreams(unhealthyStatus: String, unknownStatus: String)

    @Query("UPDATE channels SET isAvailable = 1 WHERE id IN (SELECT DISTINCT channelId FROM streams)")
    suspend fun markChannelsWithStreamsAvailable()
}

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs ORDER BY channelId ASC, startAt ASC, endAt ASC")
    fun observeAllPrograms(): Flow<List<ProgramEntity>>

    @Upsert
    suspend fun upsertPrograms(programs: List<ProgramEntity>)

    @Query("DELETE FROM programs WHERE endAt < :cutoff")
    suspend fun deleteProgramsEndedBefore(cutoff: Long)
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
