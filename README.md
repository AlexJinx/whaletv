# 鲸鱼TV

鲸鱼TV 是一个面向 Android TV / 电视盒子的国内直播 App 原型工程。第一版为纯端侧实现，不依赖自建后台：频道数据来自 iptv-org 中国 M3U，本地用 Room 缓存，后台用 WorkManager 定时刷新，播放内核使用 libVLC。APK 同时保留普通手机安装能力，方便侧载测试；手机上会强制横屏显示 TV 界面。

## 功能

- Android TV 原生启动入口，包名 `com.jing.whaletv`，最低 Android 8.0。
- 默认拉取 `https://iptv-org.github.io/iptv/index.m3u`（全球频道源），并按央视、卫视、地方、新闻、体育、音乐、娱乐、电影、少儿、纪录片等分区展示。
- Room 本地缓存频道、备用源、收藏、最近观看、节目单和播放健康状态。
- OkHttp 同步支持 ETag / Last-Modified，刷新失败时继续使用旧缓存。
- WorkManager 自动刷新频道和可选 XMLTV 节目单。
- libVLC 播放 HLS/HTTP/HTTPS 网络流，支持 User-Agent / Referer，播放失败自动切备用源。
- TV 大屏 UI：频道墙、搜索、设置、播放页快速切台、收藏和当前/下一节目展示。

## 为什么使用 VLC

本工程默认使用 `org.videolan.android:libvlc-all:3.7.2`。相较 Media3/ExoPlayer，libVLC 对各种不规整 IPTV 源、TS/HLS 变体、HTTP 直播流的兼容性更强。代价是 AAR 体积更大，并且需要关注 LGPL 2.1 许可义务。

后续如果某些设备上 VLC 的硬解或低延迟表现不理想，可以在 `playback` 包下增加第二个播放 Host，把同一套频道与切源逻辑接到 Media3。

## 构建

本机需要：

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0+

在项目根目录运行：

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Debug APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接的 Android TV / 盒子：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

如果需要先在手机上测试，也可以直接侧载同一个 APK。Manifest 中 `android.software.leanback` 被声明为可选，因此手机不会因为缺少 Android TV/Leanback 特性而拒绝安装。

## 设置项

- 自定义 M3U：追加自己的播放列表。
- XMLTV 节目单：填入可访问的 XMLTV URL 后，首页和播放页会显示当前/下一节目。
- 自动刷新：默认开启。
- 刷新间隔：默认 12 小时。
- 隐藏不可用频道：根据本地播放失败记录过滤。
- 启动进入上次频道：默认开启。
- 清除缓存：清除频道、播放源、节目单和同步状态。

## 合规备注

iptv-org/iptv 声明仓库只包含用户提交的公开视频流链接，不托管视频文件。鲸鱼TV 第一版按自用/内部分发设计；如果公开上架或商业运营，需要额外准备内容授权、投诉下架机制、隐私政策和平台审核材料。

libVLC Android 依赖使用 LGPL 2.1 许可。公开分发时应保留许可声明，并确保满足动态链接、替换库等 LGPL 要求。
