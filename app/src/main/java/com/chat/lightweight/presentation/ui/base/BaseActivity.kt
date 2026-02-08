package com.chat.lightweight.presentation.ui.base

import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * 基础Activity类
 * 提供ViewBinding支持
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("Binding cannot be accessed before onCreate or after onDestroy")

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    /**
     * 子类需要实现此方法来提供ViewBinding实例
     */
    protected abstract fun inflateBinding(): VB

    override fun setContentView(layoutResID: Int) {
        throw IllegalArgumentException("Use inflateBinding() instead of setContentView()")
    }
}
