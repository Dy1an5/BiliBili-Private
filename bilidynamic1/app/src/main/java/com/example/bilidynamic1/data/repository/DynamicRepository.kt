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
     * 加载视频动态列表
     * @param maxCount 期望获取的条数
     */
    fun loadVideoDynamics(maxCount: Int = 100): List<DynamicItem> {
        val result = mutableListOf<DynamicItem>()
        var offset = ""

        val cookie = UserManager.getCookie()
        if (cookie.isEmpty()) {
            Log.e(TAG, "Cookie is empty, please login first.")
            return emptyList()
        }

        try {
            // 循环分页获取，直到凑够数量或没有更多数据
            while (result.size < maxCount) {
                // 1. 构建 URL：添加 type=video 参数，只请求视频动态
                val url = buildFeedUrl(offset)

                // 2. 构建 Request：使用统一的 User-Agent
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", NetworkClient.USER_AGENT) // 关键！
                    .header("Referer", "https://www.bilibili.com")
                    .header("Cookie", cookie)
                    .get()
                    .build()

                // 3. 发起请求：使用单例 Client
                val response = NetworkClient.client.newCall(request).execute()
                val body = response.body?.string()

                if (!response.isSuccessful || body == null) {
                    Log.e(TAG, "Network error: code=${response.code}")
                    break
                }

                // 4. 解析 JSON
                val root = JSONObject(body)
                if (root.optInt("code") != 0) {
                    Log.e(TAG, "API Error: ${root.optString("message")}")
                    break
                }

                val data = root.optJSONObject("data") ?: break
                val items = data.optJSONArray("items")

                // 如果当前页没有数据，说明到底了
                if (items == null || items.length() == 0) break

                // 5. 遍历 Item
                for (i in 0 until items.length()) {
                    if (result.size >= maxCount) break

                    try {
                        val item = items.getJSONObject(i)

                        // 提取动态 ID (作为唯一标识)
                        val idStr = item.optString("id_str", "0")
                        val uid = idStr.toLongOrNull() ?: 0L

                        val modules = item.optJSONObject("modules") ?: continue
                        val moduleDynamic = modules.optJSONObject("module_dynamic") ?: continue

                        // 双重检查：确保是视频类型 (MAJOR_TYPE_ARCHIVE)
                        val major = moduleDynamic.optJSONObject("major") ?: continue
                        if (major.optString("type") != "MAJOR_TYPE_ARCHIVE") continue

                        val archive = major.optJSONObject("archive") ?: continue

                        // --- 提取核心数据 ---
                        val bvid = archive.optString("bvid", "")
                        // B站 Feed 流经常不返回 cid，这里允许为 0，点击时再获取
                        val cid = archive.optLong("cid", 0L)
                        val aid = archive.optLong("aid", 0L)

                        val title = archive.optString("title", "未命名视频")
                        val cover = archive.optString("cover", "")

                        // --- 提取作者信息 ---
                        val moduleAuthor = modules.optJSONObject("module_author")
                        val uname = moduleAuthor?.optString("name", "UP主") ?: ""
                        val avatar = moduleAuthor?.optString("face", "") ?: ""
                        val pubTime = moduleAuthor?.optLong("pub_ts", 0L) ?: 0L

                        // 只要有 bvid 就可以展示，cid 可以后续补全
                        if (bvid.isNotEmpty()) {
                            result.add(
                                DynamicItem(
                                    uid = uid,
                                    uname = uname,
                                    avatar = avatar,
                                    pubTime = pubTime,
                                    title = title,
                                    cover = cover,
                                    bvid = bvid,
                                    aid = aid,
                                    cid = cid // 注意：这里可能是 0
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Parse item failed: ${e.message}")
                    }
                }

                // 6. 准备下一页
                val nextOffset = data.optString("offset", "")
                if (nextOffset.isEmpty()) break
                offset = nextOffset
            }

        } catch (e: Exception) {
            Log.e(TAG, "Load dynamics failed", e)
        }

        return result
    }

    /**
     * 辅助方法：如果列表数据的 cid 为 0，调用此方法获取真实 cid
     * 用于 Fragment 点击事件中
     */
    fun fetchCid(bvid: String): Long {
        val url = "https://api.bilibili.com/x/web-interface/view?bvid=$bvid"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", NetworkClient.USER_AGENT)
            .get()
            .build()

        try {
            NetworkClient.client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return 0L
                val json = JSONObject(body)
                if (json.optInt("code") == 0) {
                    return json.optJSONObject("data")?.optLong("cid") ?: 0L
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }

    private fun buildFeedUrl(offset: String): String {
        // type=video 是关键，过滤非视频动态
        val base = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all?timezone_offset=-480&type=video"
        return if (offset.isNotEmpty()) "$base&offset=$offset" else base
    }
}