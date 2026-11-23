package com.example.myapplication.activity

import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.service.BackupService
import com.example.myapplication.service.UpdateService
import com.example.myapplication.ui.components.AppDialogOption
import com.example.myapplication.ui.components.AppDialogSpec
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 备份 / 更新操作：对话框 UI 由 Compose 页面展示，
 * 这里负责备份 / 恢复、版本检查、下载和安装流程。
 *
 * 备份恢复完需要重新查一遍存量，所以通过 [onInventoryRestored] 回调交给 Activity 处理。
 */
class AppActionsController(
    private val activity: ComponentActivity,
    /** 导入备份成功后回调，让页面重新查一遍存量 */
    private val onInventoryRestored: () -> Unit
) {
    val dialogState = mutableStateOf<AppDialogSpec?>(null)

    fun dismissDialog() {
        dialogState.value = null
    }

    /** 导出备份：让用户自己挑保存位置（系统文件选择器，不需要存储权限） */
    private val exportBackupLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            exportBackup(uri)
        }
    }

    /** 导入备份：让用户挑备份文件 */
    private val importBackupLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importBackup(uri)
        }
    }

    /** 数据备份 / 恢复：让用户选导出还是导入 */
    fun showBackupDialog() {
        dialogState.value = AppDialogSpec(
            title = activity.getString(R.string.settings_backup),
            options = listOf(
                AppDialogOption(activity.getString(R.string.settings_backup_export)) {
                    exportBackupLauncher.launch(BackupService.suggestedFileName(activity))
                },
                AppDialogOption(activity.getString(R.string.settings_backup_import)) {
                    confirmImport()
                }
            )
        )
    }

    /** 导入会把现在的数据整个覆盖掉，先确认一下 */
    private fun confirmImport() {
        dialogState.value = AppDialogSpec(
            title = activity.getString(R.string.settings_backup),
            message = activity.getString(R.string.backup_import_confirm),
            dismissLabel = activity.getString(R.string.cancel),
            confirmLabel = activity.getString(R.string.confirm),
            onConfirm = {
                importBackupLauncher.launch(
                    arrayOf("application/zip", "application/octet-stream", "*/*")
                )
            }
        )
    }

    private fun exportBackup(uri: Uri) {
        val context = activity.applicationContext
        activity.lifecycleScope.launch {
            val success = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        BackupService(context).exportTo(output)
                    } ?: false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(LOG_TAG, "导出备份失败", e)
                false
            }
            if (activity.isFinishing || activity.isDestroyed) return@launch
            dialogState.value = AppDialogSpec(
                title = activity.getString(R.string.settings_backup),
                message = activity.getString(
                    if (success) R.string.backup_export_success else R.string.backup_export_failed
                ),
                confirmLabel = activity.getString(R.string.confirm)
            )
        }
    }

    private fun importBackup(uri: Uri) {
        val context = activity.applicationContext
        activity.lifecycleScope.launch {
            val success = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BackupService(context).restoreFrom(input)
                    } ?: false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(LOG_TAG, "导入备份失败", e)
                false
            }
            if (activity.isFinishing || activity.isDestroyed) return@launch
            dialogState.value = AppDialogSpec(
                title = activity.getString(R.string.settings_backup),
                message = activity.getString(
                    if (success) R.string.backup_import_success else R.string.backup_import_failed
                ),
                confirmLabel = activity.getString(R.string.confirm)
            )
            if (success) {
                onInventoryRestored()
            }
        }
    }

    /**
     * 检查更新。
     *
     * - 本机 versionCode >= 线上：提示"当前已经是最新版本"（只有一个确认按钮）
     * - 本机 < 线上：提示"是否下载最近版本"（取消 / 下载），下载完直接弹安装
     * - 查不到（没发 release、网络不通等）：提示里带上原因，具体细节看 log
     */
    fun showUpdateDialog() {
        activity.lifecycleScope.launch {
            var reason: String? = null
            val release = withContext(Dispatchers.IO) {
                try {
                    UpdateService.fetchLatestRelease()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(UpdateService.LOG_TAG, "检查更新失败", e)
                    reason = e.message ?: e.javaClass.simpleName
                    null
                }
            }
            if (activity.isFinishing || activity.isDestroyed) return@launch
            if (release == null) {
                val message = activity.getString(
                    R.string.update_check_failed,
                    reason ?: activity.getString(R.string.update_check_unknown_reason)
                )
                dialogState.value = AppDialogSpec(
                    title = activity.getString(R.string.settings_update),
                    message = message,
                    confirmLabel = activity.getString(R.string.confirm)
                )
                return@launch
            }
            val (currentCode, currentName) = UpdateService.currentVersion(activity)
            if (UpdateService.isUpToDate(currentCode, currentName, release)) {
                showLatestVersionDialog()
            } else {
                showNewVersionDialog(release)
            }
        }
    }

    /** 已经是最新版本：只有一个确认按钮 */
    private fun showLatestVersionDialog() {
        dialogState.value = AppDialogSpec(
            title = activity.getString(R.string.settings_update),
            message = activity.getString(R.string.update_latest),
            confirmLabel = activity.getString(R.string.confirm)
        )
    }

    /** 有新版本：取消 / 下载 */
    private fun showNewVersionDialog(release: UpdateService.LatestRelease) {
        val message = if (release.versionName.isBlank()) {
            activity.getString(R.string.update_has_new)
        } else {
            activity.getString(R.string.update_has_new_version, release.versionName)
        }
        dialogState.value = AppDialogSpec(
            title = activity.getString(R.string.settings_update),
            message = message,
            dismissLabel = activity.getString(R.string.cancel),
            confirmLabel = activity.getString(R.string.update_download),
            onConfirm = { downloadAndInstall(release) }
        )
    }

    /** 下载 apk，下完交给系统安装 */
    private fun downloadAndInstall(release: UpdateService.LatestRelease) {
        val apkUrl = release.apkUrl
        if (apkUrl == null) {
            dialogState.value = AppDialogSpec(
                title = activity.getString(R.string.settings_update),
                message = activity.getString(R.string.update_no_apk),
                confirmLabel = activity.getString(R.string.confirm)
            )
            return
        }

        dialogState.value = AppDialogSpec(
            title = activity.getString(R.string.update_downloading),
            progressMessage = activity.getString(
                R.string.update_download_progress_unknown_total,
                formatSize(0)
            ),
            dismissible = false
        )

        val context = activity.applicationContext
        activity.lifecycleScope.launch {
            val file = try {
                withContext(Dispatchers.IO) {
                    UpdateService.downloadApk(context, apkUrl, release.versionName) { downloaded, total ->
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) {
                                return@runOnUiThread
                            }
                            val progressMessage = if (total > 0) {
                                context.getString(
                                    R.string.update_download_progress,
                                    formatSize(downloaded), formatSize(total)
                                )
                            } else {
                                context.getString(
                                    R.string.update_download_progress_unknown_total,
                                    formatSize(downloaded)
                                )
                            }
                            dialogState.value = dialogState.value?.copy(
                                progress = if (total > 0) downloaded.toFloat() / total else null,
                                progressMessage = progressMessage
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(UpdateService.LOG_TAG, "下载新版本失败", e)
                null
            }

            dialogState.value = null
            if (activity.isFinishing || activity.isDestroyed) return@launch
            if (file == null) {
                dialogState.value = AppDialogSpec(
                    title = activity.getString(R.string.settings_update),
                    message = activity.getString(R.string.update_download_failed),
                    confirmLabel = activity.getString(R.string.confirm)
                )
            } else {
                installApk(file)
            }
        }
    }

    /** 弹出系统安装界面（apk 在应用私有目录里，要用 FileProvider 授权给安装器） */
    private fun installApk(file: File) {
        try {
            activity.startActivity(
                UpdateService.installIntent(activity, file)
            )
        } catch (e: Exception) {
            dialogState.value = AppDialogSpec(
                title = activity.getString(R.string.settings_update),
                message = activity.getString(R.string.update_install_failed),
                confirmLabel = activity.getString(R.string.confirm)
            )
        }
    }

    /** 字节数换成人看得懂的大小（B / KB / MB / GB），进度条下面那行用 */
    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
        return String.format(Locale.getDefault(), "%.1f GB", mb / 1024.0)
    }

    private companion object {
        const val LOG_TAG = "AppActionsController"
    }
}