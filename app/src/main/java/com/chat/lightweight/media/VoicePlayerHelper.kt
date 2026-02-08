package com.chat.lightweight.media

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 语音播放器助手
 * 支持播放、暂停、进度显示
 */
class VoicePlayerHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null

    // 播放状态
    private val _playingState = MutableStateFlow<PlayingState>(PlayingState.Idle)
    val playingState: StateFlow<PlayingState> = _playingState.asStateFlow()

    // 播放进度
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    // 总时长
    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    /**
     * 播放状态
     */
    sealed class PlayingState {
        data object Idle : PlayingState()
        data object Loading : PlayingState()
        data object Playing : PlayingState()
        data object Paused : PlayingState()
        data class Error(val message: String) : PlayingState()
        data class Completed(val file: File) : PlayingState()
    }

    /**
     * 播放语音
     */
    fun play(file: File) {
        if (currentFile?.absolutePath == file.absolutePath && mediaPlayer?.isPlaying == true) {
            // 正在播放同一个文件，暂停
            pause()
            return
        }

        if (currentFile?.absolutePath == file.absolutePath && mediaPlayer != null) {
            // 同一个文件但未播放，继续播放
            resume()
            return
        }

        // 播放新文件
        currentFile = file
        _playingState.value = PlayingState.Loading

        try {
            releasePlayer()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepareAsync()

                setOnPreparedListener {
                    _duration.value = it.duration
                    start()
                    _playingState.value = PlayingState.Playing
                    startProgressUpdater()
                }

                setOnCompletionListener {
                    _playingState.value = PlayingState.Completed(file)
                    releasePlayer()
                }

                setOnErrorListener { _, what, extra ->
                    _playingState.value = PlayingState.Error("播放错误: $what, $extra")
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playingState.value = PlayingState.Error(e.message ?: "播放失败")
            releasePlayer()
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playingState.value = PlayingState.Paused
            }
        }
    }

    /**
     * 继续播放
     */
    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _playingState.value = PlayingState.Playing
                startProgressUpdater()
            }
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying || _playingState.value is PlayingState.Paused) {
                it.stop()
            }
        }
        _playingState.value = PlayingState.Idle
        _progress.value = 0
        releasePlayer()
    }

    /**
     * 跳转到指定位置
     */
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        _progress.value = position
    }

    /**
     * 获取当前播放进度百分比
     */
    fun getProgressPercentage(): Int {
        val total = _duration.value
        val current = _progress.value
        return if (total > 0) {
            (current * 100 / total)
        } else {
            0
        }
    }

    /**
     * 获取播放时长文本
     */
    fun getDurationText(): String {
        val seconds = _duration.value / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "${minutes}:${remainingSeconds.toString().padStart(2, '0')}"
    }

    /**
     * 获取当前进度文本
     */
    fun getProgressText(): String {
        val seconds = _progress.value / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "${minutes}:${remainingSeconds.toString().padStart(2, '0')}"
    }

    /**
     * 启动进度更新器
     */
    private fun startProgressUpdater() {
        val updaterThread = Thread {
            while (mediaPlayer?.isPlaying == true) {
                _progress.value = mediaPlayer?.currentPosition ?: 0
                Thread.sleep(100)
            }
        }
        updaterThread.start()
    }

    /**
     * 释放播放器
     */
    private fun releasePlayer() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        currentFile = null
    }

    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    /**
     * 获取当前文件
     */
    fun getCurrentFile(): File? {
        return currentFile
    }

    /**
     * 清理资源
     */
    fun release() {
        stop()
        releasePlayer()
    }
}
