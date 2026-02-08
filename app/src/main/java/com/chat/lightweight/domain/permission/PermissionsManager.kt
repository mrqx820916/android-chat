package com.chat.lightweight.domain.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chat.lightweight.data.model.PermissionResult
import com.chat.lightweight.data.model.PermissionType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 权限管理器
 *
 * 负责处理应用的所有权限请求逻辑
 */
class PermissionsManager(private val context: Context) {

    /**
     * 检查单个权限是否已授予
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查多个权限是否都已授予
     */
    fun arePermissionsGranted(permissions: List<String>): Boolean {
        return permissions.all { isPermissionGranted(it) }
    }

    /**
     * 检查权限类型是否已授予
     */
    fun isPermissionGranted(permissionType: PermissionType): Boolean {
        // 检查SDK版本
        if (Build.VERSION.SDK_INT < permissionType.minSdk) {
            return true // 低版本无需请求
        }
        return isPermissionGranted(permissionType.permission)
    }

    /**
     * 检查是否应该显示权限说明
     * (用户之前拒绝过该权限，但没有勾选"不再询问")
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * 检查是否应该显示权限说明
     * (Fragment版本)
     */
    fun shouldShowRationale(fragment: Fragment, permission: String): Boolean {
        return fragment.shouldShowRequestPermissionRationale(permission)
    }

