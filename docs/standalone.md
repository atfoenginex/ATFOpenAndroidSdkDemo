---
title: 非聚合接入指南
nav_order: 2
---

# ATFOpenAdSDK Android 接入指南（非聚合）

## 目录

- [集成说明](#集成说明)
  - [快速指南](#快速指南)
  - [简介](#简介)
  - [接入提示](#接入提示)
  - [注册开户](#注册开户)
- [一、依赖配置](#一依赖配置)
  - [1、远程依赖方式引入](#1远程依赖方式引入)
  - [2、权限配置](#2权限配置)
- [二、SDK 初始化](#二sdk-初始化)
  - [在 Application 中初始化](#在-application-中初始化)
  - [环境配置](#环境配置)
  - [透传参数配置](#透传参数配置)
- [三、广告加载和展示](#三广告加载和展示)
  - [开屏广告](#开屏广告)
  - [信息流广告](#信息流广告)
  - [宿主自渲染（render_type=1）](#宿主自渲染rendertype1)
    - [展示示例](#展示示例)
    - [注意事项](#注意事项)
  - [插屏广告](#插屏广告)
  - [激励视频广告](#激励视频广告)
  - [通知广告](#通知广告)
- [四、竞价回传](#四竞价回传)
  - [1、竞胜回传](#1竞胜回传)
  - [2、竞败回传](#2竞败回传)
  - [3、广告渠道映射](#3广告渠道映射)
- [五、清理广告缓存](#五清理广告缓存)
  - [1、清空全部广告缓存](#1清空全部广告缓存)
  - [2、按广告位清理缓存](#2按广告位清理缓存)
- [六、生命周期管理](#六生命周期管理)
  - [广告对象销毁](#广告对象销毁)
  - [取消正在加载的广告](#取消正在加载的广告)
- [七、错误处理](#七错误处理)
  - [错误码说明](#错误码说明)
- [八、隐私数据获取说明](#八隐私数据获取说明)
  - [1、SDK 收集的信息](#1sdk-收集的信息)
- [九、联调排查](#九联调排查)
- [十、注意事项](#十注意事项)
- [十一、技术支持](#十一技术支持)
- [十二、SDK 涉及的三方库版本](#十二sdk-涉及的三方库版本若存在不兼容等问题请联系开发进行定制化修改)

## 集成说明

### 快速指南

⬇️ Demo 下载：<https://github.com/atfoenginex/ATFOpenAndroidSdkDemo>

### 简介

ATFOpenAdSDK 是一款广告 SDK，为开发者提供广告变现能力。

**非聚合模式仅依赖 `com.atfo.ad:core`，ATFO 自有广告源开箱即用，无需任何三方 adapter 依赖**；如需三方广告源或 GroMore 聚合，参见[聚合接入指南](aggregation.md)。

### 接入提示

- 请确保您的应用已在各广告平台完成注册和审核
- 建议在测试环境中完成 SDK 集成和调试后再发布到生产环境
- 注意遵守各广告平台的规范和政策

### 注册开户

1. 联系商务人员获取媒体账户
2. 在管理后台创建应用，获取应用 ID
3. 创建广告位，获取广告位 ID
4. 获取安全密钥用于 SDK 初始化

联系对应的商务对接人员！

## 一、依赖配置

### 1、远程依赖方式引入

1、在项目的 `settings.gradle.kts` 中添加 Maven 仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.cxwlad.com/repository/maven-releases/")
        }
        google()
        mavenCentral()
    }
}
```

2、在项目的 app 下的 `build.gradle.kts` 中添加依赖：

**最新版本号：2.12.0**

```kotlin
dependencies {
    // ATFO 广告核心组件
    implementation("com.atfo.ad:core:2.12.0")
}
```

本地 aar 依赖方式引入（本地文件和接入方式，请联系 ATFO 侧）。

### 2、权限配置

**以下权限 SDK 自身已在 AAR 的 `AndroidManifest.xml` 中声明，宿主无需重复添加**；列出来只是为了说明用途和合规审核。宿主主动申请运行时权限的，只有可选的读取电话状态 / 位置两项。

```xml
<!-- 必需：网络 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 可选：防作弊 + 优化广告投放（需要宿主运行时申请才生效） -->
<uses-permission android:name="android.permission.READ_BASIC_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- 通知栏震动反馈（仅后台 configs.notice.vibrate 开启时使用） -->
<uses-permission android:name="android.permission.VIBRATE" />

<!-- 应用下载广告：下载完成后拉起安装器（8.0 以下系统忽略；26+ 运行时按 canRequestPackageInstalls 引导） -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

#### 包可见性（`<queries>`）

SDK 已声明 `MAIN` + `LAUNCHER` 的 `<queries>`，即「所有桌面可见的应用」对 SDK 可见。这是 targetSdk 30+ 的必要条件：

- deeplink 落地页的 `resolveActivity` / `getLaunchIntentForPackage`，不声明会假阴性，明明能打开却判「无应用响应」；
- 应用下载广告检查「是否已安装」，不声明会把已安装的应用判为未安装而重复下载；
- MIUI/HyperOS 链式启动适配读取微信、支付宝 uid。

只解除系统查询过滤，不采集任何数据。宿主自己也有 `<queries>` 时合并即可，不冲突。

#### 合并进来的组件（无需宿主声明）

| 类型 | 名称 | 说明 |
|---|---|---|
| Activity | `com.atfo.ad.webview.ATFOWebViewActivity` | 落地页 H5 展示 |
| Activity | `com.atfo.ad.activity.ATFORewardVideoAdActivity` | 激励视频全屏页 |
| Activity | `com.atfo.ad.debug.ATFODebugActivity` | 调试面板（`enabled=false`，仅调试入口唤起） |
| Provider | `${applicationId}.atfo.debugfile` | 调试面板导出分享 |
| Provider | `${applicationId}.atfo.downloadfile` | 应用下载广告 APK 安装 |

两个 Provider 都使用独立子类和 `${applicationId}` 前缀的 authority，不会与宿主或其他 adapter 的 FileProvider 冲突，不需要在宿主里写 `tools:replace`。

## 二、SDK 初始化

### 在 Application 中初始化

```kotlin
import android.app.Application
import android.util.Log
import com.atfo.ad.core.ATFOADSDKManager
import com.atfo.ad.core.ATFOSDKConfig
import com.atfo.ad.listener.ATFOAdInitListener

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化 SDK
        initializeATFOSDK()
    }

    private fun initializeATFOSDK() {
        val sdkConfig = ATFOSDKConfig.Builder()
            .appId("your_app_id")                    // 应用 ID
            .mediaId("your_mediaId")                 // 媒体 ID
            .secret("your_secret")                   // 安全密钥
            .appName("应用名称")
            .environment(ATFOSDKConfig.Environment.PRODUCTION)  // 环境配置
            .debug(true)                             // 调试模式，开发阶段建议开启
            // 透传参数，接入方替换为自己的userId，用于关联用户。
            .extra("userId", "your_user_id")
            // 透传参数，接入方替换为自己的OAID，必传❗❗❗提高广告填充。
            .extra("cur_id", "your_OAID")
            .build()

        ATFOADSDKManager.getInstance().init(this, sdkConfig, object : ATFOAdInitListener {
            override fun onSuccess() {
                Log.d("MyApplication", "✅ SDK初始化成功")
            }

            override fun onFail() {
                Log.e("MyApplication", "SDK初始化失败")
            }
        })
    }
}
```

`ATFOSDKConfig.Builder` 参数速查：

| 参数 | 说明 | 默认值 |
|---|---|---|
| `appId` / `mediaId` / `secret` | 开户获取的三个必填凭证（`build()` 要求 `appId` 非空） | — |
| `appName` | 应用名称 | — |
| `environment` | `PRODUCTION` / `STAGING` | `PRODUCTION` |
| `debug` | 调试日志开关 | `false` |
| `extra(key, value)` | 透传参数 | 空 |

### 环境配置

SDK 支持多种环境配置：

```kotlin
// 生产环境
.environment(ATFOSDKConfig.Environment.PRODUCTION)

// 测试环境
.environment(ATFOSDKConfig.Environment.STAGING)
```

### 透传参数配置

```kotlin
// 设置透传参数（key 在前，value 在后）
.extra("userId", "业务用户ID")     // 选传：关联用户，激励 SSV 的 rewardUserKey 未设置时回退到它
.extra("cur_id", "android_mock_oaid_123")  // 选传：宿主持有的 OAID，不传时 SDK 自行获取
```

其余参数：`grmAutoStart`（GroMore 自动 start，默认 `true`；置 `false` 需宿主自行调用 `ATFOADSDKManager.start()`）、`forbidNetworkList`（禁用的广告网络列表，默认空）、`debug`（调试日志，默认 `false`）。

## 三、广告加载和展示

加载统一走 `ATFOAdLoader(context, isDebug)`，第二个参数是调试日志开关，与初始化的 `debug` 保持一致即可；回调接口为 `ATFOAdLoader.AdLoadListener`（`onAdLoadSuccess` / `onAdLoadFailed`）。

`showAd` 有两个入口，按广告类型选择：**Activity 入口** `showAd(activity, listener)` 用于插屏、激励视频、通知栏样式信息流；**容器入口** `showAd(container, listener)` 用于开屏与其余信息流模板；自渲染信息流不走 `showAd`，读 `ATFOAd.nativeInfo` 拼 UI 后调用 `registerInteraction`。

### 开屏广告

```kotlin
class SplashActivity : AppCompatActivity() {

    private lateinit var adContainer: FrameLayout
    private var splashAdObject: ATFOAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        adContainer = findViewById(R.id.ad_container)

        // 加载开屏广告
        loadSplashAd()
    }

    private fun loadSplashAd() {
        val adSlot = ATFOAdSlot.Builder()
            .adType(AdType.SPLASH)
            .timeout(5000)                    // 可选；超时以管理后台下发为准，建议不配置（见「注意事项」）
            .slotId("") // 代码位 Id（向对接运营获取）
            .size(1080, 1920)
            .build()

        ATFOAdLoader(this, true).loadAd(adSlot, object : ATFOAdLoader.AdLoadListener {
            override fun onAdLoadSuccess(adObject: ATFOAd) {
                // 广告加载成功
                Log.d("SplashAd", "广告加载成功")
                Log.d("SplashAd", "广告ID: ${adObject.adId}")
                // 单位是分。取价需「带价格」版本 SDK（release 默认无 price 字段），用反射取值，见下文 AdPriceUtils。
                Log.d("SplashAd", "价格: "${AdPriceUtils.getPrice(adObject)})

                splashAdObject = adObject

                // 显示广告
                showSplashAd()
            }

            override fun onAdLoadFailed(error: ATFOAdError) {
                // 广告加载失败
                Log.e("SplashAd", "广告加载失败: ${error.message}")
                goToMainActivity()
            }
        })
    }

    private fun showSplashAd() {
        val adObject = splashAdObject ?: return

        if (!adObject.isValid()) {
            // 判断广告是否可用
            return
        }

        adObject.showAd(adContainer, object : AdInteractionListener {
            override fun onAdShowSuccess() {
                Log.d("SplashAd", "开屏广告展示成功")
            }

            override fun onAdShowFailed(errorCode: Int, errorMessage: String) {
                Log.e("SplashAd", "开屏广告展示失败: $errorMessage")
                goToMainActivity()
            }

            override fun onAdClicked() {
                Log.d("SplashAd", "开屏广告被点击")
                goToMainActivity()
            }

            override fun onAdClosed() {
                Log.d("SplashAd", "开屏广告被关闭")
                goToMainActivity()
            }

            override fun onAdRenderFail(errorCode: Int, errorMessage: String) {
                Log.d("SplashAd", "❌ 开屏广告renderFail")
            }

            override fun onAdExposed() {
                Log.d("SplashAd", "开屏广告exposed")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        splashAdObject?.destroy()
    }
}
```

> `goToMainActivity()` 为示例中的业务跳转方法，需要按自己的页面结构实现。

### 信息流广告

```kotlin
class FeedAdActivity : AppCompatActivity() {

    private lateinit var adContainer: FrameLayout
    private var feedAdObject: ATFOAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        adContainer = findViewById(R.id.ad_container)

        // 加载信息流广告
        loadFeedAd()
    }

    private fun loadFeedAd() {
        val adSlot = ATFOAdSlot.Builder()
            .adType(AdType.FEED)
            .timeout(8000)                    // 可选；超时以管理后台下发为准，建议不配置（见「注意事项」）
            .slotId("") // 代码位 Id（向对接运营获取）
            .size(1080, 1080)
            .extras(mapOf("scene" to "feed_list"))
            .build()

        ATFOAdLoader(this, true).loadAd(adSlot, object : ATFOAdLoader.AdLoadListener {
            override fun onAdLoadSuccess(adObject: ATFOAd) {
                Log.d("FeedAd", "信息流广告加载成功")
                Log.d("FeedAd", "广告ID: ${adObject.adId}")
                // 单位是分。取价需「带价格」版本 SDK（release 默认无 price 字段），用反射取值，见下文 AdPriceUtils。
                Log.d("FeedAd", "价格: "${AdPriceUtils.getPrice(adObject)})

                feedAdObject = adObject

                // 显示广告
                showFeedAd()
            }

            override fun onAdLoadFailed(error: ATFOAdError) {
                Log.e("FeedAd", "信息流广告加载失败: ${error.message}")
            }
        })
    }

    private fun showFeedAd() {
        val adObject = feedAdObject ?: return

        if (!adObject.isValid()) {
            // 判断广告是否可用
            return
        }

        adObject.showAd(adContainer, object : AdInteractionListener {
            override fun onAdShowSuccess() {
                Log.d("FeedAd", "信息流广告展示成功")
            }

            override fun onAdShowFailed(errorCode: Int, errorMessage: String) {
                Log.e("FeedAd", "信息流广告展示失败: $errorMessage")
            }

            override fun onAdClicked() {
                Log.d("FeedAd", "信息流广告被点击")
            }

            override fun onAdClosed() {
                Log.d("FeedAd", "信息流广告被关闭")
            }

            override fun onAdRenderFail(errorCode: Int, errorMessage: String) {
                Log.d("FeedAd", "❌ 信息流广告renderFail")
            }

            override fun onAdExposed() {
                Log.d("FeedAd", "信息流广告exposed")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        feedAdObject?.destroy()
    }
}
```

### 宿主自渲染（render_type=1）

SDK 通过 `ATFOAd.nativeInfo` 提供物料（标题、描述、图片、视频等），UI 由接入方自行构建，最后调用 `ATFOAd.registerInteraction` 绑定交互。曝光、点击、关闭的上报由 SDK 统一处理，接入方无需重复上报。

判断方式：`ATFOAd.isNativeExpress()` 为 `false` 即为自渲染广告，物料见 `ATFOAd.nativeInfo`；为 `true` 为三方模板广告，使用 `ATFOAd.showAd` 展示。

物料字段（ATFONativeFeedAdData）：

| 字段 | 说明 |
|---|---|
| `title` / `description` / `source` | 标题、描述、来源 |
| `icon` / `images` | 图标与主图（`ATFONativeImage`，含 `url` / `width` / `height`） |
| `videoUrl` / `videoCover` / `videoDurationSec` | 视频地址、封面、时长（秒）；`videoUrl` 由宿主 VideoView 播放 |
| `landingUrl` / `deeplinkUrl` / `downloadUrl` | 落地页、深链、下载地址 |
| `buttonText` | CTA 按钮文案 |
| `appInfo` | 应用下载类广告信息（名称、包名、隐私政策等） |
| `sdkVideoRender` | `true` 表示视频由三方 SDK 的 MediaView 播放，必须使用 `getAdView()` |
| `adLogo` | 广告合规角标（如百度/穿山甲），宿主 UI 需展示 |

辅助方法：`primaryImageUrl()` 返回第一张可用主图/封面/图标；`hasRenderableMaterial()` 判断物料是否足以渲染。

#### 展示示例

```kotlin
private fun showSelfRenderFeed(ad: ATFOAd, adContainer: FrameLayout) {
    // 模板广告（render_type=2/3）走 showAd；自渲染走本方法
    if (ad.isNativeExpress()) {
        ad.showAd(adContainer, interactionListener())
        return
    }

    val info = ad.nativeInfo
    if (info == null || !info.hasRenderableMaterial()) {
        Log.e("FeedAd", "自渲染广告物料为空或不足")
        return
    }

    // 1. 优先使用三方 SDK 提供的 View（如 GDT 自渲染视频 MediaView）
    val sdkView = ad.getAdView()
    if (sdkView != null) {
        adContainer.addView(
            sdkView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        ad.registerInteraction(
            container = adContainer,
            clickableViews = listOf(sdkView),
            closeViews = emptyList(),
            interactionListener = interactionListener(),
            adLogoParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                leftMargin = dp(8f)
                topMargin = dp(8f)
            }
        )
        return
    }

    // 2. sdkVideoRender=true 必须依赖 getAdView()
    if (info.sdkVideoRender) {
        Log.e("FeedAd", "sdkVideoRender=true 但 getAdView() 为空，无法展示")
        return
    }

    // 3. 宿主自行构建 UI（示例：左图右文；图片加载由接入方实现，如 Glide）
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    val cover = ImageView(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(110f), dp(110f))
        info.primaryImageUrl()?.let { loadImage(it, this) } // 自行实现图片加载（如 Glide）
    }
    val textColumn = TextView(this).apply {
        text = listOf(info.title, info.description)
            .filter { !it.isNullOrBlank() }
            .joinToString("\n")
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    root.addView(cover)
    root.addView(textColumn)
    // 合规角标需展示：info.adLogo（registerInteraction 的 adLogoParams 控制其位置）
    adContainer.addView(root)

    // 4. 绑定交互：点击区域、关闭按钮；曝光/点击/关闭由 SDK 自动上报
    ad.registerInteraction(
        container = root,
        clickableViews = listOf(root, cover, textColumn),
        closeViews = emptyList(), // 如需关闭按钮：listOf(closeButton)
        interactionListener = interactionListener(),
        adLogoParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.END or Gravity.TOP
            leftMargin = dp(8f)
            topMargin = dp(8f)
        }
    )
}

// dp 换算由接入方实现，示例：
private fun dp(value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

// 交互监听器：与模板广告一致
private fun interactionListener(): AdInteractionListener = object : AdInteractionListener {
    override fun onAdShowSuccess() {}
    override fun onAdShowFailed(errorCode: Int, errorMessage: String) {}
    override fun onAdClicked() {}
    override fun onAdClosed() {}
    override fun onAdRenderFail(errorCode: Int, errorMessage: String) {}
    override fun onAdExposed() {}
}
```

#### 注意事项

- 自渲染广告不要调用 `showAd`，否则物料无法展示；
- `nativeInfo` 为空或 `hasRenderableMaterial()` 为 `false` 时按渲染失败处理；
- `sdkVideoRender = true` 时视频由三方 SDK 渲染，必须使用 `getAdView()`，不能自行用 `videoUrl` 播放；
- `adLogo` 为广告合规角标，需要展示在广告视图上。**两种做法二选一，别混用**：
  - 宿主自己画角标：读取 `info.adLogo?.url` 或 `info.adLogo?.bitmap` 画到卡片上，`registerInteraction` 里 **不要** 传 `adLogoParams`，否则会出现两个角标；
  - 交给 SDK 画：不读 `adLogo`，把 `adLogoParams` 传给 `registerInteraction`，由 SDK 按该参数定位角标；
- `buttonText` 可能是文案，也可能是图片 URL（`http` / `https` 开头），是图片时用 ImageView 加载，别当文字直接显示；
- `sdkVideoRender = false` 时用 `videoUrl` 在媒体位自播（宿主自建 VideoView，建议静音 + 循环，避免与原声冲突）；
- 判断顺序：先看 `nativeInfo` 是否为空、`hasRenderableMaterial()` 是否为 `false`（渲染失败），再取 `getAdView()`；
- 容器需先加入视图树再调用 `registerInteraction`；
- 广告对象使用完毕后调用 `destroy()` 释放。

### 插屏广告

```kotlin
class InterstitialAdActivity : AppCompatActivity() {

    private var interstitialAdObject: ATFOAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interstitial)

        // 加载插屏广告
        loadInterstitialAd()
    }

    private fun loadInterstitialAd() {
        val adSlot = ATFOAdSlot.Builder()
            .adType(AdType.INTERSTITIAL)
            .timeout(10000)                   // 可选；超时以管理后台下发为准，建议不配置（见「注意事项」）
            .slotId("") // 代码位 Id（向对接运营获取）
            .size(1080, 1920)
            .extras(mapOf("scene" to "interstitial"))
            .build()

        ATFOAdLoader(this, true).loadAd(adSlot, object : ATFOAdLoader.AdLoadListener {
            override fun onAdLoadSuccess(adObject: ATFOAd) {
                Log.d("InterstitialAd", "插屏广告加载成功")
                Log.d("InterstitialAd", "广告ID: ${adObject.adId}")
                // 单位是分。取价需「带价格」版本 SDK（release 默认无 price 字段），用反射取值，见下文 AdPriceUtils。
                Log.d("InterstitialAd", "价格: "${AdPriceUtils.getPrice(adObject)})

                interstitialAdObject = adObject

                // 显示广告
                showInterstitialAd()
            }

            override fun onAdLoadFailed(error: ATFOAdError) {
                Log.e("InterstitialAd", "插屏广告加载失败: ${error.message}")
            }
        })
    }

    private fun showInterstitialAd() {
        val adObject = interstitialAdObject ?: return

        if (!adObject.isValid()) {
            // 判断广告是否可用
            return
        }

        adObject.showAd(this, object : AdInteractionListener {
            override fun onAdShowSuccess() {
                Log.d("InterstitialAd", "插屏广告展示成功")
            }

            override fun onAdShowFailed(errorCode: Int, errorMessage: String) {
                Log.e("InterstitialAd", "插屏广告展示失败: $errorMessage")
            }

            override fun onAdClicked() {
                Log.d("InterstitialAd", "插屏广告被点击")
            }

            override fun onAdClosed() {
                Log.d("InterstitialAd", "插屏广告被关闭")
            }

            override fun onAdRenderFail(errorCode: Int, errorMessage: String) {
                Log.d("InterstitialAd", "❌ 插屏广告renderFail")
            }

            override fun onAdExposed() {
                Log.d("InterstitialAd", "插屏广告exposed")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        interstitialAdObject?.destroy()
    }
}
```

### 激励视频广告

```kotlin
class RewardAdActivity : AppCompatActivity() {

    private var rewardAdObject: ATFOAd? = null

    /**
     * 激励页展示中（showAd 后到 onAdClosed 前）。展示期间销毁广告对象按「放弃奖励」处理：
     * 已提交的服务端校验请求继续执行，但 onRewardVerified / onRewardVerifyFailed / onRewardResult
     * 都不再投递，本次奖励会永久丢失。
     */
    private var rewardShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward)

        // 加载激励视频广告
        loadRewardAd()
    }

    private fun loadRewardAd() {
        val adSlot = ATFOAdSlot.Builder()
            .adType(AdType.REWARD)
            // 可选；超时以管理后台下发为准，建议不配置（见「注意事项」）
            // .timeout(10000)
            .slotId("") // 代码位 Id（向对接运营获取）
            .size(720, 1280)
            .extras(mapOf("scene" to "reward"))
            // 服务端奖励校验（SSV）对账的 user_key：稳定的业务用户标识（如账号 ID），
            // 不要用设备号 / 会话 ID；不设置时 SDK 回退初始化配置里的 userId
            .rewardUserKey("业务用户标识")
            .build()

        ATFOAdLoader(this, true).loadAd(adSlot, object : ATFOAdLoader.AdLoadListener {
            override fun onAdLoadSuccess(adObject: ATFOAd) {
                Log.d("RewardAd", "激励视频广告加载成功, 广告ID: ${adObject.adId}")
                // 单位是分。取价需要「带价格」版本的 SDK，release 默认不带 price 字段，
                // 直接访问 adObject.price 会编译不过，需用反射并判空
                rewardAdObject = adObject

                // 根据业务时机展示广告
                showRewardAd()
            }

            override fun onAdLoadFailed(error: ATFOAdError) {
                Log.e("RewardAd", "激励视频广告加载失败: ${error.message}")
            }
        })
    }

    private fun showRewardAd() {
        val adObject = rewardAdObject ?: return

        if (!adObject.isValid()) {
            // 判断广告是否可用
            return
        }

        rewardShowing = true

        // 激励视频广告为全屏展示，传入当前 Activity 即可
        adObject.showAd(this, object : AdInteractionListener {
            override fun onAdShowSuccess() {
                Log.d("RewardAd", "激励视频广告展示成功")
            }

            override fun onAdShowFailed(errorCode: Int, errorMessage: String) {
                // 拉起超时 / 渲染失败后广告对象已作废，需重新 loadAd，不会再回调 onAdClosed
                rewardShowing = false
                Log.e("RewardAd", "激励视频广告展示失败: $errorCode, $errorMessage")
            }

            override fun onAdClicked() {
                Log.d("RewardAd", "激励视频广告被点击")
            }

            override fun onAdClosed() {
                // 页面已关闭、校验结论已到（或按放弃处理），此时销毁安全
                rewardShowing = false
                rewardAdObject?.destroy()
                rewardAdObject = null
                Log.d("RewardAd", "激励视频广告被关闭")
            }

            override fun onAdRenderFail(errorCode: Int, errorMessage: String) {
                Log.d("RewardAd", "❌ 激励视频广告renderFail")
            }

            override fun onAdExposed() {
                Log.d("RewardAd", "激励视频广告exposed")
            }

            override fun onReward() {
                // 只有三方广告源会回调这里（本地达标，无服务端校验）；API 激励不走本回调
                Log.d("RewardAd", "激励视频广告获得奖励（三方广告源）")
            }

            override fun onRewardVerified(result: RewardVerificationResult) {
                Log.d("RewardAd", "服务端校验通过: rewardId=${result.rewardId}, duplicate=${result.duplicate}")
            }

            override fun onRewardVerifyFailed(errorCode: Int, errorMessage: String) {
                Log.w("RewardAd", "服务端校验失败: $errorCode, $errorMessage")
            }

            override fun onRewardResult(verified: Boolean) {
                // 唯一发奖判定点，每个广告对象最多回调一次，宿主发奖须幂等
                Log.d("RewardAd", "发奖结果: verified=$verified")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // 展示期间（未收到 onAdClosed）不销毁：destroy() 按放弃处理，本次 onRewardResult 会永久丢失；
        // 这种情况由 onAdClosed 兜底销毁，避免广告对象泄漏
        if (!rewardShowing) {
            rewardAdObject?.destroy()
            rewardAdObject = null
        }
    }
}
```

#### 发奖结果与服务端校验（SSV）

> 从 2.12.0 起，发奖判定统一看 `onRewardResult(verified)`；`onReward()` 只在三方广告源达标时回调，**不要只靠 `onReward()` 发奖**，否则 API（core 自渲染）激励会漏发。
>
> **Java 接入方注意**：`onRewardVerified` / `onRewardVerifyFailed` / `onRewardResult` 在 Kotlin 侧有默认实现，Java 没有。从 2.11.1 起 `AdInteractionListener` 新增了这三个方法，Java 实现的监听器不补齐会编译不过。

广告位在聚合平台开启 SSV（服务端奖励校验）后，服务端会在 RTB 自渲染激励视频的响应里下发一次性发奖凭证 `reward_verification`。SDK 在本地达到发奖条件时把凭证提交给服务端确认，拿到结论后才回调 `onRewardResult`。接入方只需要设置一个字段：

```kotlin
ATFOAdSlot.Builder()
    .adType(AdType.REWARD)
    .slotId("your_reward_slot_id")
    // 透传给服务端的 user_key，用于媒体后端对账；不设置时回退初始化配置的 userId。
    // 必须是稳定的业务用户标识（如账号 ID），不要用设备号 / 会话 ID
    .rewardUserKey("业务用户标识")
    .build()
```

四个回调的语义：

| 回调 | 触发时机 |
|---|---|
| `onReward()` | 仅三方广告源：本地达到发奖条件时回调。API 激励不回调 |
| `onRewardVerified(result)` | 服务端确认成功。`result` 带 `rewardId` / `verified` / `duplicate` / `verifiedAt`（毫秒） |
| `onRewardVerifyFailed(errorCode, errorMessage)` | 凭证无效、已过期，或重试最终失败 |
| `onRewardResult(verified)` | 本条发奖链路的最终结论，在上述奖励相关回调之后回调**一次**。API 激励唯一的发奖信号 |

`onRewardVerifyFailed` 的错误码：

| 常量 | 数值 | 含义 |
|---|---|---|
| `ATFORewardVerifyError.ERROR_NETWORK` | 1101 | 网络异常 / 超时 408 / 限流 429 / 5xx，重试（立即 → 2s → 2s → 6s → 9s，30s 后不再发起）已耗尽；或 2xx 响应体异常 |
| `ATFORewardVerifyError.ERROR_REJECTED` | 1102 | 凭证被服务端拒绝：400 / 其他终局 4xx（408、429 除外），或 2xx `verified=false` |
| `ATFORewardVerifyError.ERROR_EXPIRED` | 1103 | 凭证已过期（服务端 410，或 SDK 每次尝试前按设备时钟本地预判过期） |

关键约定：

- **一切失败统一收尾**：`onRewardResult(false)`，页面只弹「播放完成」，不出现任何奖励文案，宿主不得发奖。
- **未下发凭证**（老服务端 / 未开 SSV / 签发失败）：不发校验请求，也不会有 `onRewardVerified` / `onRewardVerifyFailed`，达到条件时直接 `onRewardResult(false)`，页面照常展示「已获得奖励」。
- **发奖条件未达到**（中途 ✕ 放弃）：不发起校验，也不回调 `onRewardResult`。
- **`destroy()` 按放弃处理**：校验未出结论时 destroy，结论不再投递宿主；发奖条件未到达时 destroy，本次不发起校验。所以**展示期间（未收到 `onAdClosed`）不要在 `onDestroy()` 里 destroy**，示例代码用 `rewardShowing` 标志处理。
- **`onRewardResult` 每个广告对象只回调一次**，宿主发奖逻辑仍须幂等。
- **强防作弊以后端为准**：服务端会把校验记录异步回调给媒体后端，按 `user_key + rewardId` 发奖；客户端回调只用于决定发奖时机。
- 页面表现（顶部奖励提示、结束层文案、✕ 关闭时机）全部由 SDK 控制，宿主不需要自绘。

### 通知广告

> 通知广告是 ATFOpenAdSDK 提供的一种特殊信息流广告：客户端按信息流广告（AdType.FEED）拉取，由 SDK 模板渲染并负责展示。
>
> 后台配置（两项缺一不可）：
>
> 1. 代码位的渲染方式选「ATFO 模板渲染」
> 2. 模板选「信息流-通知广告」
>
> 如果需要使用非 ATFO 广告平台的代码位，需要在对应的平台后台创建代码位，选择 FEED 流广告类型，自渲染即可。
>
> 展示入口固定用 `showAd(activity, listener)`：通知栏卡片由 SDK 挂到当前 Activity 的 decorView 顶层，从屏幕顶部滑入，无需宿主提供容器。

```kotlin
class NotifyAdActivity : AppCompatActivity() {

    private var notifyAdObject: ATFOAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notify_ad)


        // 加载信息流广告
        loadNotifyAd()
    }

    private fun loadNotifyAd() {
        val adSlot = ATFOAdSlot.Builder()
            .adType(AdType.FEED)
            .timeout(8000)                    // 可选；超时以管理后台下发为准，建议不配置（见「注意事项」）
            .slotId("") // 代码位 Id（向对接运营获取）
            .size(1000, 1000)
            .extras(mapOf("scene" to "notify_ad"))
            .build()

        ATFOAdLoader(this, true).loadAd(adSlot, object : ATFOAdLoader.AdLoadListener {
            override fun onAdLoadSuccess(adObject: ATFOAd) {
                Log.d("NotifyAd", "通知广告加载成功")
                Log.d("NotifyAd", "广告ID: ${adObject.adId}")
                // 单位是分。取价需「带价格」版本 SDK（release 默认无 price 字段），用反射取值，见下文 AdPriceUtils。
                Log.d("NotifyAd", "价格: "${AdPriceUtils.getPrice(adObject)})

                notifyAdObject = adObject

                // 显示广告
                showNotifyAd()
            }

            override fun onAdLoadFailed(error: ATFOAdError) {
                Log.e("NotifyAd", "通知广告加载失败: ${error.message}")
            }
        })
    }

    private fun showNotifyAd() {
        val adObject = notifyAdObject ?: return

        if (!adObject.isValid()) {
            // 判断广告是否可用
            return
        }

        adObject.showAd(this, object : AdInteractionListener {
            override fun onAdShowSuccess() {
                Log.d("NotifyAd", "通知广告展示成功")
            }

            override fun onAdShowFailed(errorCode: Int, errorMessage: String) {
                Log.e("NotifyAd", "通知广告展示失败: $errorMessage")
            }

            override fun onAdClicked() {
                Log.d("NotifyAd", "通知广告被点击")
            }

            override fun onAdClosed() {
                Log.d("NotifyAd", "通知广告被关闭")
            }

            override fun onAdRenderFail(errorCode: Int, errorMessage: String) {
                Log.d("NotifyAd", "❌ 通知广告renderFail")
            }

            override fun onAdExposed() {
                Log.d("NotifyAd", "通知广告exposed")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        notifyAdObject?.destroy()
    }
}
```

## 四、竞价回传

当媒体使用 GroMore、TopOn 等第三方聚合平台，且本 SDK 作为竞价渠道参与竞价时，需通过竞价回传功能向本 SDK 通知竞价结果。

找不到竞价回传方法请联系运营。

参与竞价（Bidding）的广告位，在竞价结束后需要把竞胜/竞败结果回传给 SDK，用于计费和效果归因。

### 1、竞胜回传

```kotlin
val winPrice = 100
val secondPrice = 20
val secondPriceWinner = AdSource.QM // 见广告渠道映射
val currency = "CNY" // 币种，默认 CNY
val mBidFeedback = BidFeedback.buildWinFeedback(
    winPrice = winPrice,
    secondPrice = secondPrice,
    secondPriceWinner = secondPriceWinner,
    currency = currency,
)
mATFOAd.sendWinNotification(mBidFeedback)
```

### 2、竞败回传

```kotlin
val winPrice = 100
val lossReason = BidFeedback.LOSS_REASON_LOW_PRICE
val winner = AdSource.QM // 见广告渠道映射
val currency = "CNY" // 币种，默认 CNY
val mBidFeedback = BidFeedback.buildLossFeedback(
    winPrice = winPrice,
    lossReason = lossReason,
    winner = winner,
    currency = currency,
)
adObject.sendLossNotification(mBidFeedback)
```

### 3、广告渠道映射

| `AdSource` | 广告平台    |
|------------|---------|
| `GRM`      | gromore |
| `GDT`      | 广点通     |
| `BAIDU`    | 百度      |
| `KS`       | 快手      |
| `MS`       | 美数      |
| `YK`       | 优酷      |
| `QM`       | 趣盟      |
| `BZ`       | 倍孜      |
| `NOAH`     | 汇川      |
| `SIGMOB`   | sigmob  |
| `WM`       | 旺脉      |

其他平台（如泛为 FW 等未在上表列出的渠道），`AdSource.fromString` 返回 `null`，竞价回传时按 `null` 处理

## 五、清理广告缓存

广告加载成功后会缓存在本地，需要主动清理时使用以下方法（`com.atfo.ad.core.ATFOADSDKManager`）。清理方法自 `2.10.0` 起提供。

### 1、清空全部广告缓存

```kotlin
val clearedCount = ATFOADSDKManager.getInstance().clearAllCachedAds()
```

### 2、按广告位清理缓存

```kotlin
val clearedCount = ATFOADSDKManager.getInstance().clearCachedAds(slotId)
```

返回值均为本次清理的广告数量。

## 六、生命周期管理

### 广告对象销毁

在 Activity 或 Fragment 销毁时，必须调用广告对象的销毁方法：

```kotlin
override fun onDestroy() {
    super.onDestroy()

    // 销毁广告对象
    adObject?.destroy()
}
```

### 取消正在加载的广告

如果需要取消正在加载的广告：

```kotlin
// 取消加载
loader.cancelLoad()
```

## 七、错误处理

### 错误码说明

| 错误码 | 常量 | 说明 |
|---|---|---|
| 1001 | `ATFOAdError.ERROR_NETWORK` | 网络错误，可以重试 |
| 1002 | `ATFOAdError.ERROR_NO_AD` | 无广告返回，显示默认内容 |
| 1003 | `ATFOAdError.ERROR_TIMEOUT` | 超时 |
| 1004 | `ATFOAdError.ERROR_INVALID_SLOT` | 无效广告位，检查配置 |
| 1005 | `ATFOAdError.ERROR_SDK_NOT_INIT` | SDK 未初始化，检查初始化流程 |
| 1006 | `ATFOAdError.ERROR_LOAD_FAILED` | 加载失败（同一 slotId 在途重复加载也会以此拒绝） |
| 1007 | `ATFOAdError.ERROR_RENDER_FAILED` | 渲染失败 |
| 1008 | `ATFOAdError.ERROR_THIRD_PARTY` | 三方广告源错误 |
| 1009 | `ATFOAdError.ERROR_BIDDING_FAILED` | 竞价失败 |
| 1010 | `ATFOAdError.ERROR_CONFIG_FAILED` | 配置获取失败 |
| 1011 | `ATFOAdError.ERROR_INVALID_RENDER_TYPE` | 渲染类型无效 |

处理示例：

```kotlin
override fun onAdLoadFailed(error: ATFOAdError) {
    when (error.code) {
        ATFOAdError.ERROR_NETWORK -> {
            // 网络错误，可以重试
            retryLoadAd()
        }
        ATFOAdError.ERROR_NO_AD -> {
            // 无广告返回，显示默认内容
            showDefaultContent()
        }
        ATFOAdError.ERROR_TIMEOUT -> {
            // 超时，可以增加超时时间重试
            loadAdWithLongerTimeout()
        }
        ATFOAdError.ERROR_INVALID_SLOT -> {
            // 无效广告位，检查配置
            Log.e("AdError", "广告位配置错误")
        }
        ATFOAdError.ERROR_SDK_NOT_INIT -> {
            // SDK未初始化，检查初始化流程
            Log.e("AdError", "SDK未初始化")
        }
        else -> {
            Log.e("AdError", "未知错误: ${error.message}")
        }
    }
}
```

## 八、隐私数据获取说明

### 1、SDK 收集的信息

| 参数 | 说明 |
|---|---|
| 操作系统 | 设备的操作系统（如：Android/iOS） |
| 系统版本名 | 设备的系统版本名 |
| 系统版本号 | 设备的系统版本号 |
| 应用包名 | 当前应用的包名 |
| 应用版本名 | 当前应用版本名 |
| 设备品牌 | 移动设备的品牌名称 |
| 设备型号 | 移动设备的型号名称 |
| 分辨率 | 设备的屏幕分辨率 |
| 屏幕方向 | 设备的屏幕方向（如：1：竖屏，2：横屏） |
| 网络类型 | 网络类型（如：WiFi、3G、4G、5G），可在初始化的时候关闭 |
| 移动网络代码 | 移动设备网络代码 |
| 移动国家代码 | 移动设备国家代码 |
| 系统语言 | 系统语言（如：zh-CN） |
| 时区 | 时区（如：GMT+08:00） |
| User Agent | User Agent 信息 |
| AndroidId | 设备广告标识符 |

## 九、联调排查

> 为方便联调，SDK 内置了日志导出页面。遇到广告加载、展示异常等问题时，可先导出日志，发给我们研发同学分析定位。
>
> 打开日志导出页面的命令：
>
> ```bash
> adb shell am start -n 你的App包名/com.atfo.ad.debug.ATFODebugActivity
> ```
>
> 在页面中导出日志后，把日志文件发给对接的研发即可。
>
> 也可以在自己应用的调试页里直接调用（代码方式，不需要知道 Activity 全名）：
>
> ```kotlin
> import com.atfo.ad.debug.DebugManager
>
> DebugManager.openDebugActivity(context)   // SDK 初始化时 debug=true 才可用
> ```

## 十、注意事项

1. **生命周期管理**：在 Activity 销毁时调用 `adObject.destroy()`
2. **网络权限**：确保添加了网络权限
3. **调试模式**：开发阶段建议开启 `debug(true)`
4. **三方 SDK**：非聚合模式仅含 ATFO 自有广告源；如需三方广告源或 GroMore 聚合，参见[聚合接入指南](aggregation.md)
5. **广告显示**：广告加载成功后需要调用 `showAd()` 方法显示广告
6. **错误处理**：实现完善的错误处理和重试机制
7. **内存管理**：及时销毁广告对象，避免内存泄漏
8. **超时管理**：超时时间以管理后台下发为准（未下发时默认 5000ms），建议不在本地通过 `timeout()` 配置

## 十一、技术支持

- **技术支持**：support@atfo.com
- **版本**：2.12.0
- **最低支持**：Android API 23（Android 6.0）

## 十二、SDK 涉及的三方库版本（若存在不兼容等问题，请联系开发进行定制化修改）

```toml
[versions]
# 核心版本
agp = "7.0.4"
kotlin = "1.5.21"

# AndroidX
coreKtx = "1.3.2"
appcompat = "1.2.0"
constraintlayout = "2.1.4"

# 网络和媒体
glide = "4.11.0"
okhttp = "4.9.0"
gson = "2.8.6"

# 协程和工具
coroutines = "1.5.2"
mmkv = "1.2.14"

# Architecture components
room = "2.3.0"

[libraries]

# AndroidX核心
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
androidx-cardview = { group = "androidx.cardview", name = "cardview", version = "1.0.0" }

# 协程
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }

# 媒体
glide = { group = "com.github.bumptech.glide", name = "glide", version.ref = "glide" }

# 网络（SDK 仅直接使用 OkHttp；接入方需引入 okhttp）
okhttp-core = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }

# 存储
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }
mmkv = { group = "com.tencent", name = "mmkv-static", version.ref = "mmkv" }

# 数据库和后台
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```
