package com.jing.whaletv.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.jing.whaletv.data.model.Program
import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream

@Entity(
    tableName = "channels",
    indices = [
        Index("priority"),
        Index("groupTitle"),
        Index("lastWatchedAt"),
    ],
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String?,
    val groupTitle: String,
    val priority: Int,
    val isFavorite: Boolean = false,
    val lastWatchedAt: Long? = null,
    val isAvailable: Boolean = true,
    val updatedAt: Long,
)

@Entity(
    tableName = "streams",
    primaryKeys = ["channelId", "url"],
    indices = [Index("channelId"), Index("healthStatus")],
)
data class StreamEntity(
    val channelId: String,
    val url: String,
    val quality: String?,
    val label: String?,
    val referrer: String?,
    val userAgent: String?,
    val healthStatus: String = StreamHealth.UNKNOWN.name,
    val failureCount: Int = 0,
    val lastFailureAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "programs",
    primaryKeys = ["channelId", "startAt", "title"],
    indices = [Index("channelId"), Index("startAt"), Index("endAt")],
)
data class ProgramEntity(
    val channelId: String,
    val title: String,
    val startAt: Long,
    val endAt: Long,
    val description: String?,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val key: String,
    val value: String?,
)

data class ChannelWithStreams(
    @Embedded val channel: ChannelEntity,
    @Relation(parentColumn = "id", entityColumn = "channelId")
    val streams: List<StreamEntity>,
)

fun ChannelWithStreams.toDomain(now: Long, programs: List<ProgramEntity>): TvChannel {
    val orderedPrograms = programs.sortedWith(compareBy({ it.startAt }, { it.endAt }))
    val currentEntity = orderedPrograms.lastOrNull { it.startAt <= now && it.endAt > now }
    val nextEntity = orderedPrograms.firstOrNull { it.startAt > now }
    val schedulePrograms = buildList {
        currentEntity?.let(::add)
        addAll(orderedPrograms.filter { it.startAt > now })
    }
        .take(CHANNEL_LIST_SCHEDULE_PROGRAM_LIMIT)
        .map { it.toDomain() }

    return TvChannel(
        id = channel.id,
        name = channel.name,
        logoUrl = channel.logoUrl,
        groupTitle = channel.groupTitle,
        priority = channel.priority,
        isFavorite = channel.isFavorite,
        lastWatchedAt = channel.lastWatchedAt,
        isAvailable = channel.isAvailable,
        streams = streams.sortedWith(compareBy<StreamEntity> { it.sortOrder }.thenBy { it.url }).map { it.toDomain() },
        currentProgram = currentEntity?.toDomain(),
        nextProgram = nextEntity?.toDomain(),
        schedulePrograms = schedulePrograms,
    )
}

fun StreamEntity.toDomain(): TvStream = TvStream(
    channelId = channelId,
    url = url,
    quality = quality,
    label = label,
    referrer = referrer,
    userAgent = userAgent,
    healthStatus = runCatching { StreamHealth.valueOf(healthStatus) }.getOrDefault(StreamHealth.UNKNOWN),
    failureCount = failureCount,
    lastFailureAt = lastFailureAt,
    lastSuccessAt = lastSuccessAt,
    sortOrder = sortOrder,
)

fun ProgramEntity.toDomain(): Program = Program(
    channelId = channelId,
    title = title,
    startAt = startAt,
    endAt = endAt,
    description = description,
)

internal const val CHANNEL_LIST_SCHEDULE_PROGRAM_LIMIT = 6
