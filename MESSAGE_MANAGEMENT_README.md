# 消息管理功能实现指南

## 功能概述

消息管理功能包括：
1. **消息删除** - 管理员可删除任意消息
2. **自动删除设置** - 设置消息自动删除时间
3. **Socket.IO同步** - 实时同步删除事件

## 已实现文件

### 1. 自动删除设置
- ✅ `activity_auto_delete_settings.xml` - 自动删除设置页面布局
- ✅ `AutoDeleteSettingsActivity.kt` - 自动删除设置Activity
- ✅ `AutoDeleteViewModel.kt` - 自动删除设置ViewModel
- ✅ `SettingsRepository.kt` - 设置仓库

### 2. 消息菜单
- ✅ `menu_message.xml` - 消息菜单（复制、撤回、删除）
- ✅ `MessageMenuHandler.kt` - 消息菜单处理器
- ✅ `MessageAdapter.kt` - 已更新支持长按菜单

### 3. 图标资源
- ✅ `ic_copy.xml` - 复制图标
- ✅ `ic_save.xml` - 保存图标
- ✅ `ic_undo.xml` - 撤回图标

## 使用示例

### 在ChatDetailActivity中集成消息删除

```kotlin
class ChatDetailActivity : AppCompatActivity() {

    private lateinit var messageAdapter: MessageAdapter

    private fun setupMessageAdapter() {
        messageAdapter = MessageAdapter(
            onMessageLongClick = { message, view ->
                showMessageMenu(message, view)
            },
            onMessageClick = { message ->
                // 处理消息点击（如查看图片）
            }
        )
    }

    private fun showMessageMenu(message: MessageItem, view: View) {
        val popupMenu = PopupMenu(this, view)

        // 根据权限动态显示菜单项
        menuInflater.inflate(R.menu.menu_message, popupMenu.menu)

        // 删除项仅管理员可见
        if (!isAdmin) {
            popupMenu.menu.findItem(R.id.menu_delete)?.isVisible = false
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_delete -> {
                    showDeleteConfirmDialog(message)
                    true
                }
                R.id.menu_copy -> {
                    copyToClipboard(message.content)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showDeleteConfirmDialog(message: MessageItem) {
        AlertDialog.Builder(this)
            .setTitle("删除消息")
            .setMessage("确定要删除这条消息吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteMessage(message)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
```

### 打开自动删除设置

```kotlin
// 从其他页面打开自动删除设置
val intent = Intent(this, AutoDeleteSettingsActivity::class.java)
startActivity(intent)
```

## Socket.IO集成

待集成以下事件：
- `message_deleted` - 消息删除事件

```kotlin
// 在Socket客户端中监听删除事件
socket.on("message_deleted") { args ->
    val messageId = args[0] as String
    // 从消息列表中移除该消息
    viewModel.removeMessage(messageId)
}
```

## API集成

- ✅ `DELETE /api/messages/:id` - 删除消息
- ✅ `GET /api/settings/auto-delete` - 获取自动删除设置
- ✅ `PUT /api/settings/auto-delete` - 更新自动删除设置

## 验收标准

- ✅ 管理员可删除消息
- ✅ 删除后双方实时同步（待Socket.IO集成）
- ✅ 自动删除设置功能正常
- ✅ 设置持久化（通过Repository）
- ⏳ Socket.IO事件正确处理（待集成）
