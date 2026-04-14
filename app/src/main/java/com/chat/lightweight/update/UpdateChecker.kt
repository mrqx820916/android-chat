package com.chat.lightweight.update

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

internal class UpdateChecker(
    private val manifestUrl: String,
    private val channel: String,
    private val store: UpdateFailureStore,
    private val installedApkSha256Provider: () -> String,
    private val transport: UpdateHttpTransport = UrlConnectionUpdateTransport(),
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val gson = Gson()

    suspend fun checkForUpdate(
        currentVersionCode: Int,
        packageName: String
    ): UpdateCheckResult {
        val body = try {
            transport.fetchString(manifestUrl, UpdateConfig.manifestTimeoutMs)
        } catch (throwable: Throwable) {
            return UpdateCheckResult.Failure(
                message = throwable.message ?: "拉取更新清单失败",
                cause = throwable
            )
        }

        val manifest = try {
            parseManifest(body)
        } catch (throwable: Throwable) {
            return UpdateCheckResult.Failure(
                message = throwable.message ?: "解析更新清单失败",
                cause = throwable
            )
        }

        if (manifest.channel != channel) {
            return UpdateCheckResult.Failure("更新清单 channel 不匹配: ${manifest.channel}")
        }
        if (manifest.packageName != packageName) {
            return UpdateCheckResult.Failure("更新清单包名不匹配: ${manifest.packageName}")
        }
        if (manifest.versionCode < currentVersionCode) {
            return UpdateCheckResult.NoUpdate("当前已是最新版本")
        }

        if (manifest.versionCode == currentVersionCode) {
            val installedSha256 = try {
                installedApkSha256Provider().trim().lowercase()
            } catch (throwable: Throwable) {
                return UpdateCheckResult.Failure(
                    message = throwable.message ?: "读取当前安装包校验值失败",
                    cause = throwable
                )
            }
            if (installedSha256 == manifest.sha256) {
                return UpdateCheckResult.NoUpdate("当前已是最新版本")
            }
            return UpdateCheckResult.UpdateAvailable(
                manifest = manifest,
                sameVersionHotfix = true
            )
        }

        // versionCode > currentVersionCode
        val failureRecord = store.getFailureRecord(channel)
        if (failureRecord != null && failureRecord.versionCode != manifest.versionCode) {
            store.clearFailure(channel)
        }
        if (store.shouldSkipVersion(channel, manifest.versionCode, nowProvider())) {
            return UpdateCheckResult.Skipped(
                manifest = manifest,
                reason = "版本 ${manifest.versionCode} 在冷却期内，暂不重复更新"
            )
        }

        return UpdateCheckResult.UpdateAvailable(manifest)
    }

    internal fun parseManifest(body: String): AppUpdateManifest {
        val manifest = try {
            gson.fromJson(body, AppUpdateManifest::class.java)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("更新清单 JSON 格式错误: ${e.message}")
        }
        requireNotNull(manifest) { "更新清单解析结果为空" }
        require(manifest.channel.isNotEmpty()) { "更新清单缺少 channel" }
        require(manifest.packageName.isNotEmpty()) { "更新清单缺少 packageName" }
        require(manifest.versionCode > 0) { "更新清单缺少 versionCode" }
        require(manifest.versionName.isNotEmpty()) { "更新清单缺少 versionName" }
        require(manifest.apkUrl.isNotEmpty()) { "更新清单缺少 apkUrl" }
        require(manifest.sha256.matches(Regex("[0-9a-f]{64}"))) { "更新清单缺少合法 sha256" }
        require(manifest.sizeBytes > 0) { "更新清单缺少 sizeBytes" }
        return manifest
    }
}
