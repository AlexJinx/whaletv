# 鲸鱼TV

鲸鱼TV 是一个面向 Android TV / 电视盒子的 IPTV 应用原型。当前版本已经具备首页浏览、搜索、播放器、设置页、EPG 节目展示和 iptv-org 数据同步能力。

## 当前能力

- Android TV 原生启动入口，包名 `com.jing.whaletv`，最低 Android 8.0。
- 首页按国家、分类、收藏和观看历史展示频道，频道卡片会显示播放源、清晰度和真实 EPG 状态。
- 搜索页支持遥控器输入、清空关键词、查看搜索结果，并可直接进入播放。
- 播放器基于 Media3，支持播放成功/失败记录、不可用源过滤、收藏切换和当前/后续节目单展示。
- 设置页支持数据源、节目单、自动刷新、同步状态、维护和来源说明。
- 默认使用 iptv-org 官方 playlist；用户可以选择优先更新范围，例如全部频道、中国频道、中文频道或新闻频道。
- 同步流程会先更新所选优先范围，再通过 WorkManager 后台补全全部频道。
- EPG 来源只使用 playlist 自动发现的 `x-tvg-url`，不再请求 iptv-org 官方 `guides.json`。
- 内置数据源会优先请求 Gitee raw 镜像，失败后兜底请求 iptv-org 官方源，适配电视无法访问 GitHub Pages 的网络环境。
- Room 本地缓存频道、播放源、节目单、收藏、观看历史和同步状态。
- OkHttp 同步支持 ETag / Last-Modified，刷新失败时继续使用旧缓存。

## Gitee 镜像同步

仓库内置 `.github/workflows/sync-iptv-gitee.yml`，用于把完整 `iptv-org/iptv` 项目同步到 Gitee `main` 分支，并把 WhaleTV 运行时需要的静态文件发布到 Gitee `pages` 分支。

需要在 GitHub 仓库 Secrets 中配置：

- `GITEE_IPTV_MIRROR_REPO`：Gitee 镜像仓库 SSH 地址，例如 `git@gitee.com:AlexJinx/iptv-mirror.git`。
- `GITEE_SSH_PRIVATE_KEY`：可推送该 Gitee 仓库的 SSH 私钥。

默认 App 镜像地址直接使用 Gitee `pages` 分支的 raw 路径：

```text
https://gitee.com/AlexJinx/iptv-mirror/raw/pages
```

如果后续改成自有静态站点或其他托管地址，可在构建时覆盖：

```powershell
.\gradlew.bat assembleDebug -Pwhaletv.giteeMirrorBaseUrl=https://你的静态域名/iptv-mirror
```

## 设计源

当前视觉基线来自首页设计参考：

```text
design/WhaleTV Android TV Homepage/
```

设置页、搜索页和播放器应继续继承首页的深色 TV 风格、遥控器焦点状态和紧凑信息密度。

## 构建与验证

本机需要：

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0+

在项目根目录运行：

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat lintDebug
```

Debug APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK 输出路径：

```text
app/build/outputs/apk/release/app-release.apk
```

安装到已连接的 Android TV / 盒子：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 手动验收清单

- 首页：首次启动能看到频道加载状态；同步完成后能浏览分类频道；收藏和历史入口能正常切换。
- 搜索：能输入关键词、删除/清空关键词；搜索结果能点击进入播放器。
- 播放器：频道能尝试播放；返回能回到上一页；收藏按钮能切换；有 EPG 的频道能显示当前和后续节目。
- 设置页：数据源、节目单、自动刷新、同步状态、维护、关于来源页面能正常切换；保存提示只在需要保存的页面短暂显示。
- 同步：选择优先更新范围后，应用先更新该范围，再后台补全全部频道；失败时设置页能看到最近错误。
- 维护：重置播放源健康、清空节目单缓存、清空观看历史均需要二次确认。

## 合规备注

iptv-org/iptv 声明仓库只包含用户提交的公开视频流链接，不托管视频文件。鲸鱼TV 当前按自用/内部分发原型设计；如果公开上架或商业运营，需要额外准备内容授权、投诉下架机制、隐私政策和平台审核材料。
