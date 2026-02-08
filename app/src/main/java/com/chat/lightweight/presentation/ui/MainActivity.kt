package com.chat.lightweight.presentation.ui

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.chat.lightweight.R
import com.chat.lightweight.databinding.ActivityMainBinding

/**
 * 主Activity - WebView容器
 * 加载Web应用并提供原生功能
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val APP_URL = "https://chat.soft1688.vip"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        loadApp()
    }

    /**
     * 配置WebView
     */
    private fun setupWebView() {
        val webSettings = binding.webview.settings

        // 启用JavaScript
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // 启用缓存
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        // webSettings.setAppCacheEnabled(true) // 已在 API 23 弃用

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
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // 设置WebViewClient
        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                url: String?
            ): Boolean {
                url?.let { view?.loadUrl(it) }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 页面加载完成
            }
        }
    }

    /**
     * 加载应用
     */
    private fun loadApp() {
        binding.webview.loadUrl(APP_URL)
    }

    /**
     * 处理返回键
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * 暂停WebView
     */
    override fun onPause() {
        super.onPause()
        binding.webview.onPause()
        binding.webview.pauseTimers()
    }

    /**
     * 恢复WebView
     */
    override fun onResume() {
        super.onResume()
        binding.webview.resumeTimers()
        binding.webview.onResume()
    }
}
