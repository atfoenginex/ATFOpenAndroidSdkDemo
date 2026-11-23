package com.atfo.ad.demo.utils

import com.atfo.ad.model.ATFOAd

/**
 * Demo 专用：读取广告行业分类标签。
 *
 * 2.13.0 起 ATFOAd 新增 getAdFirstCategory() / getAdSecondCategory()，
 * 返回本次广告的一级 / 二级行业分类，均可能为 null（未下发时为 null）。
 */
object AdCategoryUtils {

    /** 拼接一/二级行业分类文案：都有取「一级/二级」，只有其一取其一，都为 null 返回「未下发」。 */
    fun describe(ad: ATFOAd): String {
        val first = ad.getAdFirstCategory()
        val second = ad.getAdSecondCategory()
        return if (first == null && second == null) {
            "未下发"
        } else {
            listOfNotNull(first, second).joinToString("/")
        }
    }
}
