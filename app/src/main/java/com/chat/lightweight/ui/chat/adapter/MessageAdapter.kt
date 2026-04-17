package com.chat.lightweight.ui.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.chat.lightweight.R
import com.chat.lightweight.data.model.MessageItem
import com.chat.lightweight.databinding.ItemMessageReceivedBinding
import com.chat.lightweight.databinding.ItemMessageSentBinding
import com.chat.lightweight.utils.MessageUtils
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 消息列表适配器
 * 支持多种消息类型：文本、图片、语音
 */
class MessageAdapter(
    private val currentUserId: String,
    private val isAdmin: Boolean,
    private val onRetryClick: (MessageItem) -> Unit,
    private val onDeleteClick: (MessageItem) -> Unit,
    private val onImageClick: (String) -> Unit,
    private val onMessageLongClick: ((View, MessageItem) -> Unit)? = null
) : ListAdapter<MessageItem, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    // ExoPlayer 播放器
    private var exoPlayer: ExoPlayer? = null
    private var currentPlayingView: android.widget.ImageView? = null
    private val animationScope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> SentMessageViewHolder(
                ItemMessageSentBinding.inflate(inflater, parent, false),
                onRetryClick,
                onDeleteClick,
                onImageClick,
                this@MessageAdapter  // 传递adapter实例
            )
            VIEW_TYPE_RECEIVED -> ReceivedMessageViewHolder(
                ItemMessageReceivedBinding.inflate(inflater, parent, false),
                onImageClick,
                this@MessageAdapter  // 传递adapter实例
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(item, isAdmin)
            is ReceivedMessageViewHolder -> holder.bind(item)
        }
    }

    /**
     * 发送的消息ViewHolder
     */
    class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding,
        private val onRetryClick: (MessageItem) -> Unit,
        private val onDeleteClick: (MessageItem) -> Unit,
        private val onImageClick: (String) -> Unit,
        private val adapter: MessageAdapter  // 添加adapter引用
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageItem, isAdmin: Boolean) {
            // 文本消息
            binding.tvMessageContent.isVisible = item.messageType == MessageItem.TYPE_TEXT
            binding.tvMessageContent.text = item.content

            // 图片消息
            binding.ivMessageImage.isVisible = item.messageType == MessageItem.TYPE_IMAGE
            if (item.messageType == MessageItem.TYPE_IMAGE) {
                item.fileUrl?.let { url ->
                    // 处理URL：如果是相对路径，添加服务器地址
                    val fullUrl = if (url.startsWith("http")) {
                        url
                    } else {
                        "https://chat.soft1688.vip/$url"
                    }
                    android.util.Log.d("MessageAdapter", "加载图片: $fullUrl")
                    binding.ivMessageImage.load(fullUrl) {
                        placeholder(R.drawable.ic_image_placeholder)
                        error(R.drawable.ic_image_error)
                        crossfade(true)
                    }
                    binding.ivMessageImage.setOnClickListener { onImageClick(fullUrl) }
                }
            }

            // 语音消息
            binding.voiceContainer.isVisible = item.messageType == MessageItem.TYPE_VOICE
            if (item.messageType == MessageItem.TYPE_VOICE) {
                binding.tvVoiceDuration.text = item.content // 显示时长
                binding.ivPlayVoice.setOnClickListener {
                    // 播放语音
                    adapter.playVoiceMessage(item.fileUrl, binding.ivPlayVoice)
                }
            }

            // 时间（使用MessageUtils智能格式化）
            binding.tvMessageTime.text = MessageUtils.formatMessageTime(item.timestamp)

            // 消息状态图标
            when (item.status) {
                MessageItem.Status.SENDING -> {
                    binding.ivMessageStatus.setImageResource(R.drawable.ic_message_sending)
                    binding.ivMessageStatus.isVisible = true
                    binding.root.setOnClickListener(null)
                }
                MessageItem.Status.SENT -> {
                    binding.ivMessageStatus.setImageResource(R.drawable.ic_message_sent)
                    binding.ivMessageStatus.isVisible = true
                    // 管理员可以删除已发送的消息
                    if (isAdmin) {
                        binding.root.setOnClickListener { onDeleteClick(item) }
                    } else {
                        binding.root.setOnClickListener(null)
                    }
                }
                MessageItem.Status.READ -> {
                    binding.ivMessageStatus.setImageResource(R.drawable.ic_message_read)
                    binding.ivMessageStatus.isVisible = true
                    if (isAdmin) {
                        binding.root.setOnClickListener { onDeleteClick(item) }
                    } else {
                        binding.root.setOnClickListener(null)
                    }
                }
                MessageItem.Status.FAILED -> {
                    binding.ivMessageStatus.setImageResource(R.drawable.ic_message_failed)
                    binding.ivMessageStatus.isVisible = true
                    binding.root.setOnClickListener { onRetryClick(item) }
                }
                MessageItem.Status.DELETED -> {
                    binding.ivMessageStatus.isVisible = false
                    binding.root.setOnClickListener(null)
                }
            }

            // 长按弹出菜单
            if (item.status != MessageItem.Status.DELETED) {
                binding.root.setOnLongClickListener { view ->
                    adapter.onMessageLongClick?.invoke(view, item)
                    true
                }
            }
        }
    }

    /**
     * 接收的消息ViewHolder
     */
    class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding,
        private val onImageClick: (String) -> Unit,
        private val adapter: MessageAdapter  // 添加adapter引用
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageItem) {
            // 文本消息
            binding.tvMessageContent.isVisible = item.messageType == MessageItem.TYPE_TEXT
            binding.tvMessageContent.text = item.content

            // 图片消息
            binding.ivMessageImage.isVisible = item.messageType == MessageItem.TYPE_IMAGE
            if (item.messageType == MessageItem.TYPE_IMAGE) {
                item.fileUrl?.let { url ->
                    // 处理URL：如果是相对路径，添加服务器地址
                    val fullUrl = if (url.startsWith("http")) {
                        url
                    } else {
                        "https://chat.soft1688.vip/$url"
                    }
                    android.util.Log.d("MessageAdapter", "加载图片: $fullUrl")
                    binding.ivMessageImage.load(fullUrl) {
                        placeholder(R.drawable.ic_image_placeholder)
                        error(R.drawable.ic_image_error)
                        crossfade(true)
                    }
                    binding.ivMessageImage.setOnClickListener { onImageClick(fullUrl) }
                }
            }

            // 语音消息
            binding.voiceContainer.isVisible = item.messageType == MessageItem.TYPE_VOICE
            if (item.messageType == MessageItem.TYPE_VOICE) {
                binding.tvVoiceDuration.text = item.content // 显示时长
                binding.ivPlayVoice.setOnClickListener {
                    // 播放语音
                    adapter.playVoiceMessage(item.fileUrl, binding.ivPlayVoice)
                }
            }

            // 时间（使用MessageUtils智能格式化）
            binding.tvMessageTime.text = MessageUtils.formatMessageTime(item.timestamp)

            // 长按弹出菜单
            binding.root.setOnLongClickListener { view ->
                adapter.onMessageLongClick?.invoke(view, item)
                true
            }
        }
    }

    /**
     * 播放语音消息
     */
    private fun playVoiceMessage(fileUrl: String?, playButton: android.widget.ImageView) {
        fileUrl ?: return

        android.util.Log.d("MessageAdapter", "播放语音: $fileUrl")

        // 如果当前正在播放这条语音，则停止播放
        if (currentPlayingView == playButton && exoPlayer?.isPlaying == true) {
            stopVoicePlayback()
            return
        }

        // 停止之前的播放
        stopVoicePlayback()

        // 处理URL
        val fullUrl = if (fileUrl.startsWith("http")) {
            fileUrl
        } else {
            "https://chat.soft1688.vip/$fileUrl"
        }

        android.util.Log.d("MessageAdapter", "完整URL: $fullUrl")

        try {
            // 初始化 ExoPlayer
            val context = playButton.context
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                // 设置播放项
                val mediaItem = MediaItem.fromUri(fullUrl)
                setMediaItem(mediaItem)

                // 设置监听器
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_IDLE -> {
                                android.util.Log.d("MessageAdapter", "播放状态: IDLE")
                            }
                            Player.STATE_BUFFERING -> {
                                android.util.Log.d("MessageAdapter", "播放状态: BUFFERING")
                            }
                            Player.STATE_READY -> {
                                android.util.Log.d("MessageAdapter", "播放状态: READY - 开始播放")
                                currentPlayingView = playButton
                                startPlayAnimation(playButton)
                            }
                            Player.STATE_ENDED -> {
                                android.util.Log.d("MessageAdapter", "播放状态: ENDED")
                                stopVoicePlayback()
                            }
                        }
                    }

                    override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                        android.util.Log.e("MessageAdapter", "播放错误: ${error.message}", error)
                        stopVoicePlayback()
                    }
                })

                // 准备并播放
                prepare()
                play()
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageAdapter", "播放语音失败", e)
            stopVoicePlayback()
        }
    }

    /**
     * 停止语音播放
     */
    private fun stopVoicePlayback() {
        android.util.Log.d("MessageAdapter", "停止播放")
        currentPlayingView?.let {
            // 恢复原始图标
            it.setImageResource(R.drawable.ic_play_voice)
            it.rotation = 0f
        }
        currentPlayingView = null
        exoPlayer?.release()
        exoPlayer = null
    }

    /**
     * 播放动画（播放按钮旋转）
     */
    private fun startPlayAnimation(playButton: android.widget.ImageView) {
        animationScope.launch {
            var rotation = 0f
            while (exoPlayer?.isPlaying == true && currentPlayingView == playButton) {
                playButton.rotation = rotation
                rotation += 10f
                delay(100)
            }
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        stopVoicePlayback()
    }

    /**
     * DiffCallback
     */
    class MessageDiffCallback : DiffUtil.ItemCallback<MessageItem>() {
        override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
            return oldItem.id == newItem.id || oldItem.tempId == newItem.tempId
        }

        override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
            return oldItem == newItem
        }
    }
}
