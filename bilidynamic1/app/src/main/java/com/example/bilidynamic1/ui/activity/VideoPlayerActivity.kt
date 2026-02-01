package com.example.bilidynamic1.ui.activity

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.repository.PlayableMedia
import com.example.bilidynamic1.data.repository.VideoRepository

@androidx.annotation.OptIn(UnstableApi::class)
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private var exoPlayer: ExoPlayer? = null

    private var bvid: String? = null
    private var cid: Long = 0

    // 记录播放位置（防止意外重建时进度丢失）
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_video_player)

        playerView = findViewById(R.id.playerView)
        progressBar = findViewById(R.id.progressBar)

        bvid = intent.getStringExtra("bvid")
        cid = intent.getLongExtra("cid", 0)

        // 检查当前屏幕方向，决定是否全屏
        updateSystemUi(resources.configuration)
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        // 隐藏系统栏（沉浸式）
        hideSystemUi()
        if (Build.VERSION.SDK_INT <= 23 || exoPlayer == null) {
            initializePlayer()
        }
    }

    private fun initializePlayer() {
        if (bvid == null || cid == 0L) return
        if (exoPlayer != null) return // 避免重复初始化

        progressBar.visibility = View.VISIBLE

        Thread {
            // 获取播放地址
            val media = VideoRepository.fetchPlayUrl(bvid!!, cid)
            runOnUiThread {
                progressBar.visibility = View.GONE
                if (media != null) {
                    startExoPlayer(media)
                } else {
                    Toast.makeText(this, "播放地址获取失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun startExoPlayer(media: PlayableMedia) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build()
            playerView.player = exoPlayer
        }

        val headers = mapOf(
            "Referer" to media.referer,
            "User-Agent" to media.userAgent
        )

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(media.userAgent)
            .setDefaultRequestProperties(headers)

        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(media.videoUrl)))
        val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(media.audioUrl)))

        val mergedSource = MergingMediaSource(videoSource, audioSource)

        exoPlayer?.apply {
            setMediaSource(mergedSource)
            // 恢复播放状态
            this.playWhenReady = this@VideoPlayerActivity.playWhenReady
            seekTo(currentItem, playbackPosition)
            prepare()
        }
    }

    // 监听屏幕旋转（由于Manifest配置了configChanges，旋转时不会走onCreate，而是走这里）
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemUi(newConfig)
    }

    private fun updateSystemUi(config: Configuration) {
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏：隐藏系统栏，全屏体验
            hideSystemUi()
        } else {
            // 竖屏：显示系统栏（可选，看你需求，通常看视频时竖屏也希望沉浸）
            showSystemUi()
        }
    }

    // 隐藏状态栏和导航栏 (兼容 Android 11+)
    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
    }

    // 显示状态栏
    private fun showSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        exoPlayer?.let { player ->
            playbackPosition = player.currentPosition
            currentItem = player.currentMediaItemIndex
            playWhenReady = player.playWhenReady
            player.release()
        }
        exoPlayer = null
    }
}