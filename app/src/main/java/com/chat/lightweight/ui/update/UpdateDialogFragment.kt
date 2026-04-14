package com.chat.lightweight.ui.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.chat.lightweight.update.AppUpdateManifest
import com.chat.lightweight.update.UpdateDownloadResult
import com.chat.lightweight.update.UpdateManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.chat.lightweight.R
import kotlinx.coroutines.launch
import timber.log.Timber

class UpdateDialogFragment : BottomSheetDialogFragment() {

    private var manifest: AppUpdateManifest? = null

    private lateinit var titleText: TextView
    private lateinit var versionText: TextView
    private lateinit var notesText: TextView
    private lateinit var sizeText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var updateButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleText = view.findViewById(R.id.updateTitle)
        versionText = view.findViewById(R.id.updateVersion)
        notesText = view.findViewById(R.id.updateNotes)
        sizeText = view.findViewById(R.id.updateSize)
        progressBar = view.findViewById(R.id.updateProgress)
        progressText = view.findViewById(R.id.updateProgressText)
        updateButton = view.findViewById(R.id.updateButton)
        cancelButton = view.findViewById(R.id.updateCancelButton)

        manifest = arguments?.let { args ->
            val channel = args.getString("channel") ?: return@let null
            val packageName = args.getString("packageName") ?: return@let null
            val versionCode = args.getInt("versionCode")
            val versionName = args.getString("versionName") ?: return@let null
            val apkUrl = args.getString("apkUrl") ?: return@let null
            val sha256 = args.getString("sha256") ?: return@let null
            val sizeBytes = args.getLong("sizeBytes")
            val releasedAt = args.getString("releasedAt") ?: ""
            val releaseNotes = args.getString("releaseNotes") ?: ""
            val force = args.getBoolean("force")
            AppUpdateManifest(
                channel = channel,
                packageName = packageName,
                versionCode = versionCode,
                versionName = versionName,
                apkUrl = apkUrl,
                sha256 = sha256,
                sizeBytes = sizeBytes,
                releasedAt = releasedAt,
                releaseNotes = releaseNotes,
                force = force
            )
        }

        val m = manifest ?: run {
            dismiss()
            return
        }

        titleText.text = "发现新版本"
        versionText.text = "v${m.versionName}"
        notesText.text = if (m.releaseNotes.isNotEmpty()) m.releaseNotes else "优化体验，修复已知问题"
        sizeText.text = "安装包大小: ${formatSize(m.sizeBytes)}"

        if (m.force) {
            cancelButton.visibility = View.GONE
            isCancelable = false
        }

        updateButton.setOnClickListener { startDownload(m) }
        cancelButton.setOnClickListener { dismiss() }
    }

    private fun startDownload(m: AppUpdateManifest) {
        updateButton.isEnabled = false
        updateButton.text = "下载中..."
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        cancelButton.isEnabled = false

        lifecycleScope.launch {
            try {
                when (val result = UpdateManager.downloadUpdate(requireContext(), m)) {
                    is UpdateDownloadResult.Success -> {
                        progressBar.visibility = View.GONE
                        progressText.text = "下载完成，正在安装..."
                        UpdateManager.clearFailure(requireContext())
                        val installResult = UpdateManager.installUpdate(requireContext(), result.file)
                        if (!installResult.success) {
                            progressText.text = "安装失败: ${installResult.detail}"
                            updateButton.isEnabled = true
                            updateButton.text = "重试"
                            cancelButton.isEnabled = true
                        }
                    }
                    is UpdateDownloadResult.Failure -> {
                        progressBar.visibility = View.GONE
                        progressText.text = "下载失败: ${result.message}"
                        updateButton.isEnabled = true
                        updateButton.text = "重试"
                        cancelButton.isEnabled = !m.force
                        UpdateManager.recordFailure(
                            requireContext(),
                            m.versionCode,
                            result.message
                        )
                        Timber.e("更新下载失败: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                progressText.text = "发生错误: ${e.message}"
                updateButton.isEnabled = true
                updateButton.text = "重试"
                cancelButton.isEnabled = !m.force
                Timber.e(e, "更新过程异常")
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun showAllowingStateLoss(manager: FragmentManager, tag: String) {
        manager.beginTransaction()
            .add(this, tag)
            .commitAllowingStateLoss()
    }

    companion object {
        fun newInstance(manifest: AppUpdateManifest): UpdateDialogFragment {
            return UpdateDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("channel", manifest.channel)
                    putString("packageName", manifest.packageName)
                    putInt("versionCode", manifest.versionCode)
                    putString("versionName", manifest.versionName)
                    putString("apkUrl", manifest.apkUrl)
                    putString("sha256", manifest.sha256)
                    putLong("sizeBytes", manifest.sizeBytes)
                    putString("releasedAt", manifest.releasedAt)
                    putString("releaseNotes", manifest.releaseNotes)
                    putBoolean("force", manifest.force)
                }
            }
        }
    }
}
