package com.chat.lightweight.ui.member

import android.app.AlertDialog
import android.app.Application
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chat.lightweight.R
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.data.model.MemberItem
import com.chat.lightweight.databinding.DialogEditNoteBinding
import com.chat.lightweight.databinding.FragmentMemberManagementBinding
import com.chat.lightweight.presentation.viewmodel.ViewModelFactory
import com.chat.lightweight.ui.chat.ChatDetailActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 成员管理Fragment
 * 功能：
 * - 显示成员列表（仅管理员）
 * - 编辑成员备注
 * - 删除成员
 * - 点击进入对话
 */
class MemberManagementFragment : androidx.fragment.app.Fragment() {

    private var _binding: FragmentMemberManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MemberViewModel by viewModels {
        ViewModelFactory.getInstance(requireContext().applicationContext as Application)
    }
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var adapter: MemberAdapter
    private var editNoteDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager(requireContext())

        // 检查管理员权限
        checkAdminPermission()

        // 初始化UI
        setupUI()

        // 观察数据
        observeData()
    }

    /**
     * 检查管理员权限
     */
    private fun checkAdminPermission() {
        lifecycleScope.launch {
            preferencesManager.isAdminFlow().collect { isAdmin ->
                if (!isAdmin) {
                    // 非管理员，显示提示
                    showNotAdminHint()
                } else {
                    // 管理员，加载数据
                    val userId = preferencesManager.getUserId() ?: return@collect
                    viewModel.init(userId, true)
                }
            }
        }
    }

    /**
     * 初始化UI
     */
    private fun setupUI() {
        // 设置RecyclerView
        adapter = MemberAdapter(
            onMemberClick = { member -> navigateToChat(member) },
            onEditNote = { member -> showEditNoteDialog(member) },
            onDeleteMember = { member -> confirmDeleteMember(member) }
        )

        binding.membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MemberManagementFragment.adapter
        }

        // 设置下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshMembers()
        }

        // 添加成员FAB
        binding.addMemberFab.setOnClickListener {
            showAddMemberHint()
        }
    }

    /**
     * 观察数据
     */
    private fun observeData() {
        // 观察UI状态
        viewModel.uiState
            .onEach { state ->
                renderUiState(state)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    /**
     * 渲染UI状态
     */
    private fun renderUiState(state: MemberManagementUiState) {
        // 显示/隐藏加载进度
        binding.progressIndicator.visibility = if (state.isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // 设置刷新状态
        binding.swipeRefreshLayout.isRefreshing = state.isRefreshing

        // 更新成员列表
        adapter.submitList(state.members)

        // 更新空状态
        binding.emptyStateLayout.visibility = if (state.members.isEmpty() && !state.isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // 显示错误
        state.error?.let { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }

        // 显示添加成员FAB（仅管理员）
        binding.addMemberFab.visibility = if (state.isAdmin) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    /**
     * 显示编辑备注对话框
     */
    private fun showEditNoteDialog(member: MemberItem) {
        editNoteDialog = Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_edit_note)

            val binding = DialogEditNoteBinding.bind(window!!.decorView)

            // 设置用户名
            binding.memberNameTextView.text = "@${member.username}"

            // 设置当前备注
            binding.noteEditText.setText(member.note)

            // 取消按钮
            binding.cancelButton.setOnClickListener {
                dismiss()
            }

            // 保存按钮
            binding.saveButton.setOnClickListener {
                val newNote = binding.noteEditText.text?.toString()?.trim() ?: ""
                viewModel.updateMemberNote(member.id, newNote)
                dismiss()
            }

            show()
        }
    }

    /**
     * 确认删除成员
     */
    private fun confirmDeleteMember(member: MemberItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除成员")
            .setMessage("确定要删除成员 \"${member.username}\" 吗？\n\n删除后将无法恢复，该成员的所有对话和消息也会被删除。")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteMember(member)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 跳转到聊天详情
     */
    private fun navigateToChat(member: MemberItem) {
        // TODO: 实现跳转到与该成员的对话
        Toast.makeText(requireContext(), "与 ${member.username} 聊天", Toast.LENGTH_SHORT).show()
    }

    /**
     * 显示非管理员提示
     */
    private fun showNotAdminHint() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.membersRecyclerView.visibility = View.GONE
        binding.addMemberFab.visibility = View.GONE
    }

    /**
     * 显示添加成员提示
     */
    private fun showAddMemberHint() {
        Toast.makeText(requireContext(), "请让成员扫描二维码注册", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        editNoteDialog?.dismiss()
        _binding = null
    }
}
