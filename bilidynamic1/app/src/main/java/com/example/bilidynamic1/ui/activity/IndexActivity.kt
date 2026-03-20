package com.example.bilidynamic1.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.bilidynamic1.R
import com.example.bilidynamic1.ui.fragment.DynamicFragment
import com.example.bilidynamic1.ui.fragment.FavoritesFragment
import com.example.bilidynamic1.ui.fragment.WatchLaterFragment
import com.example.bilidynamic1.ui.fragment.FavoriteDetailFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class IndexActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_index)

        // 设置底部导航栏
        setupBottomNavigation()

        // 默认加载 DynamicFragment
        if (savedInstanceState == null) {
            loadFragment(DynamicFragment(), addToBackStack = false)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dynamic -> {
                    loadFragment(DynamicFragment(), addToBackStack = false)
                    true
                }
                R.id.nav_star -> {
                    // 创建 FavoritesFragment 并设置点击监听
                    val fragment = FavoritesFragment()
                    fragment.setOnFolderClickListener { mlid: Long, title: String ->  // 显式指定参数类型
                        navigateToFavoriteDetail(mlid, title)
                    }
                    loadFragment(fragment, addToBackStack = false)
                    true
                }
                R.id.nav_later -> {
                    loadFragment(WatchLaterFragment(), addToBackStack = false)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    fun navigateToFavoriteDetail(mlid: Long, title: String) {
        val fragment = FavoriteDetailFragment.newInstance(mlid, title)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("FavoriteDetail")
            .commit()
    }

    // 处理返回键，让 Fragment 能够正确返回
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}