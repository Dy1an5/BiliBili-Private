package com.example.bilidynamic1.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.model.DynamicItem
import com.example.bilidynamic1.data.repository.DynamicRepository
import com.example.bilidynamic1.data.repository.UserRepository
import com.example.bilidynamic1.ui.adapter.WatchLaterAdapter

// FavoriteDetailActivity.kt
class FavoriteDetailActivity : AppCompatActivity() {
    private lateinit var adapter: WatchLaterAdapter // 复用之前的适配器

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_later) // 复用稍后再看的布局

        // 1. 开启全屏布局模式，允许内容绘制到状态栏后面
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_watch_later)

        // 2. 找到你的标题栏布局 (include 的那个布局)
        val headerView = findViewById<View>(R.id.header)

        // 3. 监听并应用状态栏高度
        ViewCompat.setOnApplyWindowInsetsListener(headerView) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            // 将状态栏高度设为标题栏的顶部内边距
            v.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        val mlid = intent.getLongExtra("mlid", 0)
        findViewById<TextView>(R.id.tvHeaderTitle).text = intent.getStringExtra("title")
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = WatchLaterAdapter { item ->
            // 因为收藏接口不给cid，这里需要补全cid后再播放
            getCid(item)
        }
        rv.adapter = adapter

        loadVideos(mlid)
    }

    private fun loadVideos(mlid: Long) {
        Thread {
            val list = UserRepository.fetchFavVideos(mlid)
            runOnUiThread { adapter.submitList(list) }
        }.start()
    }

    private fun getCid(item: DynamicItem) {
        Thread {
            val realCid = DynamicRepository.fetchCid(item.bvid) // 复用你之前的 fetchCid
            runOnUiThread {
                val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                    putExtra("bvid", item.bvid)
                    putExtra("cid", realCid)
                }
                startActivity(intent)
            }
        }.start()
    }
}