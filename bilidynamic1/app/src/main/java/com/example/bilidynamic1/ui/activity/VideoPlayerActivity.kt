package com.example.bilidynamic1.ui.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
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

    // 记录播放位置
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    // 手势检测器
    private lateinit var gestureDetector: GestureDetector

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_video_player)

        playerView = findViewById(R.id.playerView)
        progressBar = findViewById(R.id.progressBar)

        bvid = intent.getStringExtra("bvid")
        cid = intent.getLongExtra("cid", 0)

        // 1. 初始化手势
        initGestureDetector()

        // 2. 接管 Touch 事件
        playerView.setOnTouchListener { _, event ->
            // 处理抬手逻辑 (恢复正常速度)
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                stopSpeedPlay()
            }

            // 发送给手势检测器
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun initGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            // 双击全屏
            override fun onDoubleTap(e: MotionEvent): Boolean {
                toggleOrientation()
                return true
            }

            // 长按 2倍速
            override fun onLongPress(e: MotionEvent) {
                if (exoPlayer?.isPlaying == true) {
                    startSpeedPlay()
                }
            }

            // 单击 (模拟原生的显示/隐藏控制条)
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (playerView.isControllerFullyVisible) {
                    playerView.hideController()
                } else {
                    playerView.showController()
                }
                return true
            }
        })
    }

    private fun startSpeedPlay() {
        // 直接设置速度，不显示任何 UI
        exoPlayer?.setPlaybackSpeed(2.0f)
    }

    private fun stopSpeedPlay() {
        // 恢复正常速度
        if (exoPlayer?.playbackParameters?.speed != 1.0f) {
            exoPlayer?.setPlaybackSpeed(1.0f)
        }
    }

    private fun toggleOrientation() {
        val currentOrientation = resources.configuration.orientation
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    // === 下面是状态栏隐藏和生命周期逻辑，保持不变 ===

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Build.VERSION.SDK_INT <= 23 || exoPlayer == null) {
            initializePlayer()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideSystemUi()
    }

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

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) initializePlayer()
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= 23) releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) releasePlayer()
    }

    private fun initializePlayer() {
        if (bvid == null || cid == 0L) return
        if (exoPlayer != null) return
        progressBar.visibility = View.VISIBLE
        Thread {
            val media = VideoRepository.fetchPlayUrl(bvid!!, cid)
            runOnUiThread {
                progressBar.visibility = View.GONE
                if (media != null) startExoPlayer(media)
                else Toast.makeText(this, "地址获取失败", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun startExoPlayer(media: PlayableMedia) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build()
            playerView.player = exoPlayer
        }
        val headers = mapOf("Referer" to media.referer, "User-Agent" to media.userAgent)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(media.userAgent)
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)

        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(media.videoUrl)))
        val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(media.audioUrl)))

        exoPlayer?.apply {
            setMediaSource(MergingMediaSource(videoSource, audioSource))
            this.playWhenReady = this@VideoPlayerActivity.playWhenReady
            seekTo(currentItem, playbackPosition)
            prepare()
        }
    }

    private fun releasePlayer() {
        exoPlayer?.let {
            playbackPosition = it.currentPosition
            currentItem = it.currentMediaItemIndex
            playWhenReady = it.playWhenReady
            it.release()
        }
        exoPlayer = null
    }
}