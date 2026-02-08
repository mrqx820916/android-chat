package com.chat.lightweight.presentation.ui.base

import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * 基础Fragment类
 * 提供ViewBinding支持
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("Binding cannot be accessed before onViewCreated or after onDestroyView")

    /**
     * 子类需要实现此方法来提供ViewBinding实例
     */
    protected abstract fun inflateBinding(): VB

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
