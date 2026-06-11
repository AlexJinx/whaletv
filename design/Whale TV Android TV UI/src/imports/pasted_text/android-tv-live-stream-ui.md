Design a production-grade Android TV app UI for a Chinese live TV streaming app based on iptv-org/iptv.

Target device: 1920x1080 Android TV / TV box, 10-foot remote-control experience. The app is for browsing and watching publicly available live TV streams from M3U playlists, especially Chinese channels such as CCTV, satellite TV, local channels, news, sports, kids, documentary, favorites, and all channels.

Important product truth:
- The primary data source is iptv-org M3U / stream data.
- Reliable data includes: channel name, channel logo when available, category/group, stream URL, stream count, optional stream quality, optional referrer/user-agent, favorites stored locally, recently watched stored locally, playlist sync status, and local playback health after trying a stream.
- EPG/program schedule is optional. It is only available when the user configures a valid XMLTV URL and matching program data exists.
- Do not show fake current program names, fake next programs, fake progress bars, fake channel numbers, or fake “network stable” states.
- If EPG is not available, the UI should still look complete by focusing on channel browsing, live playback, source count, category, favorites, and sync state.

Design style:
Premium, calm, cinematic, modern Chinese TV product interface. Dark mode only. Use deep charcoal / near-black backgrounds, subtle layered panels, soft glass-like overlays, clean typography, restrained cyan accent, warm gold for favorite state, red only for LIVE/error/status. Avoid childish gradients, decorative blobs, marketing hero layouts, oversized empty cards, mobile-style UI, and movie-poster streaming layouts.

Global UX requirements:
- Must be optimized for D-pad remote control.
- Every focus state must be obvious: slight scale, bright cyan border, soft glow, elevated shadow.
- Text must be readable from sofa distance.
- Dense enough for live TV browsing, but still premium and spacious.
- First screen is the actual TV browsing experience, not a landing page.
- No instructional tutorial text.
- Use 8px radius for cards and panels.
- Use real icons for search, settings, refresh, favorite, play, back.
- Use channel logo placeholders or real logo slots, not random photos.
- Avoid nested cards inside cards.
- Keep all text inside bounds.

Create these screens:

1. Home screen
- Top bar: icon-only whale brand mark, current time/date, playlist sync status, search button, refresh button, settings button.
- Do not show the text “鲸鱼TV” in the left/nav brand area; keep only the whale icon.
- Sync status examples: 正在同步, 已同步, 同步失败, 等待同步.
- Left navigation: 继续观看, 收藏, 央视, 卫视, 地方, 新闻, 体育, 少儿, 纪录片, 全部频道.
- Main hero area: focused live channel preview card showing channel logo, channel name, category, LIVE badge, source count such as “2 个播放源”, optional quality only if available.
- If EPG is unavailable, do not show current program, next program, or progress bar.
- If EPG is available, allow a compact optional program line and progress bar, clearly secondary.
- Below hero: horizontal channel rows with channel cards.
- Channel card content: logo, channel name, category/group, LIVE badge, favorite icon if favorited, source count, optional quality.
- Show one card in focused state.
- Use realistic Chinese channel names: CCTV-1 综合, CCTV-5 体育, CCTV-13 新闻, CCTV-14 少儿, 湖南卫视, 东方卫视, 广东卫视, 北京卫视.

2. Player screen
- Fullscreen live video placeholder, mostly unobstructed.
- Minimal translucent top overlay: back icon, channel name, favorite icon, LIVE status, active source label such as “源 1/2”, optional quality.
- Do not show fake “network stable”.
- Playback status may show: 正在连接, 缓冲中, 直播中, 当前源失败，正在切换备用源.
- Bottom overlay: compact channel carousel with focused channel enlarged.
- Optional right panel only when EPG data exists; otherwise no EPG panel.

3. Channel detail screen
- Large channel header with logo, channel name, category, favorite toggle, play button.
- Show available stream sources as real source rows: 源 1, 源 2, optional quality, status 待检测 / 可用 / 一般 / 不可用.
- Do not imply remote health data before playback.
- Show EPG section only if XMLTV data exists; otherwise show a small empty state: 暂无节目单数据，直播不受影响.
- Tags may include: 直播, 收藏, 分类, optional quality.

4. Search screen
- TV-friendly search UI with large input, recent searches, and results grid.
- Search result cards use the same truthful channel card format: logo, channel name, category, LIVE, source count, optional quality.
- Focus state clearly visible.

5. Settings screen
- Clean TV settings layout.
- Sections: 播放源, 节目单, 刷新, 播放, 启动, 缓存.
- Keep only implemented settings:
  - built-in iptv-org China M3U source
  - custom M3U URL
  - XMLTV EPG URL
  - auto refresh
  - refresh interval
  - hide unavailable channels
  - start with last channel
  - manual refresh
  - clear cache
- Remove unsupported settings such as subtitles, preferred quality selection, fake EPG cache size, and any movie/VOD features.

Output:
Create a polished high-fidelity Android TV UI design system and screens. Include color palette, typography scale, focused/unfocused component states, truthful channel card variants, playback overlay components, optional EPG states, source health states, and spacing guidelines.

Negative prompt:
Do not create a mobile app UI. Do not create a marketing landing page. Do not use decorative orb backgrounds, childish gradients, huge empty hero sections, rounded pill-heavy mobile controls, unreadable small text, overlapping text, nested cards, generic streaming movie posters, fake program schedules, fake progress bars, fake channel numbers, or fake network health. This is a live TV channel browsing app based on M3U streams, not a Netflix clone.