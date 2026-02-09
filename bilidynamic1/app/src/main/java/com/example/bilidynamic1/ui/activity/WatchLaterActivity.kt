package com.example.bilidynamic1.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.repository.UserRepository
import com.example.bilidynamic1.ui.adapter.WatchLaterAdapter

class WatchLaterActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: WatchLaterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_later)

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

        // 1. 初始化标题栏
        findViewById<TextView>(R.id.tvHeaderTitle).text = "稍后再看"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // 2. 初始化列表
        swipeRefresh = findViewById(R.id.swipeRefresh)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = WatchLaterAdapter { item ->
            // 直接跳转到你原有的播放器
            val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra("bvid", item.bvid)
                putExtra("cid", item.cid)
            }
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 3. 下拉刷新逻辑
        swipeRefresh.setOnRefreshListener { loadData() }

        // 首次加载
        loadData()
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        Thread {
            val list = UserRepository.fetchWatchLaterList()
            runOnUiThread {
                swipeRefresh.isRefreshing = false
                if (list.isEmpty()) {
                    Toast.makeText(this, "列表为空或加载失败", Toast.LENGTH_SHORT).show()
                } else {
                    adapter.submitList(list)
                }
            }
        }.start()
    }
}