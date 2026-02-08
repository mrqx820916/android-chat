package com.chat.lightweight.data.model

/**
 * 权限结果封装类
 */
sealed class PermissionResult {
    /**
     * 权限已授予
     */
    data object Granted : PermissionResult()

    /**
     * 权限被拒绝
     * @param permission 被拒绝的权限
     * @param shouldShowRationale 是否应该显示权限说明
     */
    data class Denied(
        val permission: String,
        val shouldShowRationale: Boolean
    ) : PermissionResult()

    /**
     * 部分权限被拒绝（批量请求时）
     * @param grantedPermissions 已授予的权限列表
     * @param deniedPermissions 被拒绝的权限列表
     */
    data class PartiallyGranted(
        val grantedPermissions: List<String>,
        val deniedPermissions: List<Denied>
    ) : PermissionResult()
}

/**
 * 权限请求回调
 */
typealias PermissionCallback = (PermissionResult) -> Unit

/**
 * 权限组请求回调
 */
typealias PermissionsCallback = (Map<String, PermissionResult>) -> Unit
