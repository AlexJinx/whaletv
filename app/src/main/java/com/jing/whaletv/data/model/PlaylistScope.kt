package com.jing.whaletv.data.model

import com.jing.whaletv.core.AppConstants

enum class PlaylistScope(
    val id: String,
    val label: String,
    val description: String,
    val playlistUrl: String,
) {
    ALL(
        id = "all",
        label = "全部频道",
        description = "iptv-org 全球公开频道索引",
        playlistUrl = AppConstants.PRIMARY_PLAYLIST_URL,
    ),
    COUNTRY_CN(
        id = "country_cn",
        label = "中国频道",
        description = "按国家筛选：中国",
        playlistUrl = "${AppConstants.IPTV_ORG_PLAYLIST_BASE_URL}/countries/cn.m3u",
    ),
    COUNTRY_US(
        id = "country_us",
        label = "美国频道",
        description = "按国家筛选：美国",
        playlistUrl = "${AppConstants.IPTV_ORG_PLAYLIST_BASE_URL}/countries/us.m3u",
    ),
    COUNTRY_JP(
        id = "country_jp",
        label = "日本频道",
        description = "按国家筛选：日本",
        playlistUrl = "${AppConstants.IPTV_ORG_PLAYLIST_BASE_URL}/countries/jp.m3u",
    ),
    COUNTRY_UK(
        id = "country_uk",
        label = "英国频道",
        description = "按国家筛选：英国",
        playlistUrl = "${AppConstants.IPTV_ORG_PLAYLIST_BASE_URL}/countries/uk.m3u",
    ),
    COUNTRY_KR(
        id = "country_kr",
        label = "韩国频道",
        description = "按国家筛选：韩国",
        playlistUrl = "${AppConstants.IPTV_ORG_PLAYLIST_BASE_URL}/countries/kr.m3u",
    ),
    LANGUAGE_ZHO(
        id = "language_zho",
        label = "中文频道",
        description = "按语言筛选：中文",
        playlistUrl = "${AppConstants.IPTV_ORG_PLAYLIST_BASE_URL}/languages/zho.m3u",
    ),
    LANGUAGE_ENG(
        id = "language_eng",
        label = "英文频道",
        description = "按语言筛选：英文",
        playlistUrl = "${AppConstants.IPTV_ORG_PLAYLIST_BASE_URL}/languages/eng.m3u",
    ),
    LANGUAGE_JPN(
        id = "language_jpn",
        label = "日文频道",
        description = "按语言筛选：日文",
        playlistUrl = "${AppConstants.IPTV_ORG_PLAYLIST_BASE_URL}/languages/jpn.m3u",
    ),
    LANGUAGE_KOR(
        id = "language_kor",
        label = "韩文频道",
        description = "按语言筛选：韩文",
        playlistUrl = "${AppConstants.IPTV_ORG_PLAYLIST_BASE_URL}/languages/kor.m3u",
    ),
    CATEGORY_NEWS(
        id = "category_news",
        label = "新闻频道",
        description = "按分类筛选：新闻",
        playlistUrl = "${AppConstants.IPTV_ORG_PLAYLIST_BASE_URL}/categories/news.m3u",
    );

    companion object {
        fun fromId(id: String?): PlaylistScope {
            return entries.firstOrNull { it.id == id } ?: ALL
        }
    }
}
