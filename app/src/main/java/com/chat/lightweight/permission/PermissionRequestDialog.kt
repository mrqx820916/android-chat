package com.chat.lightweight.permission

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import androidx.core.content.ContextCompat

/**
 * 权限请求对话框 - 提供友好的权限说明和引导
 */
object PermissionRequestDialog {

    /**
     * 显示P0权限被拒绝的对话框
     * P0权限是必需权限,拒绝后无法正常使用应用
     */
    fun showP0PermissionDeniedDialog(activity: Activity) {
        val deniedPermissions = PermissionsManager.getInstance().getDeniedP0Permissions(activity)

        if (deniedPermissions.isEmpty()) return

        val message = buildP0DeniedMessage(deniedPermissions)

        AlertDialog.Builder(activity)
            .setTitle("需要必需权限")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("去设置") { _, _ ->
                openAppSettings(activity)
                activity.finish()
            }
            .setNegativeButton("退出") { _, _ ->
                activity.finish()
            }
            .create()
            .apply {
                show()
                // 使链接可点击
                findViewById<android.widget.TextView>(android.R.id.message)?.movementMethod =
                    LinkMovementMethod.getInstance()
            }
    }

    /**
     * 构建P0权限被拒绝的消息
     */
    private fun buildP0DeniedMessage(deniedPermissions: List<String>): SpannableString {
        val message = StringBuilder()
        message.append("轻聊需要以下权限才能正常使用:\n\n")

        deniedPermissions.forEach { permission ->
            val name = PermissionsManager.getInstance().getPermissionFriendlyName(permission)
            val desc = PermissionsManager.getInstance().getPermissionDescription(permission)
            message.append("• $name: $desc\n")
        }

        message.append("\n请在设置中开启这些权限。")

        val spannableString = SpannableString(message.toString())
        Linkify.addLinks(spannableString, Linkify.ALL)
        return spannableString
    }

    /**
     * 显示权限说明对话框
     * 用于P1权限被拒绝时,向用户说明为什么需要该权限
     */
    fun showPermissionRationaleDialog(
        activity: Activity,
        permissionName: String,
        message: String
    ) {
        AlertDialog.Builder(activity)
            .setTitle("需要${permissionName}")
            .setMessage(message)
            .setPositiveButton("重试") { _, _ ->
                // 重新请求权限的逻辑由调用者处理
                activity.recreate()
            }
            .setNegativeButton("取消", null)
            .create()
            .show()
    }

    /**
     * 显示相机权限说明对话框
     */
    fun showCameraPermissionRationale(activity: Activity) {
        showPermissionRationaleDialog(
            activity,
            "相机权限",
            "轻聊需要访问您的相机才能拍照并发送照片。请在设置中允许相机权限。"
        )
    }

    /**
     * 显示录音权限说明对话框
     */
    fun showRecordAudioPermissionRationale(activity: Activity) {
        showPermissionRationaleDialog(
            activity,
            "录音权限",
            "轻聊需要访问您的麦克风才能录制并发送语音消息。请在设置中允许录音权限。"
        )
    }

    /**
     * 显示存储权限说明对话框
     */
    fun showStoragePermissionRationale(activity: Activity) {
        val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "轻聊需要访问您的照片才能选择并发送图片。请在设置中允许照片和媒体权限。"
        } else {
            "轻聊需要访问您的存储空间才能选择并发送图片。请在设置中允许存储权限。"
        }

