# ATFOpenAndroidSdkDemo

ATFO OpenAdSDK（`com.atfo.ad:core`）Android 接入示例，覆盖开屏、信息流（模板渲染 / 宿主自渲染两页）、插屏、激励视频（含服务端奖励校验 SSV）、通知广告五类，共六个广告页面，外加一个设置页（调试面板入口 + 代码位切换）。

<!-- SDK-VERSION:START -->
![SDK 最新版本](https://img.shields.io/badge/SDK%20最新版本-2.13.0-1677FF?logo=android&logoColor=white&style=for-the-badge)

<font color="red">⚠️ 版本号最新不代表合入我们最新的功能，使用版本号前请联系我们的商务。</font>
<!-- SDK-VERSION:END -->

## 目录

- [接入文档](#接入文档)
- [1. 注册开户](#1-注册开户)
- [2. 配置参数](#2-配置参数)
- [3. 运行](#3-运行)
- [4. 查看接入代码](#4-查看接入代码)
- [5. 三方广告网络 adapter](#5-三方广告网络-adapter)
- [6. 激励视频发奖（2.12.0）](#6-激励视频发奖2120)
- [7. 可选：运行环境要求（2.12.0 新增）](#7-可选运行环境要求2120-新增)
- [8. OAID 自动采集与广告行业分类（2.13.0 新增）](#8-oaid-自动采集与广告行业分类2130-新增)

## 接入文档

📖 **在线文档**（GitHub Pages，左侧导航 + 搜索）：https://atfoenginex.github.io/ATFOpenAndroidSdkDemo/

## 1. 注册开户

1. 联系商务人员获取媒体账户
2. 在管理后台创建应用，获取应用 ID
3. 创建广告位，获取广告位 ID
4. 获取安全密钥用于 SDK 初始化

以上步骤请联系对应的商务对接人员！

## 2. 配置参数

参数全部走 `local.properties`，不需要改任何代码。仓库里只提供一份空值模板 `local.properties.example`（每个 key 都有注释），先复制再生效：

```bash
cp local.properties.example local.properties
```

然后填入对接运营给的账户与代码位：

```properties
atfo.appId=
atfo.secret=
atfo.mediaId=
# PRODUCTION | STAGING，要与后台环境一致，否则取不到广告配置
atfo.env=PRODUCTION
# true 时 SDK 输出内部日志，联调阶段建议打开
atfo.debug=true

# ==== 应用标识 ====
# 包名，必填
atfo.applicationId=com.atfo.ad.demo
# App name
atfo.appName=ATFO Demo

# ==== 代码位 Id（向对接运营获取）====
atfo.splashSlotId=
# 信息流分两个代码位：下面这个后台配成「模板渲染」
atfo.feedSlotId=
# 这个配成「自渲染」，供信息流自渲染页使用
atfo.feedSelfRenderSlotId=
atfo.interstitialSlotId=
atfo.rewardSlotId=
# 通知广告复用信息流代码位，后台需配成「ATFO 模板渲染 + 信息流-通知广告」
atfo.notifySlotId=
```

`local.properties` 已被 `.gitignore` 忽略，不会误提交密钥。

- 信息流按渲染方式分两个代码位：`atfo.feedSlotId` 后台配成「ATFO 模板渲染」给模板页用，`atfo.feedSelfRenderSlotId` 配成「自渲染」给自渲染页用。加载日志里的 `renderType` 会标明本次物料类型，配错时页面底部 ConsoleView 会直接提示。
- 每个 key 都要保留 `atfo.` 前缀，去掉前缀的写法读不到。

## 3. 运行

要求 JDK 17+（Gradle 9.5 / AGP 9.3）、Android SDK 37。模板里的 `sdk.dir` 是注释状态，命令行构建时用环境变量提供，或按自己机器取消注释：

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk      # macOS 示例
./gradlew :app:assembleDebug                        # 产物 app/build/outputs/apk/debug/
./gradlew :app:installDebug                         # 装机
```

## 4. 查看接入代码

主页六个广告入口各对应一个接入示例（另有「设置」页），每页一个独立 Activity（配套同名布局 `activity_<类型>_ad.xml`），可直接整文件复制到自己的工程：

| 广告类型 | 文件 |
|---|---|
| 开屏 | `app/src/main/java/com/atfo/ad/demo/splash/SplashAdActivity.kt` |
| 信息流（模板渲染） | `app/src/main/java/com/atfo/ad/demo/feed/FeedAdActivity.kt` |
| 信息流（宿主自渲染） | `app/src/main/java/com/atfo/ad/demo/feed/FeedSelfRenderAdActivity.kt` |
| 插屏 | `app/src/main/java/com/atfo/ad/demo/interstitial/InterstitialAdActivity.kt` |
| 激励视频 | `app/src/main/java/com/atfo/ad/demo/reward/RewardAdActivity.kt` |
| 通知广告 | `app/src/main/java/com/atfo/ad/demo/notify/NotifyAdActivity.kt` |

SDK 初始化在 `AtfoDemoApplication.kt`，参数读取在 `config/DemoConfig.kt`，自渲染视图在 `view/SelfRenderAdView.kt`（左图右文卡片，布局 `view_self_render_ad.xml`）。
自渲染页的卡片一律由宿主渲染，`getAdView()` 只是媒体位素材：它为空时用 `videoUrl` 在媒体位自播。用下面的 TAG 过滤即可看到完整回调流：

```bash
adb logcat -s ATFOSplashAdActivity:V ATFOFeedAdActivity:V ATFOFeedSelfRenderAdActivity:V ATFOInterstitialAdActivity:V ATFORewardAdActivity:V ATFONotifyAdActivity:V
```

2.13.0 起六个广告页的加载成功回调里都会输出广告行业分类标签（`getAdFirstCategory()` / `getAdSecondCategory()`），同样用上面的 TAG 过滤即可看到。

## 5. 三方广告网络 adapter

Demo 默认只依赖 `com.atfo.ad:core`，ATFO 自有广告源开箱即用。SDK 还内置了多家三方广告网络的 adapter，按需追加依赖（groupId 统一为 `com.atfo.ad.adapt`，版本与 core 保持一致，见 `gradle/libs.versions.toml` 的 `atfoCore`），并在 SDK 初始化前调用对应 `register()`：

| 广告网络           | 依赖坐标（`com.atfo.ad.adapt:` 前缀） | 注册类（`import` 完整类名） |
|----------------|---|---|
| GroMore（穿山甲聚合） | `adapt-gromore` | `com.atfo.ad.adapt.gromore.initializer.GRMAdSourceInitializer` |
| 优量汇（广点通）       | `adapt-gdt` | `com.atfo.ad.adapt.gdt.initializer.GDTAdSourceInitializer` |
| 百度             | `adapt-baidu` | `com.atfo.ad.adapt.baidu.initializer.BaiduAdSourceInitializer` |
| 快手             | `adapt-ks` | `com.atfo.ad.adapt.ks.initializer.KSAdSourceInitializer` |
| 美数             | `adapt-ms` | `com.atfo.ad.adapter.ms.initializer.MSAdSourceInitializer` |
| 趣盟             | `adapt-qumeng` | `com.atfo.ad.adapt.qumeng.initializer.QuMengAdSourceInitializer` |
| 倍孜             | `adapt-bz` | `com.atfo.ad.adapt.bz.initializer.BZAdSourceInitializer` |
| 优酷             | `adapt-yk` | `com.atfo.ad.adapt.fanti.initializer.YKAdSourceInitializer` |
| 汇川（Noah）       | `adapt-noah` | `com.atfo.ad.adapt.noah.initializer.NoahAdSourceInitializer` |
| Sigmob         | `adapt-sigmob` | `com.atfo.ad.adapt.sigmob.initializer.SigmobAdSourceInitializer` |
| 旺脉             | `adapt-wm` | `com.atfo.ad.adapt.wm.initializer.WMAdSourceInitializer` |

三方广告 SDK 本体由 adapter 的 POM 传递引入，不需要单独添加依赖。

以接入优量汇为例，`app/build.gradle.kts` 追加依赖：

```kotlin
implementation("com.atfo.ad.adapt:adapt-gdt:2.13.0")
```

然后在 `Application#onCreate` 里、SDK `init` 之前导入并注册：

```kotlin
import com.atfo.ad.adapt.gdt.initializer.GDTAdSourceInitializer

GDTAdSourceInitializer.register()
```

不需要某家网络参与时，把对应 `AdSource` 放进初始化参数的 `forbidNetworkList` 即可禁用，`AtfoDemoApplication.kt` 里有注释示例。

## 6. 激励视频发奖（2.12.0）

从 2.12.0 起发奖判定统一看 `onRewardResult(verified)`：

- `onReward()` 只在三方广告源达标时回调，不要再在这里直接发奖；
- API（core 自渲染）激励开启 SSV 后，`verified=true` 表示服务端已确认，可发奖；未下发凭证时直接回调 `verified=false`；
- 广告位侧用 `ATFOAdSlot.Builder().rewardUserKey("稳定业务用户ID")` 传对账标识，不设则回退初始化时的 `userId`；
- 展示期间（未收到 `onAdClosed`）**不要**在 `onDestroy()` 里销毁广告对象，`destroy()` 按放弃奖励处理，会丢失本次 `onRewardResult`。

完整示例见 `app/src/main/java/com/atfo/ad/demo/reward/RewardAdActivity.kt`，契约细节见在线文档「激励视频广告 → 发奖结果与服务端校验（SSV）」。

## 7. 可选：运行环境要求（2.12.0 新增）

- 应用下载广告需要 `REQUEST_INSTALL_PACKAGES`（SDK 的 AAR 已声明），26+ 运行时按 `canRequestPackageInstalls` 引导用户授权；
- SDK 已声明 `MAIN` + `LAUNCHER` 的 `<queries>`；
- AAR 会合并 3 个 Activity 和 2 个 FileProvider（`${applicationId}.atfo.debugfile`、`${applicationId}.atfo.downloadfile`），用的都是独立子类，不会与宿主冲突。

## 8. OAID 自动采集与广告行业分类（2.13.0 新增）

**OAID 兜底自采**：`cur_id` 仍请必传（`.extra("cur_id", "your_OAID")`，提高广告填充）。从 2.13.0 起，未传时 SDK 会兜底自采 OAID（厂商通道 + msa 直连），填充不再依赖宿主传值：

- 优先级：宿主传入的 `cur_id` 优先；未传时用 SDK 自采结果，采到后缓存（默认 10 天 TTL，可由服务端 sdk_options 的 `oaid_cache_ttl_days` 调整），首次请求可能为空；
- 合规：宿主传入的 `cur_id` 不受 SDK 隐私开关管控，合规责任在宿主；服务端应急关停 OAID 上报时，宿主需同步停传 `cur_id`；
- 隐私开关新增管控点：`CREATIVE` 广告源的 `oaid` 开关管控 SDK 自采链路，关闭后重启进程会同时清掉 OAID 磁盘缓存；
- AAR 已自动合并 10 个 OAID 厂商包的 `<queries>`（msa、华为、OPPO、vivo、荣耀、三星、魅族、华硕、联想、努比亚），宿主零改动；混淆规则随 AAR 自动生效。

**广告行业分类**：广告对象新增 `getAdFirstCategory()` / `getAdSecondCategory()`，返回本次广告的一级 / 二级行业分类（可能为 null）：

```kotlin
ad.getAdFirstCategory()   // 一级行业分类，可能为 null
ad.getAdSecondCategory()  // 二级行业分类，可能为 null
```

六个广告页的加载成功回调里都演示了取值与输出（logcat TAG 过滤即可看到）。

**其他行为变化**：服务端已关闭的代码位不再参与加载（缓存 / 内存池 / 无缓存统一口径），无填充上报错误码 10000。
