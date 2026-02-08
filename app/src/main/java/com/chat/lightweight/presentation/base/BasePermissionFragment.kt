package com.chat.lightweight.presentation.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.chat.lightweight.domain.permission.PermissionsManager

/**
 * 基础Fragment
 *
 * 集成权限管理功能，所有需要请求权限的Fragment都应继承此类
 */
abstract class BasePermissionFragment : Fragment() {

    protected lateinit var permissionsManager: PermissionsManager
        private set

    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionsManager = PermissionsManager.getInstance(requireContext())
    }

    /**
     * 处理权限请求结果
     *
     * 子类Fragment重写onRequestPermissionsResult时，必须调用此方法
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 尝试让PermissionsManager处理
        if (!permissionsManager.handleFragmentPermissionsResult(
                this,
                requestCode,
                permissions,
                grantResults
            )
        ) {
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
