package com.example.bilidynamic1.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.repository.UserRepository
import com.example.bilidynamic1.ui.adapter.FollowingAdapter

class FollowingActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: FollowingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_following)

        // 1. 状态栏适配
        val headerView = findViewById<View>(R.id.header)
        ViewCompat.setOnApplyWindowInsetsListener(headerView) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, top, 0, 0)
            insets
        }

        // 2. 标题栏
        findViewById<TextView>(R.id.tvHeaderTitle).text = "我的关注"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // 3. 列表初始化
        swipeRefresh = findViewById(R.id.swipeRefresh)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = FollowingAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        Thread {
            val list = UserRepository.fetchFollowingList(1)
            runOnUiThread {
                swipeRefresh.isRefreshing = false
                adapter.submitList(list)
            }
        }.start()
    }
}