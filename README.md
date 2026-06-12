# 鲸鱼TV

鲸鱼TV 是一个面向 Android TV / 电视盒子的全球 IPTV 首页原型工程。当前版本已经收敛为干净的重构起点：只保留新版首页和 iptv-org 数据核心，不再包含旧播放器、搜索页、设置页或 EPG 页面。

## 当前范围

- Android TV 原生启动入口，包名 `com.jing.whaletv`，最低 Android 8.0。
- 默认拉取 `https://iptv-org.github.io/iptv/index.m3u`（全球频道源）。
- 首页按国家和 iptv-org 官方分类展示频道，默认进入 `中国 / 新闻`。
- 首页支持收藏和历史两个视觉入口，并基于本地已有 `isFavorite` / `lastWatchedAt` 数据筛选频道。
- Room 本地缓存频道、备用源和节目单数据。
- OkHttp 同步支持 ETag / Last-Modified，刷新失败时继续使用旧缓存。
- WorkManager 自动刷新频道和可选 XMLTV 节目单。

## 已清理内容

- 旧播放器、搜索页、设置页、EPG 页。
- 旧通用频道卡片和 TV 控件组件。
- libVLC 播放依赖和旧 AndroidX TV UI 依赖。
- 旧设计目录 `design/Whale TV Android TV UI/`。

## 设计源

当前首页设计参考保留在：

```text
design/WhaleTV Android TV Homepage/
```

本地生成的 `node_modules/` 和 `dist/` 不纳入版本控制。

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

## 合规备注

iptv-org/iptv 声明仓库只包含用户提交的公开视频流链接，不托管视频文件。鲸鱼TV 当前按自用/内部分发原型设计；如果公开上架或商业运营，需要额外准备内容授权、投诉下架机制、隐私政策和平台审核材料。