        showPermissionRationaleDialog(activity, "存储权限", message)
    }

    /**
     * 显示通知权限说明对话框
     */
    fun showNotificationPermissionRationale(activity: Activity) {
        showPermissionRationaleDialog(
            activity,
            "通知权限",
            "轻聊需要发送通知权限才能在收到新消息时提醒您。开启通知后,您不会错过任何重要消息。"
        )
    }

    /**
     * 打开应用设置页面
     * 允许用户手动授予权限
     */
    fun openAppSettings(activity: Activity) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", activity.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            // 如果打开详情页失败,尝试打开设置主页
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    /**
     * 显示首次权限请求说明对话框
     * 在第一次请求权限之前调用,向用户说明为什么需要权限
     */
    fun showFirstTimePermissionRequest(
        activity: Activity,
        permissions: Array<String>,
        onConfirmed: () -> Unit
    ) {
        if (permissions.isEmpty()) {
            onConfirmed()
            return
        }

        val title = "轻聊需要一些权限"
        val message = buildFirstTimePermissionMessage(permissions)

        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("好的") { _, _ ->
                onConfirmed()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    /**
     * 构建首次权限请求的消息
     */
    private fun buildFirstTimePermissionMessage(permissions: Array<String>): String {
        val message = StringBuilder()
        message.append("为了提供完整的功能体验,轻聊需要以下权限:\n\n")

        permissions.forEach { permission ->
            val name = PermissionsManager.getInstance().getPermissionFriendlyName(permission)
            val desc = PermissionsManager.getInstance().getPermissionDescription(permission)
            message.append("• $name\n  $desc\n\n")
        }

        message.append("这些权限仅用于所述用途,我们不会在未经您同意的情况下使用。")

        return message.toString()
    }

    /**
     * 显示权限被永久拒绝的对话框
     * 当用户勾选"不再询问"后,引导用户去设置页面开启
     */
    fun showPermissionPermanentlyDeniedDialog(
        activity: Activity,
        permission: String
    ) {
        val permissionName = PermissionsManager.getInstance().getPermissionFriendlyName(permission)

        AlertDialog.Builder(activity)
            .setTitle("$permissionName 被拒绝")
            .setMessage("您之前拒绝了$permissionName,并且选择了\"不再询问\"。\n\n" +
                       "您可以在系统设置中手动开启此权限。")
            .setPositiveButton("去设置") { _, _ ->
                openAppSettings(activity)
            }
            .setNegativeButton("取消", null)
            .create()
            .show()
    }

    /**
     * 显示所有必需权限的总结对话框
     * 在应用首次启动时显示
     */
    fun showPermissionsSummaryDialog(activity: Activity, onRequest: () -> Unit) {
        val message = """
            轻聊需要以下权限才能为您提供完整的聊天体验:

            • 通知权限 - 接收新消息提醒
            • 录音权限 - 发送语音消息
            • 存储权限 - 选择和发送图片
            • 相机权限 - 拍照发送

            这些权限仅在您使用相应功能时才会被调用。

            是否现在授予权限?
        """.trimIndent()

        AlertDialog.Builder(activity)
            .setTitle("权限说明")
            .setMessage(message)
            .setPositiveButton("授予权限") { _, _ ->
                onRequest()
            }
            .setNegativeButton("稍后") { _, _ ->
                // 稍后会再次请求
            }
            .setCancelable(false)
            .create()
            .show()
    }

    /**
     * 显示权限请求失败后的降级方案对话框
     */
    fun showPermissionFallbackDialog(
        activity: Activity,
        permissionName: String,
        fallbackMessage: String
    ) {
        AlertDialog.Builder(activity)
            .setTitle("无法使用此功能")
            .setMessage("由于未授予$permissionName,$fallbackMessage")
            .setPositiveButton("我知道了", null)
            .setNeutralButton("去设置") { _, _ ->
                openAppSettings(activity)
            }
            .create()
            .show()
    }

    /**
     * 检查并显示权限设置引导
     * 用于在用户使用某个功能时检查权限
     */
    fun checkAndShowPermissionGuide(
        activity: Activity,
        permission: String,
        onGranted: () -> Unit
    ) {
        val manager = PermissionsManager.getInstance()

        when {
            manager.checkPermission(activity, permission) -> {
                onGranted()
            }
            manager.isPermissionPermanentlyDenied(activity, permission) -> {
                showPermissionPermanentlyDeniedDialog(activity, permission)
            }
            else -> {
                // 显示权限说明,实际请求由调用者处理
                showPermissionRationaleDialog(
                    activity,
                    manager.getPermissionFriendlyName(permission),
                    manager.getPermissionDescription(permission)
                )
            }
        }
    }

    /**
     * 创建自定义样式的权限对话框
     * 用于需要更美观UI的场景
     */
    fun createStyledPermissionDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveText: String = "允许",
        negativeText: String? = "拒绝",
        onPositive: () -> Unit,
        onNegative: (() -> Unit)? = null
    ): AlertDialog {
        val builder = AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->
                onPositive()
            }

        if (negativeText != null) {
            builder.setNegativeButton(negativeText) { _, _ ->
                onNegative?.invoke()
            }
        }

        return builder.create()
    }
}
