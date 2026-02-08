package com.chat.lightweight.ui.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chat.lightweight.R
import com.chat.lightweight.databinding.ActivityAutoDeleteSettingsBinding
import com.chat.lightweight.presentation.viewmodel.ViewModelFactory
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

/**
 * 自动删除设置Activity
 * 管理员专用：设置消息自动删除时间
 */
class AutoDeleteSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAutoDeleteSettingsBinding
    private val viewModel: AutoDeleteViewModel by viewModels {
        ViewModelFactory.getInstance(application)
    }

    // 时间单位配置
    private val unitConfigs = mapOf(
        "minutes" to Pair(1, 59),
        "hours" to Pair(1, 23),
        "days" to Pair(1, 30),
        "months" to Pair(1, 12),
        "years" to Pair(1, 10)
    )

    private var currentUnit = "days"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutoDeleteSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 初始化UI
        setupUI()

        // 观察数据
        observeData()

        // 加载设置
        viewModel.loadSettings()
    }

    /**
     * 初始化UI
     */
    private fun setupUI() {
        // 启用开关
        binding.enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 启用时设置默认值
                binding.timeSettingsCard.visibility = View.VISIBLE
                binding.disabledHintCard.visibility = View.GONE
                updateUnit("days")
                binding.valueEditText.setText("1")
            } else {
                // 禁用时隐藏时间设置
                binding.timeSettingsCard.visibility = View.GONE
                binding.disabledHintCard.visibility = View.VISIBLE
            }
        }

        // 时间单位选择
        binding.unitChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val unit = when {
                checkedIds.contains(R.id.chipMinutes) -> "minutes"
                checkedIds.contains(R.id.chipHours) -> "hours"
                checkedIds.contains(R.id.chipDays) -> "days"
                checkedIds.contains(R.id.chipMonths) -> "months"
                checkedIds.contains(R.id.chipYears) -> "years"
                else -> "days"
            }
            updateUnit(unit)
        }

        // 保存按钮（通过Toolbar菜单）
    }

    /**
     * 更新时间单位
     */
    private fun updateUnit(unit: String) {
        currentUnit = unit
        val (min, max) = unitConfigs[unit] ?: Pair(1, 30)
        binding.rangeHintTextView.text = "范围: $min-$max"
        binding.descriptionTextView.text = "设置后，新发送的消息将在指定时间后自动删除。已有消息不受影响。"
    }

    /**
     * 观察数据
     */
    private fun observeData() {
        // 观察设置状态
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                renderUiState(state)
            }
        }
    }

    /**
     * 渲染UI状态
     */
    private fun renderUiState(state: AutoDeleteUiState) {
        // 显示/隐藏加载进度
        binding.progressIndicator.visibility = if (state.isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // 启用开关状态
        binding.enabledSwitch.isChecked = state.enabled

        // 显示时间设置卡片
        if (state.enabled && state.unit != "permanent") {
            binding.timeSettingsCard.visibility = View.VISIBLE
            binding.disabledHintCard.visibility = View.GONE

            // 设置时间单位
            val chipId = when (state.unit) {
                "minutes" -> R.id.chipMinutes
                "hours" -> R.id.chipHours
                "days" -> R.id.chipDays
                "months" -> R.id.chipMonths
                "years" -> R.id.chipYears
                else -> R.id.chipDays
            }
            binding.unitChipGroup.check(chipId)

            // 设置数值
            binding.valueEditText.setText(state.value.toString())

        } else {
            binding.timeSettingsCard.visibility = View.GONE
            binding.disabledHintCard.visibility = View.VISIBLE
        }

        // 显示错误
        state.error?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }

        // 保存成功
        if (state.saveSuccess) {
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 保存设置
     */
    private fun saveSettings() {
        val enabled = binding.enabledSwitch.isChecked
        val unit = if (enabled) currentUnit else "permanent"
        val value = binding.valueEditText.text?.toString()?.toIntOrNull() ?: 0

        viewModel.saveSettings(enabled, unit, value)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
