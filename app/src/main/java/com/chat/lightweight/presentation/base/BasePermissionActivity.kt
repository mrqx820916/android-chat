package com.chat.lightweight.presentation.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chat.lightweight.data.model.PermissionResult
import com.chat.lightweight.data.model.PermissionType
import com.chat.lightweight.domain.permission.PermissionsManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 基础Activity
 *
 * 集成权限管理功能，所有需要请求权限的Activity都应继承此类
 */
abstract class BasePermissionActivity : AppCompatActivity() {

    protected lateinit var permissionsManager: PermissionsManager
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsManager = PermissionsManager.getInstance(this)
    }

    /**
     * 请求权限（带理由说明）
     *
     * @param permissionType 权限类型
     * @return 权限请求结果
     */
    protected suspend fun requestPermissionWithRationale(permissionType: PermissionType): PermissionResult {
        return permissionsManager.requestPermission(
            activity = this@BasePermissionActivity,
            permission = permissionType.permission
        )
    }

    /**
     * 处理权限请求结果
     *
     * 子类Activity重写onRequestPermissionsResult时，必须调用此方法
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 尝试让PermissionsManager处理
        if (!permissionsManager.handlePermissionsResult(requestCode, permissions, grantResults)) {
            // 如果PermissionsManager没有处理，则调用子类的回调
            onPermissionResultHandled(requestCode, permissions, grantResults)
        }
    }

    /**
     * 当权限结果未被PermissionsManager处理时调用
     *
     * 子类可以重写此方法来处理自己的权限请求
     */
    protected open fun onPermissionResultHandled(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // 默认空实现
    }
}
