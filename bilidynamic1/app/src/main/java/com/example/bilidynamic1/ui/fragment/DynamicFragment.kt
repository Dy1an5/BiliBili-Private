package com.example.bilidynamic1.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.repository.DynamicRepository
import com.example.bilidynamic1.ui.activity.VideoPlayerActivity
import com.example.bilidynamic1.data.manager.WbiUtil

import com.example.bilydynamic1.ui.adapter.DynamicAdapter
import com.example.bilidynamic1.data.model.DynamicItem


class DynamicFragment : Fragment(R.layout.fragment_dynamic) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    // 1️⃣ 初始化 Adapter 时传入点击逻辑
    // ⭐️ 增加状态锁，防止并发刷新产生多个线程
    private var isRefreshing = false

    private val adapter = DynamicAdapter { item ->
        onDynamicClick(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        recyclerView = view.findViewById(R.id.recyclerViewDynamic)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            loadData() // 下拉刷新调用
        }

        preloadWbiKeys()
        loadData() // 首次加载调用
    }

    private fun loadData() {
        // ⭐️ 检查锁：如果正在加载中，则不开启新线程
        if (isRefreshing) return

        isRefreshing = true
        swipeRefresh.isRefreshing = true

        Thread {
            try {
                val list = DynamicRepository.loadVideoDynamics(100)
                Log.d("DynamicFragment", "list size = ${list.size}")

                // ⭐️ 使用 activity? 安全调用，防止 requireActivity() 崩溃
                activity?.runOnUiThread {
                    // ⭐️ 检查 Fragment 是否还附着在 Activity 上
                    if (!isAdded) return@runOnUiThread

                    adapter.submitList(list)

                    // 释放锁
                    swipeRefresh.isRefreshing = false
                    isRefreshing = false
                }
            } catch (e: Exception) {
                Log.e("DynamicFragment", "loadData error", e)

                activity?.runOnUiThread {
                    if (isAdded) {
                        swipeRefresh.isRefreshing = false
                        isRefreshing = false // 报错也要释放锁
                        Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun preloadWbiKeys() {
        Thread {
            try {
                // 确保 WbiUtil 已经按照之前的代码实现好了
                WbiUtil.refreshKeys()
                Log.d("DynamicFragment", "Wbi Keys preloaded success")
            } catch (e: Exception) {
                Log.e("DynamicFragment", "Wbi Keys preload failed", e)
            }
        }.start()
    }

    // 3️⃣ 处理点击跳转逻辑
    // 在 DynamicFragment 中
    private fun onDynamicClick(item: DynamicItem) {
        if (item.bvid.isEmpty()) return

        // 情况 A: 运气好，列表直接给了 cid
        if (item.cid != 0L) {
            goToPlayer(item.bvid, item.cid)
        }
        // 情况 B: 列表没给 cid，需要现场查一下
        else {
            // 显示一个 Loading
            Toast.makeText(context, "正在获取视频信息...", Toast.LENGTH_SHORT).show()

            Thread {
                // 调用上面新写的 fetchCid 方法
                val realCid = DynamicRepository.fetchCid(item.bvid)

                requireActivity().runOnUiThread {
                    if (realCid > 0) {
                        goToPlayer(item.bvid, realCid)
                    } else {
                        Toast.makeText(context, "无法播放：找不到视频文件", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    private fun goToPlayer(bvid: String, cid: Long) {
        val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
            putExtra("bvid", bvid)
            putExtra("cid", cid)
        }
        startActivity(intent)
    }

}