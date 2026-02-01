package com.example.bilidynamic1.data.manager

import android.util.Log
import com.example.bilidynamic1.data.manager.UserManager
import com.example.bilidynamic1.data.model.NetworkClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.*
import kotlin.collections.LinkedHashMap

object WbiUtil {
    // 缓存 Key
    var imgKey: String? = null
    var subKey: String? = null

    // 混淆表 (保持不变)
    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52
    )

    // 1. 获取并刷新 Key (使用 OkHttp)
    fun refreshKeys() {
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/web-interface/nav")
            .header("User-Agent", NetworkClient.USER_AGENT)
            .header("Cookie", UserManager.getCookie()) // 必须带 Cookie
            .get()
            .build()

        try {
            // execute() 是同步阻塞方法，必须在子线程调用
            NetworkClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return

                val jsonStr = response.body?.string() ?: return
                val json = JSONObject(jsonStr)

                // 简单的 JSON 解析
                val data = json.optJSONObject("data") ?: return
                val wbiImg = data.optJSONObject("wbi_img") ?: return

                val imgUrl = wbiImg.getString("img_url")
                val subUrl = wbiImg.getString("sub_url")

                imgKey = imgUrl.substringAfterLast("/").substringBefore(".")
                subKey = subUrl.substringAfterLast("/").substringBefore(".")

                Log.d("WbiUtil", "Keys refreshed: $imgKey, $subKey")
            }
        } catch (e: Exception) {
            Log.e("WbiUtil", "Failed to refresh keys", e)
        }
    }

    // 2. 计算 Mixin Key (保持不变)
    private fun getMixinKey(imgKey: String, subKey: String): String {
        val rawWbiKey = imgKey + subKey
        val result = StringBuilder()
        for (index in MIXIN_KEY_ENC_TAB) {
            if (index < rawWbiKey.length) result.append(rawWbiKey[index])
        }
        return result.toString().substring(0, 32)
    }

    // 3. 对参数进行签名 (保持不变，纯逻辑)
    fun sign(params: Map<String, String>): Map<String, String> {
        if (imgKey == null || subKey == null) {
            Log.e("WbiUtil", "Keys not loaded! Call refreshKeys() first.")
            return params
        }

        val mixinKey = getMixinKey(imgKey!!, subKey!!)
        val currTime = (System.currentTimeMillis() / 1000).toString()

        val sortedParams = TreeMap(params)
        sortedParams["wts"] = currTime

        val sj = StringBuilder()
        for ((k, v) in sortedParams) {
            // 手动编码以匹配 B 站逻辑
            val encodedValue = java.net.URLEncoder.encode(v, "UTF-8")
                .replace("+", "%20").replace("*", "%2A").replace("%7E", "~")
            if (sj.isNotEmpty()) sj.append("&")
            sj.append("$k=$encodedValue")
        }

        val rawString = sj.toString() + mixinKey
        val wRid = md5(rawString)

        val finalParams = LinkedHashMap(sortedParams)
        finalParams["w_rid"] = wRid
        return finalParams
    }

    private fun md5(s: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(s.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}