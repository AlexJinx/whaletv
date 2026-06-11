package com.jing.whaletv.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Transaction
    @Query("SELECT * FROM channels ORDER BY priority ASC, name ASC")
    fun observeChannelsWithStreams(): Flow<List<ChannelWithStreams>>

    @Transaction
    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannelWithStreams(channelId: String): ChannelWithStreams?

    @Query("SELECT * FROM channels")
    suspend fun getAllChannels(): List<ChannelEntity>

    @Query("SELECT * FROM streams")
    suspend fun getAllStreams(): List<StreamEntity>

    @Query("SELECT COUNT(*) FROM channels WHERE id IN (SELECT DISTINCT channelId FROM streams)")
    suspend fun countChannelsWithStreams(): Int

    @Upsert
    suspend fun upsertChannels(channels: List<ChannelEntity>)

    @Upsert
    suspend fun upsertStreams(streams: List<StreamEntity>)

    @Query("DELETE FROM streams WHERE channelId IN (:channelIds)")
    suspend fun deleteStreamsForChannels(channelIds: List<String>)

    @Query("UPDATE channels SET isAvailable = 0 WHERE id NOT IN (:freshIds)")
    suspend fun markUnavailableExcept(freshIds: List<String>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun setFavorite(channelId: String, isFavorite: Boolean)

    @Query("UPDATE channels SET lastWatchedAt = :watchedAt WHERE id = :channelId")
    suspend fun markWatched(channelId: String, watchedAt: Long)

    @Query("SELECT * FROM streams WHERE channelId = :channelId AND url = :url LIMIT 1")
    suspend fun getStream(channelId: String, url: String): StreamEntity?

    @Query(
        "SELECT COUNT(*) FROM streams WHERE channelId = :channelId " +
            "AND (healthStatus = :healthyStatus OR healthStatus = :unknownStatus)",
    )
    suspend fun countPlayableStreams(
        channelId: String,
        healthyStatus: String,
        unknownStatus: String,
    ): Int

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

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Query("DELETE FROM streams")
    suspend fun clearStreams()
}

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs WHERE channelId IN (:channelIds) ORDER BY channelId ASC, startAt ASC, endAt ASC")
    fun observePrograms(channelIds: List<String>): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs ORDER BY channelId ASC, startAt ASC, endAt ASC")
    fun observeAllPrograms(): Flow<List<ProgramEntity>>

    @Upsert
    suspend fun upsertPrograms(programs: List<ProgramEntity>)

    @Query("DELETE FROM programs WHERE endAt < :cutoff")
    suspend fun deleteProgramsEndedBefore(cutoff: Long)

    @Query("DELETE FROM programs")
    suspend fun clearPrograms()
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state")
    fun observeAll(): Flow<List<SyncStateEntity>>

    @Query("SELECT value FROM sync_state WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Upsert
    suspend fun setValue(state: SyncStateEntity)

    @Query("DELETE FROM sync_state")
    suspend fun clear()
}
