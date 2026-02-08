package com.chat.lightweight

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.chat.lightweight.permission.PermissionsManager

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val permissionsManager = PermissionsManager.getInstance()

    companion object {
        private const val APP_URL = "https://chat.soft1688.vip"
    }

    // 权限请求契约
    private val p0PermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            permissionsManager.handlePermissionResult(
                this,
                PermissionsManager.RC_P0_PERMISSIONS,
                permissions.keys.toTypedArray(),
                permissions.values.map { if (it) 0 else -1 }.toIntArray()
            )
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            permissionsManager.showPermissionRationaleDialog(
                this,
                "录音权限",
                "需要录音权限才能发送语音消息"
            )
        }
    }

    private val imagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            permissionsManager.showPermissionRationaleDialog(
                this,
                "存储权限",
                "需要存储权限才能选择图片"
            )
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            permissionsManager.showPermissionRationaleDialog(
                this,
                "相机权限",
                "需要相机权限才能拍照"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启用全屏模式
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        // 设置状态栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = 0x07c160
        }

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)

        // 初始化P0权限
        initializePermissions()

        // 配置 WebView
        configureWebView()

        // 加载应用
        webView.loadUrl(APP_URL)
    }

    /**
     * 初始化权限
     */
    private fun initializePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            if (!permissionsManager.checkPermission(this, notificationPermission)) {
                p0PermissionLauncher.launch(arrayOf(notificationPermission))
            }
        }
    }

    /**
     * 配置 WebView
     */
    private fun configureWebView() {
        val webSettings = webView.settings

        // 启用 JavaScript
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // 启用缓存
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        // 支持缩放
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false

        // 自适应屏幕
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true

        // 启用文件访问
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true

        // 支持混合内容
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // 设置 WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { view?.loadUrl(it) }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 页面加载完成
            }
        }

        // 设置 WebChromeClient 以支持文件上传
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                // 可以在这里添加加载进度条
            }
        }
    }

    /**
     * 请求录音权限 (供WebView调用)
     */
    fun requestAudioPermission() {
        if (!permissionsManager.checkPermission(this, Manifest.permission.RECORD_AUDIO)) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    /**
     * 请求图片选择权限 (供WebView调用)
     */
    fun requestImagePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            @Suppress("DEPRECATION")
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val needRequest = permissions.filterNot {
            permissionsManager.checkPermission(this, it)
        }.toTypedArray()

        if (needRequest.isNotEmpty()) {
            imagePermissionLauncher.launch(needRequest)
        }
    }

    /**
     * 请求相机权限 (供WebView调用)
     */
    fun requestCameraPermission() {
        if (!permissionsManager.checkPermission(this, Manifest.permission.CAMERA)) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onBackPressed() {
        // 如果 WebView 可以后退,则后退,否则退出应用
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.apply {
            onPause()
            pauseTimers()
        }
    }

    override fun onResume() {
        super.onResume()
        webView.apply {
            resumeTimers()
            onResume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
