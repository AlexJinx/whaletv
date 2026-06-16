package com.jing.whaletv.data.model

data class TvChannel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val groupTitle: String,
    val priority: Int,
    val isFavorite: Boolean,
    val lastWatchedAt: Long?,
    val isAvailable: Boolean,
    val streams: List<TvStream>,
    val currentProgram: Program?,
    val nextProgram: Program?,
    val schedulePrograms: List<Program> = emptyList(),
)

data class TvStream(
    val channelId: String,
    val url: String,
    val quality: String?,
    val label: String?,
    val referrer: String?,
    val userAgent: String?,
    val healthStatus: StreamHealth,
    val failureCount: Int,
    val lastFailureAt: Long?,
    val lastSuccessAt: Long?,
    val sortOrder: Int,
)

data class Program(
    val channelId: String,
    val title: String,
    val startAt: Long,
    val endAt: Long,
    val description: String?,
)

data class ParsedChannel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val groupTitle: String,
    val priority: Int,
    val streams: List<ParsedStream>,
)

data class ParsedStream(
    val channelId: String,
    val url: String,
    val quality: String?,
    val label: String?,
    val referrer: String?,
    val userAgent: String?,
    val sortOrder: Int,
)

data class AppSettings(
    val autoRefresh: Boolean = true,
    val refreshIntervalHours: Int = 12,
)

data class SettingsDiagnostics(
    val channelCount: Int = 0,
    val playableChannelCount: Int = 0,
    val streamCount: Int = 0,
    val programCount: Int = 0,
    val favoriteCount: Int = 0,
    val historyCount: Int = 0,
    val unhealthyStreamCount: Int = 0,
)

data class SettingsTestResult(
    val success: Boolean,
    val message: String,
)

enum class StreamHealth {
    UNKNOWN,
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
}

fun TvChannel.isPlayable(): Boolean {
    if (!isAvailable) return false
    return streams.any { it.healthStatus == StreamHealth.HEALTHY || it.healthStatus == StreamHealth.UNKNOWN }
}

data class SyncSummary(
    val playlistLastAttemptAt: Long? = null,
    val playlistLastSuccessAt: Long? = null,
    val playlistLastError: String? = null,
    val epgLastAttemptAt: Long? = null,
    val epgLastSuccessAt: Long? = null,
    val epgLastError: String? = null,
    val discoveredEpgUrl: String? = null,
)
