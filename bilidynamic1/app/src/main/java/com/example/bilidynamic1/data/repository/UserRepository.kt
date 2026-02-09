package com.example.bilidynamic1.data.repository

import android.util.Log
import com.example.bilidynamic1.data.model.UserStats
import com.example.bilidynamic1.data.model.NetworkClient
import com.example.bilidynamic1.data.manager.UserManager
import com.example.bilidynamic1.data.model.DynamicItem
import com.example.bilidynamic1.data.model.FavFolder
import com.example.bilidynamic1.data.model.UpItem
import okhttp3.Request
import org.json.JSONObject

data class UserProfile(
    val name: String,
    val face: String
)

object UserRepository {

    // 获取 MID (用户ID)，有些接口需要传 MID
    // 如果本地没有，需要先调 nav 接口获取，这里简化假设 Cookie 里能解析出 DedeUserID
    private fun getMyMid(): String {
        val cookie = UserManager.getCookie()
        // 简单的正则匹配从 Cookie 提取 DedeUserID
        val pattern = "DedeUserID=([^;]+)".toRegex()
        val match = pattern.find(cookie)
        return match?.groupValues?.get(1) ?: ""
    }

    /**
     * 获取用户统计数据 (关注、收藏、稍后再看)
     * 这是一个聚合请求，会依次调用3个接口
     */
    fun fetchUserStats(): UserStats {
        val mid = getMyMid()
        if (mid.isEmpty()) return UserStats()

        val cookie = UserManager.getCookie()

        // 1. 获取关注数 (调用 relation/stat)
        val followingCount = getFollowingCount(mid, cookie)

        // 2. 获取稍后再看数 (调用 history/toview)
        val watchLaterCount = getWatchLaterCount(cookie)

        // 3. 获取收藏数 (调用 fav/folder，把所有文件夹的媒体数相加)
        val favoritesCount = getFavoritesCount(mid, cookie)

        return UserStats(followingCount, favoritesCount, watchLaterCount)
    }

