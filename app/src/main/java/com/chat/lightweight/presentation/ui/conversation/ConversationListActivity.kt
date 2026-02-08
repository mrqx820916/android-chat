package com.chat.lightweight.presentation.ui.conversation

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chat.lightweight.databinding.ActivityConversationListBinding
import com.chat.lightweight.presentation.ui.base.BaseActivity
import com.chat.lightweight.ui.conversation.ConversationViewModel
import com.chat.lightweight.presentation.ui.chat.ChatDetailActivity
import kotlinx.coroutines.launch

/**
 * 对话列表Activity
 */
class ConversationListActivity : BaseActivity<ActivityConversationListBinding>() {

    // private lateinit var adapter: ConversationAdapter
    private val viewModel: ConversationViewModel by viewModels()

    override fun inflateBinding(): ActivityConversationListBinding {
        return ActivityConversationListBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        observeViewModel()
        loadConversations()
    }

    private fun setupUI() {
        supportActionBar?.title = "消息"

        // TODO: 下拉刷新
        // 暂时注释掉，因为 binding 视图引用有问题
        /*
        binding.swipeRefresh.setOnRefreshListener {
            loadConversations()
        }
        */
    }

    private fun setupRecyclerView() {
        // TODO: 设置 RecyclerView
        // 暂时注释掉，因为 binding 视图引用有问题
        /*
        adapter = ConversationAdapter(
            onItemClick = { conversation ->
                openChatDetail(conversation.id, conversation.memberId, conversation.memberName)
            }
        )

        binding.recyclerViewConversations.apply {
            layoutManager = LinearLayoutManager(this@ConversationListActivity)
            adapter = this@ConversationListActivity.adapter
        }
        */
    }

    private fun observeViewModel() {
        // TODO: 观察 ViewModel
        // 暂时注释掉，因为 binding 视图引用有问题
        /*
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submitList(state.conversations)

                // 隐藏加载状态
                binding.swipeRefresh.isRefreshing = state.isRefreshing
            }
        }
        */
    }

    private fun loadConversations() {
        viewModel.loadConversations()
    }

    private fun openChatDetail(conversationId: String, memberId: String?, memberName: String?) {
        val intent = Intent(this, ChatDetailActivity::class.java).apply {
            putExtra(ChatDetailActivity.CONVERSATION_ID, conversationId)
            putExtra(ChatDetailActivity.MEMBER_ID, memberId)
            putExtra(ChatDetailActivity.MEMBER_NAME, memberName)
            putExtra(ChatDetailActivity.IS_ADMIN, false) // TODO: 从用户数据获取
        }
        startActivity(intent)
    }
}
