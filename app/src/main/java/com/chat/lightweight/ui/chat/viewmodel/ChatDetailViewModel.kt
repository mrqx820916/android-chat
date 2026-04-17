package com.chat.lightweight.ui.chat.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chat.lightweight.data.model.MessageItem
import com.chat.lightweight.data.repository.ChatRepositoryImpl
import com.chat.lightweight.domain.model.MessageType
import com.chat.lightweight.media.MediaUtils
import com.chat.lightweight.network.NetworkRepository
import com.chat.lightweight.socket.NewMessageEvent
import com.chat.lightweight.socket.MessagesReadEvent
import com.chat.lightweight.socket.SocketClient
import com.chat.lightweight.socket.SocketEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * 聊天详情ViewModel
 * 管理消息列表、发送消息、处理Socket事件
 */
class ChatDetailViewModel(
    private val context: Context
) : ViewModel() {

    private val chatRepository: ChatRepositoryImpl
        get() = ChatRepositoryImpl.getInstance(context)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>()
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private var conversationId: String = ""
    private var memberId: String? = null
    var currentUserId: String = ""
        private set
    var isAdmin: Boolean = false
        private set

    // 临时消息缓存（用于乐观更新）
    private val pendingMessages = mutableMapOf<String, MessageItem>()

    init {
        observeSocketEvents()
    }

    /**
     * 初始化对话
     */
    fun initConversation(
        convId: String,
        mId: String?,
        mName: String?,
        admin: Boolean
    ) {
        android.util.Log.d("ChatViewModel", "initConversation: 开始 - conversationId=$convId")
        conversationId = convId
        memberId = mId
        isAdmin = admin

        viewModelScope.launch {
            // 获取当前用户ID
            val userId = chatRepository.getUserId()
            android.util.Log.d("ChatViewModel", "initConversation: getUserId()返回=$userId")

            if (userId != null) {
                currentUserId = userId
                android.util.Log.d("ChatViewModel", "initConversation: 获取到currentUserId=$currentUserId")
            } else {
                android.util.Log.e("ChatViewModel", "initConversation: userId为空！使用备用方案")
                // 备用方案：使用memberId作为当前用户ID（临时方案）
                currentUserId = mId ?: ""
                android.util.Log.d("ChatViewModel", "initConversation: 使用备用currentUserId=$currentUserId")
            }

            loadMessages()

            // 加入Socket房间
            SocketClient.joinConversation(conversationId)
        }
    }

    /**
     * 加载消息
     */
    private fun loadMessages() {
        android.util.Log.d("ChatViewModel", "loadMessages: 开始加载 - conversationId=$conversationId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = chatRepository.getMessages(conversationId)
            result.onSuccess { messages ->
                android.util.Log.d("ChatViewModel", "loadMessages: 加载成功 - ${messages.size}条消息")
                // 将domain.Message转换为MessageItem
                val messageItems = messages.map { msg ->
                    MessageItem.fromDomainMessage(msg, currentUserId)
                }
                _uiState.update { state ->
                    state.copy(
                        messages = messageItems,
                        isLoading = false,
                        shouldScrollToBottom = true
                    )
                }
                android.util.Log.d("ChatViewModel", "loadMessages: UI状态已更新 - ${messageItems.size}条消息")
            }.onFailure { error ->
                android.util.Log.e("ChatViewModel", "loadMessages: 加载失败 - ${error.message}")
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(ChatEvent.LoadMessagesFailed(error.message ?: "加载失败"))
            }
        }
    }

    /**
     * 加载更多历史消息
     */
    fun loadMoreMessages() {
        // TODO: 实现分页加载
    }

    /**
     * 发送文本消息（乐观更新）
     */
    fun sendTextMessage(content: String) {
        if (content.isBlank()) return

        // 1. 创建临时消息
        val tempMessage = MessageItem.createTemp(
            conversationId = conversationId,
            senderId = currentUserId,
            content = content.trim(),
            messageType = MessageItem.TYPE_TEXT
        )

        android.util.Log.d("ChatViewModel", "sendTextMessage: 创建临时消息 - tempId=${tempMessage.tempId}, content=${content.trim()}")

        // 2. 添加到列表（乐观更新）
        addMessage(tempMessage)

        // 3. 发送到服务器
        viewModelScope.launch {
            val request = com.chat.lightweight.domain.model.SendMessageRequest(
                conversationId = conversationId,
                content = content.trim(),
                messageType = com.chat.lightweight.domain.model.MessageType.TEXT.value,
                fileUrl = null,
                tempId = tempMessage.tempId
            )

            android.util.Log.d("ChatViewModel", "sendTextMessage: 发送请求 - tempId=${tempMessage.tempId}")

            val result = chatRepository.sendMessage(request)
            result.onSuccess { /* Socket会通知更新 */ }
                .onFailure { error ->
                    android.util.Log.e("ChatViewModel", "sendTextMessage: 发送失败 - tempId=${tempMessage.tempId}, error=${error.message}")
                    // 标记为失败
                    updateMessageStatus(tempMessage.tempId ?: "", MessageItem.Status.FAILED)
                    _events.emit(ChatEvent.SendMessageFailed(error.message ?: "发送失败"))
                }
        }
    }

    /**
     * 发送图片消息
     */
    fun sendImageMessage(imageUri: String) {
        viewModelScope.launch {
            // 创建临时消息
            val tempMessage = MessageItem.createTemp(
                conversationId = conversationId,
                senderId = currentUserId,
                content = "图片",
                messageType = MessageItem.TYPE_IMAGE
            ).copy(fileUrl = imageUri)  // 暂时用本地URI做预览
            addMessage(tempMessage)

            try {
                // 1. 将URI转为File
                val uri = Uri.parse(imageUri)
                val file = MediaUtils.getFileFromUri(context, uri)

                if (file == null) {
                    updateMessageStatus(tempMessage.tempId ?: "", MessageItem.Status.FAILED)
                    _events.emit(ChatEvent.SendMessageFailed("无法读取图片文件"))
                    return@launch
                }

                // 2. 上传图片文件
                val uploadResult = NetworkRepository.getInstance().uploadFile(file, "member")

                when (uploadResult) {
                    is com.chat.lightweight.network.ApiResponse.Success -> {
                        val fileUrl = uploadResult.data

                        // 3. 发送消息请求
                        val request = com.chat.lightweight.domain.model.SendMessageRequest(
                            conversationId = conversationId,
                            content = "图片",
                            messageType = MessageType.IMAGE.value,
                            fileUrl = fileUrl,
                            tempId = tempMessage.tempId
                        )

                        val result = chatRepository.sendMessage(request)
                        result.onFailure { error ->
                            updateMessageStatus(tempMessage.tempId ?: "", MessageItem.Status.FAILED)
                            _events.emit(ChatEvent.SendMessageFailed(error.message ?: "发送失败"))
                        }
                    }
                    is com.chat.lightweight.network.ApiResponse.Error -> {
                        updateMessageStatus(tempMessage.tempId ?: "", MessageItem.Status.FAILED)
                        _events.emit(ChatEvent.SendMessageFailed("图片上传失败: ${uploadResult.message}"))
                    }
                    else -> {
                        updateMessageStatus(tempMessage.tempId ?: "", MessageItem.Status.FAILED)
                        _events.emit(ChatEvent.SendMessageFailed("图片上传失败"))
                    }
                }
            } catch (e: Exception) {
                updateMessageStatus(tempMessage.tempId ?: "", MessageItem.Status.FAILED)
                _events.emit(ChatEvent.SendMessageFailed(e.message ?: "发送失败"))
            }
        }
    }

    /**
     * 发送语音消息
     */
    fun sendVoiceMessage(file: File, duration: Long) {
        viewModelScope.launch {
            // 创建临时消息
            val tempMessage = MessageItem.createTemp(
                conversationId = conversationId,
                senderId = currentUserId,
                content = com.chat.lightweight.media.MediaUtils.formatDuration(duration),
                messageType = MessageItem.TYPE_VOICE
            )
            addMessage(tempMessage)

            try {
                android.util.Log.d("ChatViewModel", "开始上传语音文件: ${file.absolutePath}")

                // 上传语音文件
                val uploadResult = NetworkRepository.getInstance().uploadFile(file, "member")

                when (uploadResult) {
                    is com.chat.lightweight.network.ApiResponse.Success -> {
                        val fileUrl = uploadResult.data
                        android.util.Log.d("ChatViewModel", "语音文件上传成功: $fileUrl")

                        // 发送消息请求
                        val request = com.chat.lightweight.domain.model.SendMessageRequest(
                            conversationId = conversationId,
                            content = com.chat.lightweight.media.MediaUtils.formatDuration(duration),
                            messageType = MessageType.VOICE.value,
                            fileUrl = fileUrl,
                            tempId = tempMessage.tempId
                        )

                        val result = chatRepository.sendMessage(request)
                        result.onFailure { error ->
                            android.util.Log.e("ChatViewModel", "发送语音消息失败: ${error.message}")
                            updateMessageStatus(tempMessage.tempId ?: "", MessageItem.Status.FAILED)
                            _events.emit(ChatEvent.SendMessageFailed(error.message ?: "发送失败"))
                        }
                    }
                    is com.chat.lightweight.network.ApiResponse.Error -> {
                        android.util.Log.e("ChatViewModel", "语音文件上传失败: ${uploadResult.message}")
                        updateMessageStatus(tempMessage.tempId ?: "", MessageItem.Status.FAILED)
                        _events.emit(ChatEvent.SendMessageFailed("上传失败: ${uploadResult.message}"))
                    }
                    else -> {
                        android.util.Log.e("ChatViewModel", "语音文件上传: 未知错误")
                        updateMessageStatus(tempMessage.tempId ?: "", MessageItem.Status.FAILED)
                        _events.emit(ChatEvent.SendMessageFailed("上传失败"))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "发送语音消息异常", e)
                updateMessageStatus(tempMessage.tempId ?: "", MessageItem.Status.FAILED)
                _events.emit(ChatEvent.SendMessageFailed(e.message ?: "发送失败"))
            } finally {
                // 清理临时文件
                com.chat.lightweight.media.MediaUtils.deleteFile(file)
            }
        }
    }

    /**
     * 重试发送失败的消息
     */
    fun retrySendMessage(message: MessageItem) {
        when (message.messageType) {
            MessageItem.TYPE_TEXT -> sendTextMessage(message.content)
            MessageItem.TYPE_IMAGE -> message.fileUrl?.let { sendImageMessage(it) }
            else -> {}
        }
    }

    /**
     * 删除消息
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            val result = chatRepository.deleteMessage(messageId)
            result.fold(
                onSuccess = {
                    _events.emit(ChatEvent.DeleteMessageSuccess)
                },
                onFailure = { error ->
                    _events.emit(ChatEvent.DeleteMessageFailed(error.message ?: "删除失败"))
                }
            )
        }
    }

    /**
     * 标记为已读
     */
    fun markAsRead() {
        viewModelScope.launch {
            chatRepository.markConversationRead(conversationId)
        }
    }

    /**
     * 观察Socket事件
     */
    private fun observeSocketEvents() {
        viewModelScope.launch {
            SocketClient.getSocketEvents().collect { event ->
                when (event) {
                    is SocketEvent.NewMessage -> {
                        handleNewMessage(event.message)
                    }
                    is SocketEvent.MessageDeleted -> {
                        handleMessageDeleted(event.event)
                    }
                    is SocketEvent.MessageSendFailed -> {
                        handleMessageSendFailed(event.event)
                    }
                    is SocketEvent.MessagesRead -> {
                        handleMessagesRead(event.event)
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 处理新消息
     */
    private fun handleNewMessage(event: NewMessageEvent) {
        // 过滤不属于当前对话的消息
        if (event.conversation_id != conversationId) return

        android.util.Log.d("ChatViewModel", "handleNewMessage: 收到消息 - id=${event.id}, temp_id=${event.temp_id}, sender=${event.sender_id}")

        val messageItem = MessageItem.fromSocketEvent(event, currentUserId)

        // 如果有tempId，更新临时消息状态
        if (event.temp_id != null) {
            android.util.Log.d("ChatViewModel", "handleNewMessage: 匹配临时消息 - temp_id=${event.temp_id}")
            updateMessageStatus(event.temp_id, MessageItem.Status.SENT, messageItem)
        } else {
            android.util.Log.d("ChatViewModel", "handleNewMessage: 添加新消息 - temp_id为null")
            // 添加新消息
            addMessage(messageItem)
        }

        // 收到对方的消息时，自动标记已读
        if (event.sender_id != currentUserId) {
            markAsRead()
        }
    }

    /**
     * 处理消息删除
     */
    private fun handleMessageDeleted(event: com.chat.lightweight.socket.MessageDeletedEvent) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == event.messageId) {
                        message.copy(isDeleted = true, status = MessageItem.Status.DELETED)
                    } else {
                        message
                    }
                }
            )
        }
    }

    /**
     * 处理消息发送失败
     */
    private fun handleMessageSendFailed(event: com.chat.lightweight.socket.MessageSendFailedEvent) {
        updateMessageStatus(event.tempMessageId, MessageItem.Status.FAILED)
    }

    /**
     * 处理消息已读回执
     */
    private fun handleMessagesRead(event: MessagesReadEvent) {
        if (event.conversationId != conversationId) return

        val readIdSet = event.messageIds.toSet()
        if (readIdSet.isEmpty()) return

        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id in readIdSet && message.status != MessageItem.Status.DELETED) {
                        message.copy(status = MessageItem.Status.READ, isRead = true)
                    } else {
                        message
                    }
                }
            )
        }
    }

    /**
     * 添加消息到列表
     */
    private fun addMessage(message: MessageItem) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages + message,
                shouldScrollToBottom = true
            )
        }
    }

    /**
     * 更新消息状态
     */
    private fun updateMessageStatus(tempId: String, status: MessageItem.Status, newMessage: MessageItem? = null) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.tempId == tempId) {
                        if (newMessage != null) {
                            newMessage.copy(status = status)
                        } else {
                            message.copy(status = status)
                        }
                    } else {
                        message
                    }
                },
                shouldScrollToBottom = true  // 更新消息状态时也滚动
            )
        }
    }

    /**
     * 重置滚动状态（在UI完成滚动后调用）
     */
    fun onScrollCompleted() {
        _uiState.update { it.copy(shouldScrollToBottom = false) }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        // ViewModel销毁时清理
    }

    /**
     * 聊天UI状态
     */
    data class ChatUiState(
        val isLoading: Boolean = false,
        val messages: List<MessageItem> = emptyList(),
        val shouldScrollToBottom: Boolean = false,
        val canSend: Boolean = true
    )

    /**
     * 聊天事件
     */
    sealed class ChatEvent {
        data class SendMessageFailed(val error: String) : ChatEvent()
        object SendMessageSuccess : ChatEvent()
        object DeleteMessageSuccess : ChatEvent()
        data class DeleteMessageFailed(val error: String) : ChatEvent()
        data class LoadMessagesFailed(val error: String) : ChatEvent()
    }
}
