package com.example.bilidynamic1.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.manager.UserManager // 确保引用了你的 UserManager
import com.example.bilidynamic1.data.repository.UserRepository
import com.example.bilidynamic1.ui.activity.FavoritesActivity
import com.example.bilidynamic1.ui.activity.FollowingActivity
import com.example.bilidynamic1.ui.activity.WatchLaterActivity

class InformationFragment : Fragment(R.layout.fragment_information) {

    // === UI 控件变量 ===
    private lateinit var imgHeaderBg: ImageView
    private lateinit var imgAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvSign: TextView

    // 数字控件
    private lateinit var tvFollowingCount: TextView
    private lateinit var tvFavoritesCount: TextView
    private lateinit var tvWatchLaterCount: TextView

    // 按钮点击区域
    private lateinit var btnFollowing: LinearLayout
    private lateinit var btnFavorites: LinearLayout
    private lateinit var btnWatchLater: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupClickListeners()

        // 统一调用一个加载方法
        loadAllData()
    }

    private fun loadAllData() {
        // 如果未登录，直接显示占位符并返回
        if (!UserManager.isLoggedIn()) {
            tvUsername.text = "点击登录"
            resetStatsToZero()
            return
        }

        Thread {
            try {
                // 1. 同步获取用户信息和统计数字
                val profile = UserRepository.fetchUserInfo()
                val stats = UserRepository.fetchUserStats()

                // 2. 使用安全调用切换回 UI 线程
                activity?.runOnUiThread {
                    // 3. 检查 Fragment 状态
                    if (!isAdded) return@runOnUiThread

                    // 更新用户信息
                    profile?.let {
                        tvUsername.text = it.name
                        Glide.with(this)
                            .load(it.face)
                            .circleCrop()
                            .placeholder(R.mipmap.ic_launcher)
                            .into(imgAvatar)
                    }

                    // 更新统计数字
                    tvFollowingCount.text = formatCount(stats.following)
                    tvFavoritesCount.text = formatCount(stats.favorites)
                    tvWatchLaterCount.text = formatCount(stats.watchLater)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    if (isAdded) Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun initViews(view: View) {
        imgAvatar = view.findViewById(R.id.imgAvatar)
        tvUsername = view.findViewById(R.id.tvUsername)

        tvFollowingCount = view.findViewById(R.id.tvFollowingCount)
        tvFavoritesCount = view.findViewById(R.id.tvFavoritesCount)
        tvWatchLaterCount = view.findViewById(R.id.tvWatchLaterCount)

        // 注意：这里绑定的 ID 是你布局中 LinearLayout 的 ID
        btnFollowing = view.findViewById(R.id.btnFollowing)
        btnFavorites = view.findViewById(R.id.btnFavorites)
        btnWatchLater = view.findViewById(R.id.btnWatchLater)
    }



    private fun setupClickListeners() {
        // 1. 关注列表
        btnFollowing.setOnClickListener {
            if (checkLogin()) {
                val intent = Intent(requireContext(), FollowingActivity::class.java)
                startActivity(intent)
            }
        }

        // 2. 收藏列表
        btnFavorites.setOnClickListener {
            if (checkLogin()) {
                val intent = Intent(requireContext(), FavoritesActivity::class.java)
                startActivity(intent)
            }
        }

        // 3. 稍后再看
        btnWatchLater.setOnClickListener {
            if (checkLogin()) {
                val intent = Intent(requireContext(), WatchLaterActivity::class.java)
                startActivity(intent)
            }
        }
    }

    /**
     * 辅助方法：检查登录状态
     */
    private fun checkLogin(): Boolean {
        if (UserManager.isLoggedIn()) {
            return true
        }
        Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
        return false
    }

    /**
     * 辅助方法：格式化数字 (例如 12500 -> 1.2万)
     */
    private fun formatCount(count: Int): String {
        return when {
            count >= 10000 -> String.format("%.1f万", count / 10000.0)
            else -> count.toString()
        }
    }

    /**
     * 辅助方法：重置数字为 --
     */
    private fun resetStatsToZero() {
        tvFollowingCount.text = "--"
        tvFavoritesCount.text = "--"
        tvWatchLaterCount.text = "--"
    }
}