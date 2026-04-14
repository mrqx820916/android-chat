package com.chat.lightweight.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chat.lightweight.R
import com.chat.lightweight.databinding.ActivityChatDetailBinding
import com.chat.lightweight.presentation.viewmodel.ViewModelFactory
import com.chat.lightweight.ui.chat.adapter.MessageAdapter
import com.chat.lightweight.ui.chat.viewmodel.ChatDetailViewModel
import com.chat.lightweight.media.VoiceRecordingDialog
import com.chat.lightweight.permission.PermissionsManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 聊天详情页面
 * 显示单个对话的消息列表，支持发送文本、图片消息
 */
class ChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private lateinit var viewModel: ChatDetailViewModel
    private lateinit var messageAdapter: MessageAdapter  // 延迟初始化

    // 生命周期标志位（修复加号按钮跳转问题）
    private var isActivityActive = false

    // 录音权限请求启动器
    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，启动录音
            startVoiceRecordingInternal()
        } else {
            // 权限被拒绝
            Snackbar.make(
                binding.root,
                "需要录音权限才能发送语音消息",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_MEMBER_NAME = "member_name"
        const val EXTRA_MEMBER_ID = "member_id"
        const val EXTRA_IS_ADMIN = "is_admin"
        private const val REQUEST_IMAGE_PICK = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 创建ViewModel
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory.getInstance(application)
        )[ChatDetailViewModel::class.java]

        setupToolbar()
        setupRecyclerView()  // 先初始化RecyclerView的LayoutManager
        setupInput()         // 先初始化输入框
        loadConversation()   // 再加载对话信息
        observeUiState()     // 观察UI状态
    }

    /**
     * 加载对话信息
     */
    private fun loadConversation() {
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: ""
        val memberId = intent.getStringExtra(EXTRA_MEMBER_ID)
        val memberName = intent.getStringExtra(EXTRA_MEMBER_NAME) ?: "未知"
        val isAdmin = intent.getBooleanExtra(EXTRA_IS_ADMIN, false)

        // 先初始化对话，然后再设置RecyclerView
        viewModel.initConversation(
            convId = conversationId,
            mId = memberId,
            mName = memberName,
            admin = isAdmin
        )
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra(EXTRA_MEMBER_NAME) ?: "聊天"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatDetailActivity).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupMessageAdapter() {
        val menuHandler = com.chat.lightweight.ui.menu.MessageMenuHandler(
            onMessageDelete = { messageId -> viewModel.deleteMessage(messageId) },
            onMessageCopy = { content ->
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("消息", content))
                Snackbar.make(binding.root, "已复制", Snackbar.LENGTH_SHORT).show()
            },
            onMessageRecall = { messageId -> viewModel.deleteMessage(messageId) },
            isAdmin = viewModel.isAdmin
        )

        messageAdapter = MessageAdapter(
            currentUserId = viewModel.currentUserId,
            isAdmin = viewModel.isAdmin,
            onRetryClick = { message ->
                viewModel.retrySendMessage(message)
            },
            onDeleteClick = { message ->
                menuHandler.showDeleteConfirmDialog(this) {
                    viewModel.deleteMessage(message.id)
                }
            },
            onImageClick = { imageUrl ->
                openImagePreview(imageUrl)
            },
            onMessageLongClick = { view, message ->
                menuHandler.show(view, message.id, message.content)
            }
        )

        binding.rvMessages.adapter = messageAdapter

        // 立即提交当前UI状态的消息数据（修复消息列表空白问题）
        viewModel.uiState.value.let { state ->
            if (state.messages.isNotEmpty()) {
                messageAdapter.submitList(state.messages)
                android.util.Log.d("ChatDetail", "setupMessageAdapter: 提交了 ${state.messages.size} 条缓存消息")
            }
        }
    }

    private fun setupInput() {
        // 发送按钮
        binding.btnSend.setOnClickListener {
            sendTextMessage()
        }

        // 发送按钮初始灰色
        updateSendButtonState("")

        // 输入框内容变化时更新发送按钮状态
        binding.etMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateSendButtonState(s?.toString()?.trim() ?: "")
            }
        })

        // 输入框回车发送
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTextMessage()
                true
            } else {
                false
            }
        }

        // 表情按钮
        binding.tilMessage.setEndIconOnClickListener {
            // 切换软键盘/表情面板
            toggleEmojiPanel()
        }

        // 添加按钮（图片/附件/语音）
        binding.btnAdd.setOnClickListener {
            showAddOptions()
        }
    }

    /**
     * 切换表情面板
     */
    private fun toggleEmojiPanel() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)

        val emojis = arrayOf(
            "😊", "😂", "👍", "❤️", "🎉", "🙏", "💪", "🔥",
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😘",
            "😍", "🥰", "😜", "🤔", "😴", "🤩", "😎", "🥺",
            "😭", "😱", "😤", "👏", "🙌", "💯", "✨", "⭐"
        )

        val gridView = android.widget.GridView(this).apply {
            numColumns = 6
            verticalSpacing = 8
            horizontalSpacing = 8
            setPadding(16, 16, 16, 16)
            stretchMode = android.widget.GridView.STRETCH_COLUMN_WIDTH

            adapter = object : android.widget.BaseAdapter() {
                override fun getCount(): Int = emojis.size
                override fun getItem(position: Int): Any = emojis[position]
                override fun getItemId(position: Int): Long = position.toLong()

                override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                    val textView = (convertView as? android.widget.TextView) ?: android.widget.TextView(this@ChatDetailActivity).apply {
                        textSize = 32f
                        gravity = android.view.Gravity.CENTER
                        setPadding(8, 8, 8, 8)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        // 不设置 isClickable，让点击事件传递到 GridView 的 onItemClickListener
                    }
                    textView.text = emojis[position]
                    return textView
                }
            }

            onItemClickListener = android.widget.AdapterView.OnItemClickListener { _, _, position, _ ->
                binding.etMessage.text?.append(emojis[position])
                bottomSheetDialog.dismiss()
            }
        }

        bottomSheetDialog.setContentView(gridView)
        bottomSheetDialog.show()
    }

    /**
     * 显示软键盘
     */
    private fun showKeyboard() {
        binding.etMessage.post {
            binding.etMessage.requestFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.etMessage, 0)
        }
    }

    /**
     * 隐藏软键盘
     */
    private fun hideKeyboard() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etMessage.windowToken, 0)
    }

    /**
     * 显示添加选项
     */
    private fun showAddOptions() {
        // 生命周期保护（修复加号按钮跳转问题）
        if (!isActivityActive || isFinishing || isDestroyed) {
            android.util.Log.w("ChatDetail", "showAddOptions: Activity不可用，跳过显示对话框")
            return
        }

        // 使用Material3兼容的BottomSheetDialog
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(
            android.R.layout.simple_list_item_1, null
        ) as android.widget.TextView

        // 创建选项列表
        val options = arrayOf("发送图片", "发送语音")
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            options
        )

        // 创建ListView
        val listView = android.widget.ListView(this).apply {
            this.adapter = adapter
            dividerHeight = 1
            setOnItemClickListener { _, _, which, _ ->
                when (which) {
                    0 -> openImagePicker()
                    1 -> startVoiceRecording()
                }
                bottomSheetDialog.dismiss()
            }
        }

        bottomSheetDialog.setContentView(listView)
        bottomSheetDialog.show()
    }

    /**
     * 开始录音
     */
    private fun startVoiceRecording() {
        // 检查录音权限
        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // 已有权限，直接启动录音
            startVoiceRecordingInternal()
        } else {
            // 请求录音权限
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    /**
     * 内部录音启动方法
     */
    private fun startVoiceRecordingInternal() {
        try {
            val dialog = VoiceRecordingDialog(
                context = this,
                onRecordingComplete = { file, duration ->
                    // 上传语音文件并发送消息
                    android.util.Log.d("ChatDetail", "录音完成: 文件=${file.absolutePath}, 时长=${duration}ms")
                    viewModel.sendVoiceMessage(file, duration)
                },
                onRecordingCancel = {
                    android.util.Log.d("ChatDetail", "录音取消")
                }
            )
            dialog.show()
            dialog.startRecording()
        } catch (e: Exception) {
            android.util.Log.e("ChatDetail", "启动录音失败", e)
            Snackbar.make(
                binding.root,
                "启动录音失败: ${e.message}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // 首次获取到currentUserId时初始化MessageAdapter
                if (!::messageAdapter.isInitialized && viewModel.currentUserId.isNotEmpty()) {
                    setupMessageAdapter()
                }
                updateUi(state)
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun updateUi(state: ChatDetailViewModel.ChatUiState) {
        // 只在MessageAdapter初始化后才更新消息列表
        if (::messageAdapter.isInitialized) {
            android.util.Log.d("ChatDetail", "updateUi: 提交 ${state.messages.size} 条消息")
            messageAdapter.submitList(state.messages)

            // 滚动到底部（新消息）- 延迟执行确保DiffUtil完成
            if (state.shouldScrollToBottom) {
                binding.rvMessages.post {
                    if (messageAdapter.itemCount > 0) {
                        binding.rvMessages.smoothScrollToPosition(
                            messageAdapter.itemCount - 1
                        )
                        // 滚动完成后重置状态
                        viewModel.onScrollCompleted()
                    }
                }
            }
        } else {
            android.util.Log.w("ChatDetail", "updateUi: MessageAdapter未初始化，跳过 ${state.messages.size} 条消息")
        }

        // 显示/隐藏加载进度
        binding.progressLoading.isVisible = state.isLoading
    }

    private fun handleEvent(event: ChatDetailViewModel.ChatEvent) {
        when (event) {
            is ChatDetailViewModel.ChatEvent.SendMessageFailed -> {
                Snackbar.make(
                    binding.root,
                    event.error,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            is ChatDetailViewModel.ChatEvent.LoadMessagesFailed -> {
                Snackbar.make(
                    binding.root,
                    event.error,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            else -> {}
        }
    }

    private fun sendTextMessage() {
        val content = binding.etMessage.text?.toString()?.trim() ?: ""
        if (content.isNotEmpty()) {
            viewModel.sendTextMessage(content)
            binding.etMessage.text?.clear()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_IMAGE_PICK)
    }

    private fun scrollToBottom() {
        if (messageAdapter.itemCount > 0) {
            binding.rvMessages.smoothScrollToPosition(
                messageAdapter.itemCount - 1
            )
        }
    }

    /**
     * 打开图片预览
     */
    private fun openImagePreview(imageUrl: String) {
        val intent = Intent(this, ImagePreviewActivity::class.java).apply {
            putExtra(ImagePreviewActivity.EXTRA_IMAGE_URL, imageUrl)
        }
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                viewModel.sendImageMessage(uri.toString())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityActive = true
        viewModel.markAsRead()
    }

    override fun onPause() {
        super.onPause()
        isActivityActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanup()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.cleanup()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * 根据输入内容更新发送按钮状态
     * 有内容时高亮（主题色），无内容时灰色
     */
    private fun updateSendButtonState(text: String) {
        val hasContent = text.isNotEmpty()
        binding.btnSend.isEnabled = hasContent
        binding.btnSend.alpha = if (hasContent) 1.0f else 0.4f
    }
}
