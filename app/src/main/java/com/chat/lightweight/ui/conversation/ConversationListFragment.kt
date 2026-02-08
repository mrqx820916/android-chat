package com.chat.lightweight.ui.conversation

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chat.lightweight.databinding.FragmentConversationListBinding
import com.chat.lightweight.ui.chat.ChatDetailActivity
import com.chat.lightweight.presentation.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * 对话列表Fragment
 *
 * 功能:
 * - 显示对话列表
 * - 下拉刷新
 * - 点击进入聊天详情
 */
class ConversationListFragment : Fragment() {

    private var _binding: FragmentConversationListBinding? = null
    private val binding get() = _binding!!

    // 创建ViewModel实例
    private val viewModel: ConversationViewModel by viewModels {
        ViewModelFactory.getInstance(requireContext().applicationContext as Application)
    }

    private lateinit var adapter: ConversationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        observeUiState()
        viewModel.loadConversations()  // 立即加载数据
    }

    /**
     * 初始化RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = ConversationAdapter { conversation ->
            navigateToChatDetail(conversation.id, conversation.userId, conversation.username)
        }

        binding.conversationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ConversationListFragment.adapter
        }
    }

    /**
     * 设置下拉刷新
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshConversations()
        }
    }

    /**
     * 观察UI状态
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                renderUiState(state)
            }
        }
    }

    /**
     * 渲染UI状态
     */
    private fun renderUiState(state: ConversationListUiState) {
        // 更新对话列表
        adapter.submitList(state.conversations)

        // 更新加载状态
        binding.swipeRefreshLayout.isRefreshing = state.isRefreshing

        // 更新错误提示
        if (state.error != null) {
            showError(state.error)
            viewModel.clearError()
        }
    }

    /**
     * 显示错误提示
     */
    private fun showError(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    /**
     * 跳转到聊天详情
     */
    private fun navigateToChatDetail(
        conversationId: String,
        otherUserId: String,
        otherUsername: String
    ) {
        // 标记对话为已读
        viewModel.markConversationRead(conversationId)

        // 跳转到聊天详情页
        val intent = Intent(requireContext(), ChatDetailActivity::class.java).apply {
            putExtra(ChatDetailActivity.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(ChatDetailActivity.EXTRA_MEMBER_ID, otherUserId)
            putExtra(ChatDetailActivity.EXTRA_MEMBER_NAME, otherUsername)
        }
        startActivity(intent)
    }

    /**
     * 处理返回键
     */
    fun onBackPressed(): Boolean {
        return false
    }

    override fun onResume() {
        super.onResume()
        // 每次返回时刷新数据
        viewModel.loadConversations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * 创建实例的工厂方法
         */
        fun newInstance() = ConversationListFragment()
    }
}
