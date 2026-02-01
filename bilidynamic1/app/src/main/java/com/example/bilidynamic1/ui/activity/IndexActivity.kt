package com.example.bilidynamic1.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bilidynamic1.R
import com.example.bilidynamic1.ui.fragment.DynamicFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class IndexActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_index)

        // 默认加载 DynamicFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DynamicFragment())
                .commit()
        }

        // 可以在 bottomNav 的点击事件里切换 Fragment
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dynamic -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, DynamicFragment())
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, DynamicFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}