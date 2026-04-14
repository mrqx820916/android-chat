package com.chat.lightweight.ui.menu

import android.view.MenuItem
import android.widget.PopupMenu
import com.chat.lightweight.R

/**
 * 消息菜单处理器
 * 负责消息长按菜单的实现
 * 遵循单一职责原则，仅负责菜单逻辑
 */
class MessageMenuHandler(
    private val onMessageDelete: (messageId: String) -> Unit,
    private val onMessageCopy: (content: String) -> Unit,
    private val onMessageRecall: (messageId: String) -> Unit,
    private val isAdmin: Boolean = false
) {

    /**
     * 创建消息选项菜单
     * @param anchor 锚点View（消息item的root view）
     * @param messageId 消息ID
     * @param content 消息内容（用于复制）
     */
    fun show(anchor: android.view.View, messageId: String, content: String?) {
        val popup = PopupMenu(anchor.context, anchor)
        popup.menuInflater.inflate(R.menu.menu_message, popup.menu)

        // 管理员显示删除菜单
        popup.menu.findItem(R.id.menu_delete)?.isVisible = isAdmin

        popup.setOnMenuItemClickListener { item ->
            handleMenuItemSelected(item, messageId, content)
        }
        popup.show()
    }

    /**
     * 处理菜单项点击
     */
    fun handleMenuItemSelected(item: MenuItem, messageId: String, content: String?): Boolean {
        return when (item.itemId) {
            R.id.menu_delete -> {
                if (isAdmin) {
                    onMessageDelete(messageId)
                    true
                } else {
                    false
                }
            }
            R.id.menu_copy -> {
                content?.let { onMessageCopy(it) }
                true
            }
            R.id.menu_recall -> {
                onMessageRecall(messageId)
                true
            }
            else -> false
        }
    }

    /**
     * 显示删除确认对话框
     */
    fun showDeleteConfirmDialog(
        activity: androidx.appcompat.app.AppCompatActivity,
        onConfirm: () -> Unit
    ) {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("删除消息")
            .setMessage("确定要删除这条消息吗？\n\n删除后双方都将无法查看此消息。")
            .setPositiveButton("删除") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
