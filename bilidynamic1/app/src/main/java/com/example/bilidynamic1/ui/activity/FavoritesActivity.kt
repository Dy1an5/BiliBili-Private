package com.example.bilidynamic1.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.repository.UserRepository
import com.example.bilidynamic1.ui.adapter.FavFolderAdapter

class FavoritesActivity : AppCompatActivity() {

    private lateinit var adapter: FavFolderAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 沉浸式状态栏适配
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_favorites)

        // 2. 处理标题栏和状态栏间距
        val headerView = findViewById<View>(R.id.header)
        ViewCompat.setOnApplyWindowInsetsListener(headerView) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        // 3. 初始化标题文字和返回按钮
        findViewById<TextView>(R.id.tvHeaderTitle).text = "我的收藏夹"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // 4. 初始化 SwipeRefreshLayout
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadFolders() }

        // 5. 初始化 RecyclerView (只初始化一次！)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // 设置网格布局：一行 2 个
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // 设置适配器及其点击跳转逻辑
        adapter = FavFolderAdapter { folder ->
            val intent = Intent(this, FavoriteDetailActivity::class.java).apply {
                putExtra("mlid", folder.mlid)
                putExtra("title", folder.title)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // 6. 开始加载数据
        loadFolders()
    }

    private fun loadFolders() {
        swipeRefresh.isRefreshing = true

        Thread {
            try {
                // 调用仓库方法获取数据
                val list = UserRepository.fetchFavFolders()

                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    if (list.isEmpty()) {
                        Toast.makeText(this, "未找到收藏夹或加载失败", Toast.LENGTH_SHORT).show()
                    } else {
                        adapter.submitList(list)
                    }
                }
            } catch (e: Exception) {
                Log.e("FavoritesActivity", "Load Error", e)
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this, "网络请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}