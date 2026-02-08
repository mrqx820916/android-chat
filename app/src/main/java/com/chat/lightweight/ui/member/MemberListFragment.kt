package com.chat.lightweight.ui.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chat.lightweight.databinding.FragmentMemberListBinding

/**
 * 成员列表Fragment（占位）
 * TODO: 实现成员管理功能
 */
class MemberListFragment : Fragment() {

    private var _binding: FragmentMemberListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
