package com.example.bilidynamic1.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.repository.DynamicRepository
import com.example.bilidynamic1.ui.activity.VideoPlayerActivity
import com.example.bilidynamic1.data.manager.WbiUtil
import com.example.bilidynamic1.data.model.DynamicItem
import com.example.bilydynamic1.ui.adapter.DynamicAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout

class DynamicFragment : Fragment(R.layout.fragment_dynamic) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var tvCollapsedTitle: TextView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var dividerLine: View

    private val adapter = DynamicAdapter { item ->
        onDynamicClick(item)
    }

    // 下拉刷新状态锁
    private var isRefreshing = false

    // 分页加载相关变量
    private var currentOffset = ""
    private var isLoadingMore = false
    private var hasMoreData = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupTitle()
        setupAppBarListener()
        setupRecyclerView(view)
        setupSwipeRefresh()

        preloadWbiKeys()
        loadFirstPage() // 首次加载第一页
    }

    private fun initViews(view: View) {
        collapsingToolbar = view.findViewById(R.id.collapsingToolbar)
        tvCollapsedTitle = view.findViewById(R.id.tvCollapsedTitle)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        dividerLine = view.findViewById(R.id.dividerLine)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        recyclerView = view.findViewById(R.id.recyclerViewDynamic)
    }

    private fun setupTitle() {
        collapsingToolbar.title = "动态"
        tvCollapsedTitle.visibility = View.GONE
    }

    private fun setupAppBarListener() {
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBar, verticalOffset ->
            val scrollRange = appBar.totalScrollRange
            val scrollPercentage = if (scrollRange > 0) {
                (Math.abs(verticalOffset) * 1.0f / scrollRange)
            } else {
                0f
            }

            when {
                scrollPercentage == 0f -> {
                    collapsingToolbar.title = "动态"
                    tvCollapsedTitle.visibility = View.GONE
                    dividerLine.visibility = View.VISIBLE
                }
                scrollPercentage >= 0.95f -> {
                    collapsingToolbar.title = ""
                    tvCollapsedTitle.visibility = View.VISIBLE
                    tvCollapsedTitle.text = "动态"
                    dividerLine.visibility = View.GONE
                }
                else -> {
                    collapsingToolbar.title = "动态"
                    tvCollapsedTitle.visibility = View.GONE
                    dividerLine.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupRecyclerView(view: View) {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true

        // 添加上拉加载更多的监听
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                // 当滑到底部且还有更多数据且不在加载中时，加载更多
                // 使用 lastVisibleItemPosition >= totalItemCount - 3 提前触发加载
                if (!isLoadingMore && hasMoreData && lastVisibleItemPosition >= totalItemCount - 3) {
                    loadMoreData()
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            refreshData() // 下拉刷新
        }

        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        swipeRefresh.setProgressViewOffset(
            true,
            dpToPx(100),
            dpToPx(140)
        )
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /**
     * 加载第一页数据
     */
    private fun loadFirstPage() {
        currentOffset = ""
        hasMoreData = true
        loadData(isRefresh = true)
    }

    /**
     * 刷新数据
     */
    private fun refreshData() {
        if (isRefreshing) return
        currentOffset = ""
        hasMoreData = true
        loadData(isRefresh = true)
    }

    /**
     * 加载更多数据
     */
    private fun loadMoreData() {
        if (!hasMoreData || isLoadingMore) return
        loadData(isRefresh = false)
    }

    /**
     * 统一的数据加载方法
     * @param isRefresh 是否是下拉刷新（true会清空列表）
     */
    private fun loadData(isRefresh: Boolean) {
        // 检查锁
        if (isRefresh) {
            if (isRefreshing) return
            isRefreshing = true
            swipeRefresh.isRefreshing = true
        } else {
            if (isLoadingMore) return
            isLoadingMore = true
            showLoadMoreIndicator()
        }

        Thread {
            try {
                // 调用分页加载方法
                val result = DynamicRepository.loadVideoDynamicsPage(currentOffset)

                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread

                    if (isRefresh) {
                        // 下拉刷新：替换整个列表
                        adapter.submitList(result.items)
                        isRefreshing = false
                        swipeRefresh.isRefreshing = false

                        // 处理空数据情况
                        if (result.items.isEmpty()) {
                            Toast.makeText(context, "暂无动态", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 加载更多：追加到现有列表
                        if (result.items.isNotEmpty()) {
                            val currentList = adapter.currentList.toMutableList()
                            currentList.addAll(result.items)
                            adapter.submitList(currentList)
                        }
                        isLoadingMore = false
                        hideLoadMoreIndicator()

                        // 如果没有更多数据，提示用户
                        if (!result.hasMore) {
                            Toast.makeText(context, "没有更多动态了", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // 更新分页参数
                    currentOffset = result.nextOffset
                    hasMoreData = result.hasMore

                    Log.d("DynamicFragment", "Loaded ${result.items.size} items, hasMore: ${result.hasMore}")
                }
            } catch (e: Exception) {
                Log.e("DynamicFragment", "loadData error", e)

                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread

                    if (isRefresh) {
                        isRefreshing = false
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(context, "刷新失败", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoadingMore = false
                        hideLoadMoreIndicator()
                        Toast.makeText(context, "加载更多失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    /**
     * 显示底部加载更多指示器
     */
    private fun showLoadMoreIndicator() {
        // 可以在这里添加一个Footer显示"正在加载..."
        // 简单起见，可以用日志或Toast
        Log.d("DynamicFragment", "Loading more...")
    }

    /**
     * 隐藏底部加载更多指示器
     */
    private fun hideLoadMoreIndicator() {
        Log.d("DynamicFragment", "Loading more finished")
    }

    private fun preloadWbiKeys() {
        Thread {
            try {
                WbiUtil.refreshKeys()
                Log.d("DynamicFragment", "Wbi Keys preloaded success")
            } catch (e: Exception) {
                Log.e("DynamicFragment", "Wbi Keys preload failed", e)
            }
        }.start()
    }

    private fun onDynamicClick(item: DynamicItem) {
        if (item.bvid.isEmpty()) return

        if (item.cid != 0L) {
            goToPlayer(item.bvid, item.cid)
        } else {
            Toast.makeText(context, "正在获取视频信息...", Toast.LENGTH_SHORT).show()

            Thread {
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