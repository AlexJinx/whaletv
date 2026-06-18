package com.jing.whaletv.core

import com.jing.whaletv.BuildConfig

object AppConstants {
    const val APP_NAME = "鲸鱼TV"
    const val IPTV_ORG_PLAYLIST_BASE_URL = "https://iptv-org.github.io/iptv"
    const val PRIMARY_PLAYLIST_URL = "https://iptv-org.github.io/iptv/index.m3u"
    const val DEFAULT_REFRESH_INTERVAL_HOURS = 12
    const val HTTP_TIMEOUT_SECONDS = 12L

    val GITEE_MIRROR_BASE_URL: String = BuildConfig.WHALETV_GITEE_MIRROR_BASE_URL.trimEnd('/')

    val remoteDataSources: List<RemoteDataSource>
        get() = buildList {
            if (GITEE_MIRROR_BASE_URL.isNotBlank()) {
                add(
                    RemoteDataSource(
                        id = "gitee",
                        label = "Gitee raw 镜像",
                        playlistBaseUrl = "$GITEE_MIRROR_BASE_URL/iptv",
                    ),
                )
            }
            add(
                RemoteDataSource(
                    id = "iptv_org",
                    label = "iptv-org 官方源",
                    playlistBaseUrl = IPTV_ORG_PLAYLIST_BASE_URL,
                ),
            )
        }

    fun officialPlaylistUrl(path: String): String = "$IPTV_ORG_PLAYLIST_BASE_URL/$path"

    fun playlistUrls(path: String): List<RemoteUrl> {
        return remoteDataSources.map { source ->
            RemoteUrl(label = source.label, url = "${source.playlistBaseUrl}/$path")
        }
    }

}

data class RemoteDataSource(
    val id: String,
    val label: String,
    val playlistBaseUrl: String,
)

data class RemoteUrl(
    val label: String,
    val url: String,
)
