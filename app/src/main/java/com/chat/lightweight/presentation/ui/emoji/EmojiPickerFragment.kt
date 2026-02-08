package com.chat.lightweight.presentation.ui.emoji

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chat.lightweight.data.cache.EmojiCacheManager
import com.chat.lightweight.data.model.Emoji
import com.chat.lightweight.data.model.EmojiCategory
import com.chat.lightweight.data.model.EmojiData
import com.chat.lightweight.databinding.FragmentEmojiPickerBinding
import kotlinx.coroutines.launch

/**
 * 表情选择器Fragment
 *
 * 提供表情选择功能，支持分类、搜索和最近使用
 */
class EmojiPickerFragment : Fragment() {

    private var _binding: FragmentEmojiPickerBinding? = null
    private val binding get() = _binding!!

    private lateinit var emojiCacheManager: EmojiCacheManager
    private lateinit var emojiAdapter: com.chat.lightweight.presentation.adapter.EmojiAdapter
    private lateinit var categoryAdapter: com.chat.lightweight.presentation.adapter.EmojiCategoryAdapter

    private var currentCategory: EmojiCategory = EmojiCategory.SMILEYS
    private var onEmojiSelectedListener: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emojiCacheManager = EmojiCacheManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmojiPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeRecentEmojis()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 设置表情选择监听器
     */
    fun setOnEmojiSelectedListener(listener: (String) -> Unit) {
        onEmojiSelectedListener = listener
    }

    private fun setupViews() {
        // 设置表情网格
        emojiAdapter = com.chat.lightweight.presentation.adapter.EmojiAdapter { emoji ->
            onEmojiSelected(emoji)
        }

        binding.rvEmojis.apply {
            layoutManager = GridLayoutManager(requireContext(), 8)
            adapter = emojiAdapter
            itemAnimator = null
        }

        // 设置分类Tab
        categoryAdapter = com.chat.lightweight.presentation.adapter.EmojiCategoryAdapter { category ->
            selectCategory(category)
        }

        binding.rvCategories.apply {
            layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(
                    requireContext(),
                    androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                    false
                )
            adapter = categoryAdapter
        }

        // 加载所有分类
        val categories = listOf(EmojiCategory.RECENT) + EmojiCategory.getAllCategories()
        categoryAdapter.submitList(categories)

        // 搜索功能
        binding.etSearch.addTextChangedListener(object :
            android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchEmojis(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // 清除按钮
        binding.btnClear.setOnClickListener {
            binding.etSearch.text?.clear()
        }

        // 删除按钮（仅最近使用分类显示）
        binding.btnDelete.setOnClickListener {
            lifecycleScope.launch {
                emojiCacheManager.clearRecentEmojis()
                selectCategory(EmojiCategory.RECENT)
            }
        }

        // 默认选择第一个分类
        selectCategory(EmojiCategory.RECENT)
    }

    /**
     * 观察最近使用的表情
     */
    private fun observeRecentEmojis() {
        lifecycleScope.launch {
            emojiCacheManager.getRecentEmojisFlow().collect { recentEmojis ->
                // 如果当前是最近使用分类，刷新显示
                if (currentCategory == EmojiCategory.RECENT) {
                    emojiAdapter.submitList(recentEmojis)
                    updateDeleteButtonVisibility(recentEmojis.isNotEmpty())
                }
            }
        }
    }

    /**
     * 选择分类
     */
    private fun selectCategory(category: EmojiCategory) {
        currentCategory = category
        categoryAdapter.setSelectedCategory(category)

        // 加载表情数据
        lifecycleScope.launch {
            val emojis = if (category == EmojiCategory.RECENT) {
                emojiCacheManager.getRecentEmojis()
            } else {
                EmojiData.getEmojisByCategory(category)
            }

            emojiAdapter.submitList(emojis)
            updateDeleteButtonVisibility(category == EmojiCategory.RECENT && emojis.isNotEmpty())
        }
    }

    /**
     * 搜索表情
     */
    private fun searchEmojis(query: String) {
        if (query.isBlank()) {
            // 恢复当前分类
            selectCategory(currentCategory)
            return
        }

        // 搜索并显示结果
        val results = EmojiData.searchEmojis(query)
        emojiAdapter.submitList(results)

        // 取消分类选择
        categoryAdapter.setSelectedCategory(null)

        // 隐藏删除按钮
        updateDeleteButtonVisibility(false)
    }

    /**
     * 表情被选中
     */
    private fun onEmojiSelected(emoji: Emoji) {
        // 添加到最近使用
        lifecycleScope.launch {
            emojiCacheManager.addRecentEmoji(emoji)
        }

        // 通知监听器
        onEmojiSelectedListener?.invoke(emoji.emoji)
    }

    /**
     * 更新删除按钮可见性
     */
    private fun updateDeleteButtonVisibility(visible: Boolean) {
        binding.btnDelete.visibility = if (visible) View.VISIBLE else View.GONE
    }

    companion object {
        /**
         * 创建实例
         */
        fun newInstance() = EmojiPickerFragment()

        /**
         * 创建实例并设置监听器
         */
        fun newInstance(listener: (String) -> Unit): EmojiPickerFragment {
            return EmojiPickerFragment().apply {
                setOnEmojiSelectedListener(listener)
            }
        }
    }
}
