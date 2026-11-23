package com.atfo.ad.demo.reward

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.atfo.ad.core.ATFOAdLoader
import com.atfo.ad.demo.config.DemoConfig
import com.atfo.ad.demo.databinding.ActivityRewardAdBinding
import com.atfo.ad.demo.utils.AdCategoryUtils
import com.atfo.ad.demo.utils.AdPriceUtils
import com.atfo.ad.demo.utils.applyStatusBarPadding
import com.atfo.ad.demo.utils.lightStatusBar
import com.atfo.ad.listener.ATFOAdError
import com.atfo.ad.listener.AdInteractionListener
import com.atfo.ad.model.ATFOAd
import com.atfo.ad.model.ATFOAdSlot
import com.atfo.ad.model.AdType
import com.atfo.ad.reward.ATFORewardVerifyError
import com.atfo.ad.reward.RewardVerificationResult

/**
 * 激励视频：全屏展示。发奖判定统一看 [com.atfo.ad.listener.AdInteractionListener.onRewardResult]：
 * 三方广告源没有服务端校验，`verified` 为 false；API（core 自渲染）激励在开启 SSV 后
 * 由服务端确认通过才为 true。`onReward()` 只在三方广告源达标时回调，**不要**只靠它发奖。
 * 进页面不自动加载，点「加载广告」才发起请求，加载成功后点「展示广告」。
 * 本页只负责展示与回调，每个回调都打日志并追加到页面底部的 ConsoleView，不做任何跳转。
 */
class RewardAdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardAdBinding
    private var loader: ATFOAdLoader? = null
    private var rewardAdObject: ATFOAd? = null

    /**
     * 激励页展示中（showAd 后到 onAdClosed 前）。展示期间销毁广告对象按「放弃奖励」处理：
     * 已提交的服务端校验请求会继续执行，但 onRewardVerified / onRewardVerifyFailed / onRewardResult
     * 都不再投递，本次奖励会永久丢失。所以 [onDestroy] 里展示中不 destroy，交给 onAdClosed 兜底。
     */
    private var rewardShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lightStatusBar()
        binding = ActivityRewardAdBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnLoad.setOnClickListener { loadRewardAd() }
        binding.btnShow.setOnClickListener { showRewardAd() }
        binding.btnBack.setOnClickListener { finish() }
        binding.buttonBar.applyStatusBarPadding()
    }

    private fun loadRewardAd() {
        // 重新加载前先释放上一次的广告对象，并取消在途请求
        releaseAd()
        loader?.cancelLoad()

        val adSlot = ATFOAdSlot.Builder()
            .adType(AdType.REWARD)
            .slotId(DemoConfig.slotIdOf(AdType.REWARD))
            .size(720, 1280)
            .extras(mapOf("scene" to "reward"))
            // 服务端奖励校验（SSV）对账用的 user_key：必须是稳定的业务用户标识（如账号 ID），
            // 不要用设备号 / 会话 ID；不设置时 SDK 回退初始化配置里的 userId
            .rewardUserKey(DemoConfig.userKey)
            .build()

        val adLoader = ATFOAdLoader(this, DemoConfig.debug)
        loader = adLoader
        adLoader.loadAd(adSlot, object : ATFOAdLoader.AdLoadListener {
            override fun onAdLoadSuccess(adObject: ATFOAd) {
                val msg = "激励视频加载成功, adId=${adObject.adId}, price=${AdPriceUtils.getPrice(adObject)}, 行业=${AdCategoryUtils.describe(adObject)}"
                logToConsole(msg)
                rewardAdObject = adObject
            }

            override fun onAdLoadFailed(error: ATFOAdError) {
                val msg = "激励视频加载失败, code=${error.code}, message=${error.message}"
                logToConsole(msg, Log.ERROR)
            }
        })
    }

    private fun showRewardAd() {
        val adObject = rewardAdObject
        if (adObject == null) {
            val msg = "尚未加载到广告，请先点击「加载广告」"
            logToConsole(msg, Log.WARN)
            return
        }
        if (!adObject.isValid()) {
            val msg = "激励视频已失效，请重新加载"
            logToConsole(msg, Log.WARN)
            return
        }
        rewardShowing = true
        adObject.showAd(this@RewardAdActivity, object : AdInteractionListener {
            override fun onAdShowSuccess() {
                val msg = "激励视频展示成功"
                logToConsole(msg)
            }

            override fun onAdShowFailed(errorCode: Int, errorMessage: String) {
                // 拉起超时 / 渲染失败时广告对象已作废，需重新 loadAd；不会再回调 onAdClosed
                rewardShowing = false
                val msg = "激励视频展示失败, code=$errorCode, message=$errorMessage"
                logToConsole(msg, Log.ERROR)
            }

            override fun onAdClicked() {
                val msg = "激励视频被点击"
                logToConsole(msg)
            }

            override fun onAdClosed() {
                // 页面已关闭、校验结论已到（或按放弃处理），此时销毁安全
                rewardShowing = false
                releaseAd()
                val msg = "激励视频被关闭"
                logToConsole(msg)
            }

            override fun onAdRenderFail(errorCode: Int, errorMessage: String) {
                val msg = "激励视频渲染失败, code=$errorCode, message=$errorMessage"
                logToConsole(msg, Log.ERROR)
            }

            override fun onAdExposed() {
                val msg = "激励视频曝光"
                logToConsole(msg)
            }

            override fun onReward() {
                // 只有三方广告源会回调这里（本地达标，无服务端校验）；API 激励不走本回调。
                // 发奖统一看 onRewardResult，不要在这里直接发奖，否则未开 SSV 的 API 激励会漏发
                val msg = "激励视频达标（三方广告源），等待 onRewardResult"
                logToConsole(msg)
            }

            override fun onRewardVerified(result: RewardVerificationResult) {
                // 服务端校验通过：rewardId 用于后端对账，duplicate=true 表示该凭证之前已校验过
                val msg = "服务端校验通过, rewardId=${result.rewardId}, duplicate=${result.duplicate}, verifiedAt=${result.verifiedAt}"
                logToConsole(msg)
            }

            override fun onRewardVerifyFailed(errorCode: Int, errorMessage: String) {
                val reason = when (errorCode) {
                    ATFORewardVerifyError.ERROR_NETWORK -> "网络异常/重试耗尽"
                    ATFORewardVerifyError.ERROR_REJECTED -> "凭证被拒绝"
                    ATFORewardVerifyError.ERROR_EXPIRED -> "凭证已过期"
                    else -> "未知"
                }
                val msg = "服务端校验失败($reason), code=$errorCode, message=$errorMessage"
                logToConsole(msg, Log.WARN)
            }

            override fun onRewardResult(verified: Boolean) {
                // 每个广告对象最多回调一次，是唯一的发奖判定点。宿主侧发奖逻辑必须幂等。
                // verified=true 只代表服务端确认（SSV）；未开 SSV 时为 false，需按业务口径决定是否发奖
                val msg = "发奖结果 verified=$verified，真实业务在此按 verified 发放奖励"
                logToConsole(msg)
            }
        })
    }

    /** 打日志并追加到 ConsoleView（各回调统一走这里）；线程切换、格式化、清空都由 ConsoleView 负责。 */
    private fun logToConsole(msg: String, priority: Int = Log.DEBUG) {
        binding.consoleView.log(msg, priority, "ATFO${this::class.java.simpleName}")
    }

    /** 释放当前广告对象；重新加载前与 onDestroy 调用。 */
    private fun releaseAd() {
        rewardAdObject?.destroy()
        rewardAdObject = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // 展示期间（未收到 onAdClosed）不销毁：destroy() 按放弃处理，本次 onRewardResult 会永久丢失。
        // 这种情况由 onAdClosed 兜底销毁，避免广告对象泄漏
        if (!rewardShowing) releaseAd()
        loader?.cancelLoad()
        loader = null
    }
}
