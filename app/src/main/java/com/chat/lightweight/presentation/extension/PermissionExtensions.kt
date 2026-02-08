package com.chat.lightweight.presentation.extension

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.chat.lightweight.data.model.PermissionResult
import com.chat.lightweight.data.model.PermissionType
import com.chat.lightweight.domain.permission.PermissionsManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Activity权限请求扩展
 */
suspend fun AppCompatActivity.requestPermissionWithRationale(
    permissionType: PermissionType,
    customRationale: String? = null
): PermissionResult {
    val permissionsManager = PermissionsManager.getInstance(this)

    // 检查SDK版本
    if (android.os.Build.VERSION.SDK_INT < permissionType.minSdk) {
        return PermissionResult.Granted
    }

    // 检查是否已授予
    if (permissionsManager.isPermissionGranted(permissionType)) {
        return PermissionResult.Granted
    }

    // 检查是否应该显示说明
    val shouldShowRationale = permissionsManager.shouldShowRationale(this, permissionType.permission)

    if (shouldShowRationale) {
        // TODO: 显示权限说明弹窗
        // 暂时注释掉，因为 PermissionRequestDialog 已被删除
        /*
        val rationale = customRationale ?: permissionType.rationale
        com.chat.lightweight.presentation.dialog.PermissionRequestDialog(
            context = this,
            permissionName = permissionType.description,
            rationale = rationale
        ) {
            // 用户点击确定后请求权限
        }.show()
        */
    }

    // 请求权限
    return permissionsManager.requestPermission(this, permissionType.permission)
}

/**
 * Fragment权限请求扩展
 */
suspend fun Fragment.requestPermissionWithRationale(
    permissionType: PermissionType,
    customRationale: String? = null
): PermissionResult {
    val permissionsManager = PermissionsManager.getInstance(requireContext())

    // 检查SDK版本
    if (android.os.Build.VERSION.SDK_INT < permissionType.minSdk) {
        return PermissionResult.Granted
    }

    // 检查是否已授予
    if (permissionsManager.isPermissionGranted(permissionType)) {
        return PermissionResult.Granted
    }

    // 检查是否应该显示说明
    val shouldShowRationale = permissionsManager.shouldShowRationale(this, permissionType.permission)

    if (shouldShowRationale) {
        // TODO: 显示权限说明弹窗
        // 暂时注释掉，因为 PermissionRequestDialog 已被删除
        /*
        val rationale = customRationale ?: permissionType.rationale
        com.chat.lightweight.presentation.dialog.PermissionRequestDialog(
            context = requireContext(),
            permissionName = permissionType.description,
            rationale = rationale
        ) {
            // 用户点击确定后请求权限
        }.show()
        */
    }

    // 请求权限
    return permissionsManager.requestPermission(this, permissionType.permission)
}

/**
 * 处理权限请求结果
 *
 * @param permissionType 权限类型
 * @param granted 授予时的回调
 * @param denied 拒绝时的回调（参数为是否应该显示说明）
 * @param permanentlyDenied 永久拒绝时的回调
 */
fun handlePermissionResult(
    permissionType: PermissionType,
    result: PermissionResult,
    granted: () -> Unit,
    denied: (Boolean) -> Unit = {},
    permanentlyDenied: () -> Unit = {}
) {
    when (result) {
        is PermissionResult.Granted -> granted()
        is PermissionResult.Denied -> {
            if (!result.shouldShowRationale) {
                permanentlyDenied()
            } else {
                denied(result.shouldShowRationale)
            }
        }
        else -> {}
    }
}

/**
 * 检查权限是否已授予（扩展函数）
 */
fun Activity.isPermissionGranted(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * 检查权限是否已授予（扩展函数）
 */
fun Fragment.isPermissionGranted(permission: String): Boolean {
    return requireContext().isPermissionGranted(permission)
}

/**
 * 检查权限是否已授予（Context扩展函数）
 */
fun android.content.Context.isPermissionGranted(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * 检查多个权限是否都已授予
 */
fun Activity.arePermissionsGranted(permissions: List<String>): Boolean {
    return permissions.all { isPermissionGranted(it) }
}

/**
 * 检查多个权限是否都已授予
 */
fun Fragment.arePermissionsGranted(permissions: List<String>): Boolean {
    return requireContext().arePermissionsGranted(permissions)
}

/**
 * 检查多个权限是否都已授予
 */
fun android.content.Context.arePermissionsGranted(permissions: List<String>): Boolean {
    return permissions.all { isPermissionGranted(it) }
}
