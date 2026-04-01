package com.example.bilidynamic1.data.repository

import android.util.Log
import com.example.bilidynamic1.data.manager.UserManager
import com.example.bilidynamic1.data.model.DynamicItem
import com.example.bilidynamic1.data.model.NetworkClient
import okhttp3.Request
import org.json.JSONObject

object DynamicRepository {

    private const val TAG = "DynamicRepository"

    /**
     * 加载视频动态列表（分页）
     * @param offset 分页偏移量，第一页传空字符串
     * @return 返回结果包含数据列表和下一页的offset
     */
    fun loadVideoDynamicsPage(offset: String = ""): LoadResult {
        val result = mutableListOf<DynamicItem>()
        var nextOffset = ""

        val cookie = UserManager.getCookie()
        if (cookie.isEmpty()) {
            Log.e(TAG, "Cookie is empty, please login first.")
            return LoadResult(emptyList(), "", false)
        }

        try {
            // 1. 构建 URL
            val url = buildFeedUrl(offset)

            // 2. 构建 Request
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", NetworkClient.USER_AGENT)
                .header("Referer", "https://www.bilibili.com")
                .header("Cookie", cookie)
                .get()
                .build()

            // 3. 发起请求
            val response = NetworkClient.client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                Log.e(TAG, "Network error: code=${response.code}")
                return LoadResult(emptyList(), "", false)
            }

            // 4. 解析 JSON
            val root = JSONObject(body)
            if (root.optInt("code") != 0) {
                Log.e(TAG, "API Error: ${root.optString("message")}")
                return LoadResult(emptyList(), "", false)
            }

            val data = root.optJSONObject("data") ?: return LoadResult(emptyList(), "", false)
            val items = data.optJSONArray("items")

            // 如果没有数据，说明到底了
            if (items == null || items.length() == 0) {
                return LoadResult(emptyList(), "", true) // hasMore = false
            }

            // 5. 遍历解析 Item
            for (i in 0 until items.length()) {
                try {
                    val item = items.getJSONObject(i)
                    parseDynamicItem(item)?.let { dynamicItem ->
                        result.add(dynamicItem)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Parse item failed: ${e.message}")
                }
            }

            // 6. 获取下一页的 offset
            nextOffset = data.optString("offset", "")
            val hasMore = nextOffset.isNotEmpty()

            return LoadResult(result, nextOffset, hasMore)

        } catch (e: Exception) {
            Log.e(TAG, "Load dynamics failed", e)
            return LoadResult(emptyList(), "", false)
        }
    }

    /**
     * 解析单个动态项
     */
    private fun parseDynamicItem(item: JSONObject): DynamicItem? {
        val modules = item.optJSONObject("modules") ?: return null
        val moduleAuthor = modules.optJSONObject("module_author") ?: return null

        // ✅ 修复 1：获取真正的作者 UID (mid)
        val uid = moduleAuthor.optLong("mid", 0L)
        val uname = moduleAuthor.optString("name", "UP主")
        val avatar = moduleAuthor.optString("face", "")
        val pubTime = moduleAuthor.optLong("pub_ts", 0L)

        val moduleDynamic = modules.optJSONObject("module_dynamic") ?: return null
        val major = moduleDynamic.optJSONObject("major") ?: return null
        if (major.optString("type") != "MAJOR_TYPE_ARCHIVE") return null

        val archive = major.optJSONObject("archive") ?: return null
        val bvid = archive.optString("bvid", "")
        if (bvid.isEmpty()) return null

        // 注意：有些动态 JSON 里可能没有 cid，需要通过 fetchCid 获取
        val cid = archive.optLong("cid", 0L)

        return DynamicItem(
            uid = uid,
            uname = uname,
            avatar = avatar,
            pubTime = pubTime,
            title = archive.optString("title", "未命名"),
            cover = archive.optString("cover", ""),
            bvid = bvid,
            aid = archive.optLong("aid", 0L),
            cid = cid
        )
    }

    /**
     * ✅ 修复 2：补全 fetchCid 的 Header 校验
     */
    fun fetchCid(bvid: String): Long {
        val url = "https://api.bilibili.com/x/web-interface/view?bvid=$bvid"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", NetworkClient.USER_AGENT)
            .header("Referer", "https://www.bilibili.com") // ⬅️ 必须有这个
            .header("Cookie", UserManager.getCookie())    // ⬅️ 建议加上
            .get()
            .build()

        try {
            NetworkClient.client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return 0L
                val json = JSONObject(body)
                if (json.optInt("code") == 0) {
                    val data = json.optJSONObject("data")
                    val realCid = data?.optLong("cid") ?: 0L
                    Log.d(TAG, "成功获取真实 CID: $realCid")
                    return realCid
                } else {
                    Log.e(TAG, "获取 CID 失败: ${json.optString("message")}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchCid 网络异常", e)
        }
        return 0L
    }

    private fun buildFeedUrl(offset: String): String {
        val base = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all?timezone_offset=-480&type=video"
        return if (offset.isNotEmpty()) "$base&offset=$offset" else base
    }

    data class LoadResult(
        val items: List<DynamicItem>,
        val nextOffset: String,
        val hasMore: Boolean
    )
}