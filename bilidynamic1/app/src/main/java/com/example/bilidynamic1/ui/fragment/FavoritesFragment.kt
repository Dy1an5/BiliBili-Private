package com.example.bilidynamic1.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.repository.UserRepository
import com.example.bilidynamic1.ui.adapter.FavFolderAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout

class FavoritesFragment : Fragment() {

    private var _binding: View? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FavFolderAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var tvCollapsedTitle: TextView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var dividerLine: View

    // 直接使用函数类型，不需要定义接口
    private var onFolderClick: ((mlid: Long, title: String) -> Unit)? = null

    // 设置监听器的方法
    fun setOnFolderClickListener(listener: (mlid: Long, title: String) -> Unit) {
        this.onFolderClick = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflater.inflate(R.layout.fragment_favorites, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupTitle()
        setupSwipeRefresh(view)
        setupRecyclerView(view)
        loadFolders()
    }

    private fun initViews(view: View) {
        collapsingToolbar = view.findViewById(R.id.collapsingToolbar)
        tvCollapsedTitle = view.findViewById(R.id.tvCollapsedTitle)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        dividerLine = view.findViewById(R.id.dividerLine)
    }

    private fun setupTitle() {
        // 初始状态：显示 CollapsingToolbar 的标题，隐藏自定义 TextView
        collapsingToolbar.title = "收藏夹"
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
                // 完全展开状态 (scrollPercentage == 0)
                scrollPercentage == 0f -> {
                    collapsingToolbar.title = "收藏夹"
                    tvCollapsedTitle.visibility = View.GONE
                    dividerLine.visibility = View.VISIBLE
                }
                // 完全折叠状态 (scrollPercentage >= 0.95f)
                scrollPercentage >= 0.95f -> {
                    collapsingToolbar.title = ""  // 清空 CollapsingToolbar 的标题
                    tvCollapsedTitle.visibility = View.VISIBLE
                    tvCollapsedTitle.text = "收藏夹"
                    dividerLine.visibility = View.GONE  // 折叠时隐藏分割线
                }
                // 中间过渡状态
                else -> {
                    collapsingToolbar.title = "收藏夹"
                    tvCollapsedTitle.visibility = View.GONE
                    dividerLine.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupSwipeRefresh(view: View) {
        swipeRefresh.setOnRefreshListener { loadFolders() }

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
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // 保持 RecyclerView 的嵌套滚动启用，因为它现在是直接的滚动视图
        recyclerView.isNestedScrollingEnabled = true

        adapter = FavFolderAdapter { folder ->
            // 调用函数类型监听器
            onFolderClick?.invoke(folder.mlid, folder.title)
        }
        recyclerView.adapter = adapter
    }

    private fun loadFolders() {
        swipeRefresh.isRefreshing = true

        Thread {
            try {
                val list = UserRepository.fetchFavFolders()

                requireActivity().runOnUiThread {
                    if (isAdded) {
                        swipeRefresh.isRefreshing = false
                        if (list.isEmpty()) {
                            Toast.makeText(requireContext(), "未找到收藏夹或加载失败", Toast.LENGTH_SHORT).show()
                        } else {
                            adapter.submitList(list)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FavoritesFragment", "Load Error", e)
                requireActivity().runOnUiThread {
                    if (isAdded) {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(requireContext(), "网络请求异常", Toast.LENGTH_SHORT).show()
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