package com.chat.lightweight.presentation.ui.chat

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chat.lightweight.databinding.ActivityChatDetailBinding
import com.chat.lightweight.presentation.ui.base.BaseActivity
import com.chat.lightweight.ui.chat.viewmodel.ChatDetailViewModel
import kotlinx.coroutines.launch

/**
 * 聊天详情Activity
 */
class ChatDetailActivity : BaseActivity<ActivityChatDetailBinding>() {

    // private lateinit var adapter: MessageAdapter
    private val viewModel: ChatDetailViewModel by viewModels()

    private var conversationId: String = ""
    private var memberId: String? = null
    private var memberName: String? = null
    private var isAdmin: Boolean = false

    override fun inflateBinding(): ActivityChatDetailBinding {
        return ActivityChatDetailBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 获取传递的参数
        conversationId = intent.getStringExtra(CONVERSATION_ID) ?: ""
        memberId = intent.getStringExtra(MEMBER_ID)
        memberName = intent.getStringExtra(MEMBER_NAME)
        isAdmin = intent.getBooleanExtra(IS_ADMIN, false)

        setupUI()
        setupRecyclerView()
        observeViewModel()
        initConversation()
    }

    private fun setupUI() {
        // 设置标题
        supportActionBar?.title = memberName ?: "聊天"

        // TODO: 设置返回按钮和发送消息按钮
        // 暂时注释掉，因为 binding 视图引用有问题
        /*
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }
        */
    }

    private fun setupRecyclerView() {
        // TODO: 设置 RecyclerView
        // 暂时注释掉，因为 binding 视图引用有问题
        /*
        adapter = MessageAdapter(
            onRetryClick = { message ->
                // 重试发送失败的消息
                viewModel.retrySendMessage(message)
            },
            onDeleteClick = { message ->
                // 删除消息
                viewModel.deleteMessage(message.id)
            }
        )

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatDetailActivity)
            adapter = this@ChatDetailActivity.adapter
        }
        */
    }

    private fun observeViewModel() {
        // TODO: 观察 ViewModel
        // 暂时注释掉，因为 binding 视图引用有问题
        /*
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // 更新消息列表
                adapter.submitList(state.messages)

                // 滚动到底部
                if (state.shouldScrollToBottom) {
                    binding.recyclerViewMessages.scrollToPosition(
                        adapter.itemCount - 1
                    )
                }

                // 显示加载状态
                if (state.isLoading) {
                    // 显示加载指示器
                }
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is ChatDetailViewModel.ChatEvent.SendMessageFailed -> {
                        Toast.makeText(this@ChatDetailActivity, event.error, Toast.LENGTH_SHORT).show()
                    }
                    is ChatDetailViewModel.ChatEvent.SendMessageSuccess -> {
                        // 处理发送成功
                    }
                    is ChatDetailViewModel.ChatEvent.DeleteMessageSuccess -> {
                        Toast.makeText(this@ChatDetailActivity, "消息已删除", Toast.LENGTH_SHORT).show()
                    }
                    is ChatDetailViewModel.ChatEvent.DeleteMessageFailed -> {
                        Toast.makeText(this@ChatDetailActivity, event.error, Toast.LENGTH_SHORT).show()
                    }
                    is ChatDetailViewModel.ChatEvent.LoadMessagesFailed -> {
                        Toast.makeText(this@ChatDetailActivity, event.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        */
    }

    private fun initConversation() {
        viewModel.initConversation(
            convId = conversationId,
            mId = memberId,
            mName = memberName,
            admin = isAdmin
        )
        viewModel.markAsRead()
    }

    private fun sendMessage() {
        val content = binding.etMessage.text.toString().trim()
        if (content.isEmpty()) return

        viewModel.sendTextMessage(content)
        binding.etMessage.text?.clear()
    }

    companion object {
        const val CONVERSATION_ID = "conversation_id"
        const val MEMBER_ID = "member_id"
        const val MEMBER_NAME = "member_name"
        const val IS_ADMIN = "is_admin"
    }
}
