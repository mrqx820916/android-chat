package com.chat.lightweight.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 语音录制器助手
 * 支持按住录音、自动停止、时长限制
 */
class VoiceRecorderHelper(private val context: Context) : DefaultLifecycleObserver {

    companion object {
        private const val MAX_DURATION = 60 * 1000L // 60秒
        private const val MIN_DURATION = 1000L // 1秒
    }

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var startTime: Long = 0
    private var isRecording = false

    // 录音状态
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    // 音量等级 (0-1)
    private val _volumeLevel = MutableStateFlow(0f)
    val volumeLevel: StateFlow<Float> = _volumeLevel.asStateFlow()

    // 录音时长
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    /**
     * 录音状态
     */
    sealed class RecordingState {
        data object Idle : RecordingState()
        data object Recording : RecordingState()
        data class Completed(val file: File, val duration: Long) : RecordingState()
        data class Error(val message: String) : RecordingState()
        data object TooShort : RecordingState()
        data object TooLong : RecordingState()
    }

    /**
     * 开始录音
     */
    fun startRecording(): Boolean {
        if (isRecording) return false

        return try {
            // 创建录音文件
            audioFile = createAudioFile()

            // 初始化MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile?.absolutePath)

                prepare()
                start()
            }

            startTime = System.currentTimeMillis()
            isRecording = true
            _recordingState.value = RecordingState.Recording

            // 启动时长监控
            startDurationMonitor()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            _recordingState.value = RecordingState.Error(e.message ?: "录音失败")
            cleanup()
            false
        }
    }

    /**
     * 停止录音
     */
    fun stopRecording(): File? {
        if (!isRecording) return null

        val duration = System.currentTimeMillis() - startTime

        return try {
            mediaRecorder?.apply {
                stop()
                reset()
            }

            isRecording = false

            when {
                duration < MIN_DURATION -> {
                    _recordingState.value = RecordingState.TooShort
                    cleanup()
                    null
                }
                duration > MAX_DURATION -> {
                    _recordingState.value = RecordingState.TooLong
                    cleanup()
                    null
                }
                else -> {
                    val file = audioFile
                    _recordingState.value = RecordingState.Completed(
                        file = file ?: return null,
                        duration = duration
                    )
                    file
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _recordingState.value = RecordingState.Error(e.message ?: "停止录音失败")
            cleanup()
            null
        }
    }

    /**
     * 取消录音
     */
    fun cancelRecording() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                reset()
            }

            // 删除录音文件
            audioFile?.delete()
            audioFile = null

            isRecording = false
            _recordingState.value = RecordingState.Idle
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cleanup()
        }
    }

    /**
     * 启动时长监控
     */
    private fun startDurationMonitor() {
        val monitorThread = Thread {
            while (isRecording) {
                val currentDuration = System.currentTimeMillis() - startTime
                _duration.value = currentDuration

                // 更新音量等级（模拟）
                _volumeLevel.value = (Math.random() * 0.8 + 0.2).toFloat()

                if (currentDuration >= MAX_DURATION) {
                    // 超过最大时长，自动停止
                    stopRecording()
                    break
                }

                Thread.sleep(100)
            }
        }
        monitorThread.start()
    }

    /**
     * 创建录音文件
     */
    private fun createAudioFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "VOICE_$timeStamp.m4a"

        val audioDir = File(context.getExternalFilesDir(null), "voices")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        return File(audioDir, fileName)
    }

    /**
     * 获取录音时长文本
     */
    fun getDurationText(duration: Long): String {
        val seconds = (duration / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "${minutes}:${remainingSeconds.toString().padStart(2, '0')}"
    }

    /**
     * 清理资源
     */
    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        _volumeLevel.value = 0f
        _duration.value = 0L
    }

    /**
     * 生命周期回调
     */
    override fun onResume(owner: LifecycleOwner) {
        // Activity恢复时检查录音状态
    }

    override fun onPause(owner: LifecycleOwner) {
        // Activity暂停时停止录音
        if (isRecording) {
            cancelRecording()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cleanup()
    }
}
