// com/example/bilidynamic/ui/activity/LoginActivity.kt
package com.example.bilidynamic1.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.manager.UserManager
import com.example.bilidynamic1.ui.activity.IndexActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val webView = findViewById<WebView>(R.id.webLogin)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (url == null) return

                val cookie =
                    CookieManager.getInstance().getCookie("https://www.bilibili.com")

                Log.d("Login", "url=$url")
                Log.d("Login", "cookie=$cookie")

                if (!cookie.isNullOrEmpty() && cookie.contains("SESSDATA")) {
                    saveCookieAndGoIndex(cookie)
                }
            }
        }

        webView.loadUrl("https://passport.bilibili.com/login")
    }

    private fun saveCookieAndGoIndex(cookie: String) {
        // 使用UserManager保存cookie
        UserManager.saveCookie(cookie)

        Log.d("Login", "cookie saved, go Index")

        startActivity(Intent(this, IndexActivity::class.java))
        finish()
    }
}