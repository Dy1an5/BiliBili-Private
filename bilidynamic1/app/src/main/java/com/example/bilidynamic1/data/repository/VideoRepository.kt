package com.example.bilidynamic1.data.repository

import com.example.bilidynamic1.data.manager.UserManager
import com.example.bilidynamic1.data.manager.WbiUtil
import com.example.bilidynamic1.data.model.NetworkClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONObject

// 数据类保持不变
data class PlayableMedia(
    val videoUrl: String,
    val audioUrl: String,
    val userAgent: String = NetworkClient.USER_AGENT,
    val referer: String = "https://www.bilibili.com"
)

object VideoRepository {

    // 在子线程调用
    fun fetchPlayUrl(bvid: String, cid: Long): PlayableMedia? {
        // 1. 确保 Key 存在
        if (WbiUtil.imgKey == null) {
            WbiUtil.refreshKeys()
        }

        // 2. 准备基础参数
        val params = mapOf(
            "bvid" to bvid,
            "cid" to cid.toString(),
            "qn" to "80",       // 1080P
            "fnval" to "4048",  // 4048 = DASH
            "fourk" to "1"
        )

        // 3. 获取带签名的参数 Map
        val signedParams = WbiUtil.sign(params)

        // 4. 使用 OkHttp 构建 URL
        val urlBuilder = "https://api.bilibili.com/x/player/playurl".toHttpUrlOrNull()?.newBuilder()
            ?: return null

        // 将签名后的参数填入 URL
        for ((key, value) in signedParams) {
            urlBuilder.addQueryParameter(key, value)
        }
        val finalUrl = urlBuilder.build()

        // 5. 构建请求
        val request = Request.Builder()
            .url(finalUrl)
            .header("User-Agent", NetworkClient.USER_AGENT)
            .header("Cookie", UserManager.getCookie()) // 必填
            .header("Referer", "https://www.bilibili.com")
            .get()
            .build()

        // 6. 发起请求并解析
        try {
            NetworkClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)

                // 检查 API 返回码
                if (json.optInt("code") != 0) {
                    return null
                }

                val data = json.optJSONObject("data") ?: return null
                val dash = data.optJSONObject("dash") ?: return null

                // 获取视频流 (baseUrl)
                // video 数组里可能有多个不同编码(AVC/HEVC)，取第一个通常是最高画质默认编码
                val videoArray = dash.optJSONArray("video")
                val videoUrl = videoArray?.optJSONObject(0)?.optString("baseUrl")

                // 获取音频流 (baseUrl)
                val audioArray = dash.optJSONArray("audio")
                val audioUrl = audioArray?.optJSONObject(0)?.optString("baseUrl")

                if (!videoUrl.isNullOrEmpty() && !audioUrl.isNullOrEmpty()) {
                    return PlayableMedia(videoUrl, audioUrl)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}