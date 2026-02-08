package com.chat.lightweight.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Coil图片加载配置
 *
 * 优化图片加载性能：
 * - 内存缓存配置
 * - 磁盘缓存配置
 * - 图片压缩策略
 */
object CoilImageLoadConfig {

    /**
     * 创建优化的ImageLoader
     */
    fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(200 * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(true)
            .build()
    }

    /**
     * 创建ImageRequest构建器（用于单个请求）
     */
    fun createImageRequestBuilder(context: Context): ImageRequest.Builder {
        return ImageRequest.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
    }
}
