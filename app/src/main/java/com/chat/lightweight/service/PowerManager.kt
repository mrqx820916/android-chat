package com.chat.lightweight.service

import android.content.Context
import android.os.PowerManager as SystemPowerManager
import timber.log.Timber

/**
 * 电源管理器
 * 处理Doze模式和省电模式兼容性
 */
class PowerManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: PowerManager? = null

        fun getInstance(context: Context): PowerManager {
            return instance ?: synchronized(this) {
                instance ?: PowerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as SystemPowerManager

    /**
     * 检查是否在省电模式
     */
    fun isPowerSaveMode(): Boolean {
        return powerManager.isPowerSaveMode
    }

    /**
     * 检查是否在Doze模式
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    /**
     * 检查Doze模式状态
     */
    fun getDozeState(): DozeState {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return DozeState.NOT_SUPPORTED
        }

        return if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            DozeState.WHITELISTED
        } else {
            DozeState.NOT_WHITELISTED
        }
    }

    /**
     * 获取智能心跳间隔
     * 根据电源状态调整心跳频率
     */
    fun getSmartHeartbeatInterval(baseInterval: Long): Long {
        return when {
            isPowerSaveMode() -> baseInterval * 3 // 省电模式：3倍间隔
            isDeviceIdle() -> baseInterval * 2     // 空闲：2倍间隔
            else -> baseInterval                    // 正常
        }
    }

    /**
     * 检查设备是否空闲
     */
    private fun isDeviceIdle(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager.isDeviceIdleMode
        } else {
            false
        }
    }

    enum class DozeState {
        NOT_SUPPORTED,    // 系统不支持Doze
        NOT_WHITELISTED,  // 未在白名单
        WHITELISTED       // 已在白名单
    }
}
