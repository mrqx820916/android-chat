package com.chat.lightweight

import android.app.Application
import android.content.Context
import coil.ImageLoader
import com.chat.lightweight.di.CoilImageLoadConfig

/**
 * 轻聊应用Application类
 *
 * 初始化全局配置和第三方库
 */
class LightweightChatApp : Application() {

    companion object {
        lateinit var instance: LightweightChatApp
            private set

        lateinit var imageLoader: ImageLoader
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化Coil图片加载器
        imageLoader = CoilImageLoadConfig.createImageLoader(this)

        // 初始化其他组件...
    }
}

/**
 * 获取ApplicationContext的扩展函数
 */
fun Context.getImageLoader(): ImageLoader {
    val app = applicationContext as LightweightChatApp
    return LightweightChatApp.imageLoader
}
