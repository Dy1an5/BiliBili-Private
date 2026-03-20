package com.example.bilidynamic1.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.repository.UserRepository
import com.example.bilidynamic1.ui.activity.VideoPlayerActivity
import com.example.bilidynamic1.ui.adapter.WatchLaterAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout

class WatchLaterFragment : Fragment() {

    private var _binding: View? = null
    private val binding get() = _binding!!

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: WatchLaterAdapter
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var tvCollapsedTitle: TextView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var dividerLine: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflater.inflate(R.layout.fragment_watch_later, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupTitle()
        setupAppBarListener()
        setupSwipeRefresh(view)
        setupRecyclerView(view)
        loadData()
    }

    private fun initViews(view: View) {
        collapsingToolbar = view.findViewById(R.id.collapsingToolbar)
        tvCollapsedTitle = view.findViewById(R.id.tvCollapsedTitle)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        dividerLine = view.findViewById(R.id.dividerLine)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
    }

    private fun setupTitle() {
        // 初始状态：显示 CollapsingToolbar 的标题，隐藏自定义 TextView
        collapsingToolbar.title = "稍后再看"
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

            // 根据滚动状态控制标题的显示
            when {
                // 完全展开状态
                scrollPercentage == 0f -> {
                    collapsingToolbar.title = "稍后再看"
                    tvCollapsedTitle.visibility = View.GONE
                    dividerLine.visibility = View.VISIBLE
                }
                // 完全折叠状态
                scrollPercentage >= 0.95f -> {
                    collapsingToolbar.title = ""
                    tvCollapsedTitle.visibility = View.VISIBLE
                    tvCollapsedTitle.text = "稍后再看"
                    dividerLine.visibility = View.GONE
                }
                // 中间过渡状态
                else -> {
                    collapsingToolbar.title = "稍后再看"
                    tvCollapsedTitle.visibility = View.GONE
                    dividerLine.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupSwipeRefresh(view: View) {
        swipeRefresh.setOnRefreshListener { loadData() }

        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // 设置下拉刷新的偏移量，避免被标题栏遮挡
        swipeRefresh.setProgressViewOffset(
            true,
            dpToPx(100),
            dpToPx(140)
        )
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        adapter = WatchLaterAdapter { item ->
            val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                putExtra("bvid", item.bvid)
                putExtra("cid", item.cid)
            }
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true

        Thread {
            try {
                val list = UserRepository.fetchWatchLaterList()

                requireActivity().runOnUiThread {
                    if (isAdded) {
                        swipeRefresh.isRefreshing = false
                        if (list.isEmpty()) {
                            Toast.makeText(requireContext(), "列表为空或加载失败", Toast.LENGTH_SHORT).show()
                        } else {
                            adapter.submitList(list)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WatchLaterFragment", "Load Error", e)
                requireActivity().runOnUiThread {
                    if (isAdded) {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(requireContext(), "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}