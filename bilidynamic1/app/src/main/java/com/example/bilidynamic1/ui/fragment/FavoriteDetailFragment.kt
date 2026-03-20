package com.example.bilidynamic1.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.model.DynamicItem
import com.example.bilidynamic1.data.repository.DynamicRepository
import com.example.bilidynamic1.data.repository.UserRepository
import com.example.bilidynamic1.ui.activity.VideoPlayerActivity
import com.example.bilidynamic1.ui.adapter.WatchLaterAdapter

class FavoriteDetailFragment : Fragment() {

    private var _binding: View? = null
    private val binding get() = _binding!!

    private lateinit var adapter: WatchLaterAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private var mlid: Long = 0
    private var folderTitle: String = ""

    companion object {
        private const val ARG_MLID = "mlid"
        private const val ARG_TITLE = "title"

        fun newInstance(mlid: Long, title: String): FavoriteDetailFragment {
            val fragment = FavoriteDetailFragment()
            val args = Bundle()
            args.putLong(ARG_MLID, mlid)
            args.putString(ARG_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mlid = it.getLong(ARG_MLID, 0)
            folderTitle = it.getString(ARG_TITLE, "")
        }
    }

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

        // 初始化视图
        initViews(view)

        // 设置下拉刷新
        setupSwipeRefresh()

        // 设置RecyclerView
        setupRecyclerView()

        // 加载视频列表
        loadVideos(mlid)
    }

    private fun initViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        recyclerView = view.findViewById(R.id.recyclerView)
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener { loadVideos(mlid) }

        // 设置下拉刷新配色
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = WatchLaterAdapter { item ->
            // 因为收藏接口不给cid，这里需要补全cid后再播放
            getCidAndPlay(item)
        }
        recyclerView.adapter = adapter
    }

    private fun loadVideos(mlid: Long) {
        swipeRefresh.isRefreshing = true

        Thread {
            try {
                val list = UserRepository.fetchFavVideos(mlid)

                requireActivity().runOnUiThread {
                    if (isAdded) {
                        swipeRefresh.isRefreshing = false
                        if (list.isEmpty()) {
                            Toast.makeText(requireContext(), "该收藏夹暂无视频", Toast.LENGTH_SHORT).show()
                        } else {
                            adapter.submitList(list)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FavoriteDetailFragment", "Load Error", e)
                requireActivity().runOnUiThread {
                    if (isAdded) {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(requireContext(), "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun getCidAndPlay(item: DynamicItem) {
        // 显示加载中（可选，可以使用一个全局的ProgressBar）
        // showLoading()

        Thread {
            try {
                val realCid = DynamicRepository.fetchCid(item.bvid)

                requireActivity().runOnUiThread {
                    if (isAdded) {
                        // hideLoading()
                        val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                            putExtra("bvid", item.bvid)
                            putExtra("cid", realCid)
                        }
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                Log.e("FavoriteDetailFragment", "Get CID Error", e)
                requireActivity().runOnUiThread {
                    if (isAdded) {
                        // hideLoading()
                        Toast.makeText(requireContext(), "获取视频信息失败", Toast.LENGTH_SHORT).show()
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