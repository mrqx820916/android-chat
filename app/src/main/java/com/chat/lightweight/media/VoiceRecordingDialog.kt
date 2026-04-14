package com.chat.lightweight.media

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.chat.lightweight.R
import com.chat.lightweight.databinding.DialogVoiceRecordingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

/**
 * 语音录制对话框
 * 显示录音界面和波形动画
 */
class VoiceRecordingDialog(
    context: Context,
    private val onRecordingComplete: (File, Long) -> Unit,
    private val onRecordingCancel: () -> Unit
) : Dialog(context, R.style.Theme_LightChat), LifecycleEventObserver {

    private lateinit var binding: DialogVoiceRecordingBinding
    private lateinit var voiceRecorder: VoiceRecorderHelper
    private var isRecording = false
    private val handler = Handler(Looper.getMainLooper())
    private val recordingScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogVoiceRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置对话框属性
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupRecorder()
        setupUI()
    }

    /**
     * 设置录音器
     */
    private fun setupRecorder() {
        voiceRecorder = VoiceRecorderHelper(context)

        // Dialog 没有 lifecycle，暂时不添加观察者
        // lifecycle.addObserver(voiceRecorder)
    }

    /**
     * 设置UI
     */
    private fun setupUI() {
        // 取消按钮
        binding.buttonCancel.setOnClickListener {
            if (isRecording) {
                stopRecording()
            }
            dismiss()
            onRecordingCancel()
        }

        // 发送按钮
        binding.buttonSend.setOnClickListener {
            if (isRecording) {
                val file = stopRecording()
                file?.let {
                    val duration = System.currentTimeMillis() - binding.textViewRecordingTime.tag as Long
                    onRecordingComplete(it, duration)
                }
            }
            dismiss()
        }
    }

    /**
     * 开始录音
     */
    fun startRecording() {
        isRecording = true
        binding.buttonSend.isEnabled = false
        binding.buttonCancel.text = "取消"

        // 启动录音
        voiceRecorder.startRecording()

        // 启动计时
        startTimer()

        // 启动波形动画
        binding.waveformView.startAnimation()

        // 监听录音状态
        collectRecordingState()
    }

    /**
     * 停止录音
     */
    fun stopRecording(): File? {
        isRecording = false
        binding.waveformView.stopAnimation()
        return voiceRecorder.stopRecording()
    }

    /**
     * 启动计时器
     */
    private fun startTimer() {
        val startTime = System.currentTimeMillis()
        binding.textViewRecordingTime.tag = startTime

        val timerThread = Thread {
            while (isRecording) {
                val elapsed = System.currentTimeMillis() - startTime
                val seconds = elapsed / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60

                handler.post {
                    binding.textViewRecordingTime.text =
                        "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
                }

                Thread.sleep(1000)

                // 超过60秒自动停止并发送
                if (seconds >= 60) {
                    handler.post {
                        val file = stopRecording()
                        file?.let {
                            val duration = System.currentTimeMillis() - binding.textViewRecordingTime.tag as Long
                            onRecordingComplete(it, duration)
                        }
                        dismiss()
                    }
                    break
                }
            }
        }
        timerThread.start()
    }

    /**
     * 收集录音状态
     */
    private fun collectRecordingState() {
        recordingScope.launch {
            voiceRecorder.recordingState.collect { state ->
                when (state) {
                    is VoiceRecorderHelper.RecordingState.Recording -> {
                        binding.textViewStatus.text = "正在录音..."
                        binding.buttonSend.isEnabled = true
                        binding.buttonCancel.text = "取消"
                    }
                    is VoiceRecorderHelper.RecordingState.TooShort -> {
                        binding.textViewStatus.text = "说话时间太短"
                        binding.buttonSend.isEnabled = false
                    }
                    is VoiceRecorderHelper.RecordingState.Error -> {
                        binding.textViewStatus.text = "录音失败"
                        binding.buttonSend.isEnabled = false
                    }
                    else -> {}
                }
            }
        }
    }

    override fun dismiss() {
        isRecording = false
        binding.waveformView.stopAnimation()
        recordingScope.cancel()
        super.dismiss()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Dialog 没有 lifecycle，暂时不处理
        // lifecycle.removeObserver(voiceRecorder)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                // 对话框显示
            }
            Lifecycle.Event.ON_PAUSE -> {
                // 对话框隐藏
                if (isShowing && isRecording) {
                    stopRecording()
                }
            }
            else -> {}
        }
    }
}
