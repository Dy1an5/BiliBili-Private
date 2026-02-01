// com/example/bilidynamic/ui/activity/MainActivity.kt
package com.example.bilidynamic1.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.bilidynamic1.data.manager.UserManager
import com.example.bilidynamic1.R
import com.example.bilidynamic1.ui.activity.IndexActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化UserManager
        UserManager.init(applicationContext)

        if (UserManager.isLoggedIn()) {
            // 已登录，直接进首页
            startActivity(Intent(this, IndexActivity::class.java))
            finish()
            return
        }

        // 未登录，显示按钮页面
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}