    /**
     * 检查权限是否被永久拒绝
     * (用户勾选了"不再询问")
     */
    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return !isPermissionGranted(permission) &&
                !shouldShowRationale(activity, permission)
    }

    /**
     * 检查权限是否被永久拒绝
     * (Fragment版本)
     */
    fun isPermissionPermanentlyDenied(fragment: Fragment, permission: String): Boolean {
        return !isPermissionGranted(permission) &&
                !shouldShowRationale(fragment, permission)
    }

    /**
     * 从Activity请求单个权限（协程版本）
     */
    suspend fun requestPermission(
        activity: Activity,
        permission: String
    ): PermissionResult = suspendCancellableCoroutine { continuation ->
        // 如果已经授予，直接返回成功
        if (isPermissionGranted(permission)) {
            continuation.resume(PermissionResult.Granted)
            return@suspendCancellableCoroutine
        }

        // 生成请求码
        val requestCode = generateRequestCode()

        // 注册回调
        registerCallback(requestCode) { grantResults ->
            val granted = grantResults[permission] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                continuation.resume(PermissionResult.Granted)
            } else {
                val shouldShowRationale = shouldShowRationale(activity, permission)
                continuation.resume(
                    PermissionResult.Denied(permission, shouldShowRationale)
                )
            }
        }

        // 发起请求
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            requestCode
        )

        continuation.invokeOnCancellation {
            unregisterCallback(requestCode)
        }
    }

    /**
     * 从Activity请求多个权限（协程版本）
     */
    suspend fun requestPermissions(
        activity: Activity,
        permissions: List<String>
    ): Map<String, PermissionResult> = suspendCancellableCoroutine { continuation ->
        // 过滤已授予的权限
        val needsRequest = permissions.filter { !isPermissionGranted(it) }

        // 如果所有权限都已授予，直接返回成功
        if (needsRequest.isEmpty()) {
            val result = permissions.associateWith { PermissionResult.Granted }
            continuation.resume(result)
            return@suspendCancellableCoroutine
        }

        // 生成请求码
        val requestCode = generateRequestCode()

        // 注册回调
        registerCallback(requestCode) { grantResults ->
            val result = mutableMapOf<String, PermissionResult>()

            permissions.forEach { permission ->
                val granted = grantResults[permission] == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    result[permission] = PermissionResult.Granted
                } else {
                    val shouldShowRationale = shouldShowRationale(activity, permission)
                    result[permission] = PermissionResult.Denied(
                        permission,
                        shouldShowRationale
                    )
                }
            }

            continuation.resume(result)
        }

        // 发起请求
        ActivityCompat.requestPermissions(
            activity,
            needsRequest.toTypedArray(),
            requestCode
        )

        continuation.invokeOnCancellation {
            unregisterCallback(requestCode)
        }
    }

    /**
     * 从Fragment请求单个权限（协程版本）
     */
    suspend fun requestPermission(
        fragment: Fragment,
        permission: String
    ): PermissionResult = suspendCancellableCoroutine { continuation ->
        // 如果已经授予，直接返回成功
        if (isPermissionGranted(permission)) {
            continuation.resume(PermissionResult.Granted)
            return@suspendCancellableCoroutine
        }

        // 生成请求码
        val requestCode = generateRequestCode()

        // 注册回调
        registerFragmentCallback(fragment, requestCode) { grantResults ->
            val granted = grantResults[permission] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                continuation.resume(PermissionResult.Granted)
            } else {
                val shouldShowRationale = shouldShowRationale(fragment, permission)
                continuation.resume(
                    PermissionResult.Denied(permission, shouldShowRationale)
                )
            }
        }

        // 发起请求
        fragment.requestPermissions(arrayOf(permission), requestCode)

        continuation.invokeOnCancellation {
            unregisterFragmentCallback(fragment, requestCode)
        }
    }

    /**
     * 从Fragment请求多个权限（协程版本）
     */
    suspend fun requestPermissions(
        fragment: Fragment,
        permissions: List<String>
    ): Map<String, PermissionResult> = suspendCancellableCoroutine { continuation ->
        // 过滤已授予的权限
        val needsRequest = permissions.filter { !isPermissionGranted(it) }

        // 如果所有权限都已授予，直接返回成功
        if (needsRequest.isEmpty()) {
            val result = permissions.associateWith { PermissionResult.Granted }
            continuation.resume(result)
            return@suspendCancellableCoroutine
        }

        // 生成请求码
        val requestCode = generateRequestCode()

        // 注册回调
        registerFragmentCallback(fragment, requestCode) { grantResults ->
            val result = mutableMapOf<String, PermissionResult>()

            permissions.forEach { permission ->
                val granted = grantResults[permission] == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    result[permission] = PermissionResult.Granted
                } else {
                    val shouldShowRationale = shouldShowRationale(fragment, permission)
                    result[permission] = PermissionResult.Denied(
                        permission,
                        shouldShowRationale
                    )
                }
            }

            continuation.resume(result)
        }

        // 发起请求
        fragment.requestPermissions(needsRequest.toTypedArray(), requestCode)

        continuation.invokeOnCancellation {
            unregisterFragmentCallback(fragment, requestCode)
        }
    }

    /**
     * 处理权限请求结果（从Activity的onRequestPermissionsResult调用）
     */
    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        val callback = callbacks[requestCode] ?: return false

        val grantResultMap = permissions.mapIndexed { index, permission ->
            permission to grantResults[index]
        }.toMap()

        callback.invoke(grantResultMap)
        unregisterCallback(requestCode)
        return true
    }

    /**
     * 处理Fragment的权限请求结果
     */
    fun handleFragmentPermissionsResult(
        fragment: Fragment,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        val callback = fragmentCallbacks[fragment]?.get(requestCode) ?: return false

        val grantResultMap = permissions.mapIndexed { index, permission ->
            permission to grantResults[index]
        }.toMap()

        callback.invoke(grantResultMap)
        unregisterFragmentCallback(fragment, requestCode)
        return true
    }

    // ========== 私有方法 ==========

    private val callbacks = mutableMapOf<Int, (Map<String, Int>) -> Unit>()
    private val fragmentCallbacks = mutableMapOf<Fragment, MutableMap<Int, (Map<String, Int>) -> Unit>>()
    private var currentRequestCode = 1000

    private fun generateRequestCode(): Int {
        return currentRequestCode++
    }

    private fun registerCallback(
        requestCode: Int,
        callback: (Map<String, Int>) -> Unit
    ) {
        callbacks[requestCode] = callback
    }

    private fun unregisterCallback(requestCode: Int) {
        callbacks.remove(requestCode)
    }

    private fun registerFragmentCallback(
        fragment: Fragment,
        requestCode: Int,
        callback: (Map<String, Int>) -> Unit
    ) {
        val fragmentCallbacksMap = fragmentCallbacks.getOrPut(fragment) { mutableMapOf() }
        fragmentCallbacksMap[requestCode] = callback
    }

    private fun unregisterFragmentCallback(fragment: Fragment, requestCode: Int) {
        fragmentCallbacks[fragment]?.remove(requestCode)
        if (fragmentCallbacks[fragment]?.isEmpty() == true) {
            fragmentCallbacks.remove(fragment)
        }
    }

    companion object {
        @Volatile
        private var instance: PermissionsManager? = null

        /**
         * 获取权限管理器单例
         */
        fun getInstance(context: Context): PermissionsManager {
            return instance ?: synchronized(this) {
                instance ?: PermissionsManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
