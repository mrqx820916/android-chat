package com.chat.lightweight.media

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.chat.lightweight.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * 语音波形动画视图
 * 显示录音时的音频波形
 */
class VoiceWaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 波形条数
    private var barCount = 8

    // 波形颜色
    private var waveColor = ContextCompat.getColor(context, R.color.brand_primary)

    // 波形高度
    private var minHeight = 8.dpToPx()
    private var maxHeight = 48.dpToPx()

    // 波形宽度
    private var barWidth = 4.dpToPx()
    private var barSpacing = 4.dpToPx()

    // 波形高度数组
    private var barHeights = FloatArray(barCount) { minHeight }

    // 动画相关
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var animationJob: Job? = null

    // 绘制画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = waveColor
        style = Paint.Style.FILL
    }

    // 音量等级 (0-1)
    private var volumeLevel: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateWaveform()
        }

    init {
        // 初始化波形
        updateWaveform()
    }

    /**
     * 开始动画
     */
    fun startAnimation() {
        animationJob?.cancel()
        animationJob = scope.launch {
            while (true) {
                // 模拟音量变化
                volumeLevel = Random.nextFloat()
                delay(100)
            }
        }
    }

    /**
     * 停止动画
     */
    fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
        // 重置波形
        barHeights.fill(minHeight)
        invalidate()
    }

    /**
     * 更新波形
     */
    private fun updateWaveform() {
        val totalWidth = barCount * barWidth + (barCount - 1) * barSpacing
        val startX = (width - totalWidth) / 2f

        for (i in 0 until barCount) {
            // 使用正弦函数创建平滑波形
            val phase = (i.toFloat() / barCount) * 2 * Math.PI.toFloat()
            val amplitude = sin(phase) * 0.5f + 0.5f

            // 结合音量等级
            val targetHeight = minHeight + (maxHeight - minHeight) * amplitude * volumeLevel

            // 平滑过渡
            barHeights[i] = barHeights[i] + (targetHeight - barHeights[i]) * 0.3f
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalWidth = barCount * barWidth + (barCount - 1) * barSpacing
        val startX = (width - totalWidth) / 2f
        val centerY = height / 2f

        for (i in 0 until barCount) {
            val x = startX + i * (barWidth + barSpacing)
            val barHeight = barHeights[i]
            val y = centerY - barHeight / 2f

            // 绘制圆角矩形
            canvas.drawRoundRect(
                x,
                y,
                x + barWidth,
                y + barHeight,
                barWidth / 2f,
                barWidth / 2f,
                paint
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = barCount * barWidth + (barCount - 1) * barSpacing + paddingLeft + paddingRight
        val desiredHeight = maxHeight + paddingTop + paddingBottom

        setMeasuredDimension(
            resolveSize(desiredWidth.toInt(), widthMeasureSpec),
            resolveSize(desiredHeight.toInt(), heightMeasureSpec)
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    /**
     * dp转px
     */
    private fun Int.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }
}