    fun fetchUserInfo(): UserProfile? {
        val url = "https://api.bilibili.com/x/web-interface/nav"
        val request = Request.Builder()
            .url(url)
            .header("Cookie", UserManager.getCookie())
            .header("User-Agent", NetworkClient.USER_AGENT)
            .build()

        return try {
            NetworkClient.client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string() ?: "")
                if (json.optInt("code") == 0) {
                    val data = json.getJSONObject("data")
                    UserProfile(
                        name = data.optString("uname", "未知用户"),
                        face = data.optString("face", "")
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // --- 内部私有方法，分别请求各个接口 ---

    private fun getFollowingCount(mid: String, cookie: String): Int {
        val url = "https://api.bilibili.com/x/relation/stat?vmid=$mid"
        try {
            val request = Request.Builder().url(url).header("Cookie", cookie).build()
            val response = NetworkClient.client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            if (json.optInt("code") == 0) {
                return json.optJSONObject("data")?.optInt("following") ?: 0
            }
        } catch (e: Exception) { e.printStackTrace() }
        return 0
    }

    private fun getWatchLaterCount(cookie: String): Int {
        val url = "https://api.bilibili.com/x/v2/history/toview"
        try {
            val request = Request.Builder().url(url).header("Cookie", cookie).build()
            val response = NetworkClient.client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            if (json.optInt("code") == 0) {
                // 这个接口返回的是 data: { count: 10, list: [...] }
                return json.optJSONObject("data")?.optInt("count") ?: 0
            }
        } catch (e: Exception) { e.printStackTrace() }
        return 0
    }

    private fun getFavoritesCount(mid: String, cookie: String): Int {
        val url = "https://api.bilibili.com/x/v3/fav/folder/created/list-all?up_mid=$mid"
        var total = 0
        try {
            val request = Request.Builder().url(url).header("Cookie", cookie).build()
            val response = NetworkClient.client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            if (json.optInt("code") == 0) {
                val data = json.optJSONObject("data")
                val list = data?.optJSONArray("list")
                if (list != null) {
                    for (i in 0 until list.length()) {
                        // 把每个收藏夹的 media_count 加起来
                        total += list.getJSONObject(i).optInt("media_count", 0)
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return total
    }

    fun fetchWatchLaterList(): List<DynamicItem> {
        val list = mutableListOf<DynamicItem>()
        val cookie = UserManager.getCookie()
        if (cookie.isEmpty()) return list

        val url = "https://api.bilibili.com/x/v2/history/toview"
        val request = Request.Builder()
            .url(url)
            .header("Cookie", cookie)
            .header("User-Agent", NetworkClient.USER_AGENT)
            .build()

        try {
            NetworkClient.client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return list
                val json = JSONObject(body)
                if (json.optInt("code") == 0) {
                    val data = json.optJSONObject("data")
                    val array = data?.optJSONArray("list") ?: return list

                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val owner = item.getJSONObject("owner")

                        list.add(
                            DynamicItem(
                                uid = item.optLong("add_at"), // 借用uid存一下添加时间
                                uname = owner.optString("name"),
                                avatar = owner.optString("face"),
                                pubTime = item.optLong("pubdate"),
                                title = item.optString("title"),
                                cover = item.optString("pic"),
                                bvid = item.optString("bvid"),
                                aid = item.optLong("aid"),
                                cid = item.optLong("cid") // 稍后再看接口直接返回了cid
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun fetchFavFolders(): List<FavFolder> {
        val list = mutableListOf<FavFolder>()
        val mid = getMyMid()
        if (mid.isEmpty()) {
            Log.e("UserRepository", "MID is empty, check your cookie")
            return list
        }

        // 1. 确保 URL 和参数正确
        val url = "https://api.bilibili.com/x/v3/fav/folder/created/list-all?up_mid=$mid"

        val request = Request.Builder()
            .url(url)
            .header("Cookie", UserManager.getCookie())
            .header("User-Agent", NetworkClient.USER_AGENT)
            // ⭐️ 必须增加 Referer，V3 接口校验非常严
            .header("Referer", "https://www.bilibili.com/")
            .build()

        try {
            NetworkClient.client.newCall(request).execute().use { response ->
                // 2. 将 body 取出存为变量，方便打印和多次使用
                val rawBody = response.body?.string() ?: ""
                Log.d("UserRepository", "FavFolders Raw Response: $rawBody")

                if (rawBody.isEmpty()) return list

                val json = JSONObject(rawBody)
                if (json.optInt("code") == 0) {
                    val data = json.optJSONObject("data") ?: return list
                    val array = data.optJSONArray("list") ?: return list

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)

                        // ⭐️ 修正后的解析逻辑：直接获取 JSONObject
                        val upperObj = obj.optJSONObject("upper")
                        val faceUrl = upperObj?.optString("face") ?: ""
                        val coverUrl = obj.optString("cover")

                        // 如果有头像则用头像（B站风格），否则用封面
                        val finalCover = if (faceUrl.isNotEmpty()) faceUrl else coverUrl

                        list.add(FavFolder(
                            mlid = obj.optLong("id"),
                            title = obj.optString("title"),
                            cover = finalCover,
                            mediaCount = obj.optInt("media_count")
                        ))
                    }
                } else {
                    Log.e("UserRepository", "API Error: ${json.optString("message")}")
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Fetch Fav Folders Failed", e)
        }
        return list
    }

    /**
     * 2. 获取指定收藏夹内的视频列表
     */
    fun fetchFavVideos(mlid: Long): List<DynamicItem> {
        val list = mutableListOf<DynamicItem>()
        val url = "https://api.bilibili.com/x/v3/fav/resource/list?media_id=$mlid&pn=1&ps=20"
        val request = Request.Builder()
            .url(url)
            .header("Cookie", UserManager.getCookie())
            .header("User-Agent", NetworkClient.USER_AGENT)
            .build()

        try {
            NetworkClient.client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string() ?: "")
                if (json.optInt("code") == 0) {
                    val data = json.optJSONObject("data")
                    val medias = data?.optJSONArray("medias") ?: return list
                    for (i in 0 until medias.length()) {
                        val obj = medias.getJSONObject(i)
                        val upper = obj.getJSONObject("upper")
                        list.add(DynamicItem(
                            uid = obj.optLong("id"),
                            uname = upper.optString("name"),
                            avatar = upper.optString("face"),
                            pubTime = obj.optLong("pubtime"),
                            title = obj.optString("title"),
                            cover = obj.optString("cover"),
                            bvid = obj.optString("bv_id"),
                            aid = obj.optLong("id"),
                            cid = 0L // 收藏接口通常不给cid，跳转时需要fetchCid
                        ))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    fun fetchFollowingList(page: Int = 1): List<UpItem> {
        val list = mutableListOf<UpItem>()
        val mid = getMyMid() // 之前写的提取 DedeUserID 的方法
        if (mid.isEmpty()) return list

        val url = "https://api.bilibili.com/x/relation/followings?vmid=$mid&pn=$page&ps=50&order=desc"

        val request = Request.Builder()
            .url(url)
            .header("Cookie", UserManager.getCookie())
            .header("User-Agent", NetworkClient.USER_AGENT)
            .header("Referer", "https://space.bilibili.com/$mid/fans/follow") // 必须加 Referer
            .build()

        try {
            NetworkClient.client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                if (json.optInt("code") == 0) {
                    val data = json.optJSONObject("data")
                    val array = data?.optJSONArray("list") ?: return list
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            UpItem(
                                mid = obj.optLong("mid"),
                                uname = obj.optString("uname"),
                                face = obj.optString("face"),
                                sign = obj.optString("sign")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}