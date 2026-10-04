package com.example.myapplication.activity

import android.app.AlertDialog
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.service.BackupService
import com.example.myapplication.service.UpdateService
import java.io.File
import kotlin.concurrent.thread

/**
 * 仓库页的「设置侧边栏」：左上角齿轮打开侧栏，侧栏里目前两行——
 * 数据备份 / 恢复、版本更新。
 *
 * 从 WareFragment 里拆出来，页面本身只管存量表格；备份恢复完需要重新查一遍存量，
 * 所以通过 [onInventoryRestored] 回调交给页面处理。
 *
 * 注意：两个 registerForActivityResult 必须在 Fragment 初始化阶段注册，
 * 所以这个对象要在 WareFragment 的字段里就 new 出来（不能等 onCreateView）。
 */
class WareDrawerPanel(
    private val fragment: Fragment,
    /** 导入备份成功后回调，让页面重新查一遍存量 */
    private val onInventoryRestored: () -> Unit
) {

    private var drawerLayout: DrawerLayout? = null

    /** 导出备份：让用户自己挑保存位置（系统文件选择器，不需要存储权限） */
    private val exportBackupLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            exportBackup(uri)
        }
    }

    /** 导入备份：让用户挑备份文件 */
    private val importBackupLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importBackup(uri)
        }
    }

    /** 把侧边栏接上：齿轮按钮 + 两行设置项 */
    fun attach(view: View) {
        val drawer = view.findViewById<DrawerLayout>(R.id.ware_drawer)
        drawerLayout = drawer

        view.findViewById<ImageButton>(R.id.button_settings).setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }
        view.findViewById<View>(R.id.row_backup).setOnClickListener {
            drawer.closeDrawer(GravityCompat.START)
            showBackupDialog()
        }
        view.findViewById<View>(R.id.row_update).setOnClickListener {
            drawer.closeDrawer(GravityCompat.START)
            showUpdateDialog()
        }
    }

    /** 数据备份 / 恢复：让用户选导出还是导入 */
    private fun showBackupDialog() {
        drawerLayout?.closeDrawer(GravityCompat.START)
        val items = arrayOf(
            fragment.getString(R.string.settings_backup_export),
            fragment.getString(R.string.settings_backup_import)
        )
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(R.string.settings_backup)
            .setItems(items) { _, which ->
                if (which == 0) {
                    exportBackupLauncher.launch(BackupService.suggestedFileName())
                } else {
                    confirmImport()
                }
            }
            .show()
    }

    /** 导入会把现在的数据整个覆盖掉，先确认一下 */
    private fun confirmImport() {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(R.string.settings_backup)
            .setMessage(R.string.backup_import_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                importBackupLauncher.launch(
                    arrayOf("application/zip", "application/octet-stream", "*/*")
                )
            }
            .show()
    }

    private fun exportBackup(uri: Uri) {
        val context = fragment.requireContext().applicationContext
        thread {
            val success = try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    BackupService(context).exportTo(output)
                } ?: false
            } catch (e: Exception) {
                false
            }
            fragment.activity?.runOnUiThread {
                if (!fragment.isAdded) {
                    return@runOnUiThread
                }
                val messageRes =
                    if (success) R.string.backup_export_success else R.string.backup_export_failed
                Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importBackup(uri: Uri) {
        val context = fragment.requireContext().applicationContext
        thread {
            val success = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BackupService(context).restoreFrom(input)
                } ?: false
            } catch (e: Exception) {
                false
            }
            fragment.activity?.runOnUiThread {
                if (!fragment.isAdded) {
                    return@runOnUiThread
                }
                val messageRes =
                    if (success) R.string.backup_import_success else R.string.backup_import_failed
                Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
                if (success) {
                    // 数据库连接已经在恢复时关掉了，让页面重新查一遍就是新数据
                    onInventoryRestored()
                }
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
    private fun showUpdateDialog() {
        val context = fragment.requireContext().applicationContext
        thread {
            var reason: String? = null
            val release = try {
                UpdateService.fetchLatestRelease()
            } catch (e: Exception) {
                // 失败原因要看得见：404 是"还没发版本"，UnknownHost 才是网络问题
                android.util.Log.w(UpdateService.LOG_TAG, "检查更新失败", e)
                reason = e.message ?: e.javaClass.simpleName
                null
            }
            fragment.activity?.runOnUiThread {
                if (!fragment.isAdded) {
                    return@runOnUiThread
                }
                if (release == null) {
                    val message = fragment.getString(
                        R.string.update_check_failed,
                        reason ?: fragment.getString(R.string.update_check_unknown_reason)
                    )
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val (currentCode, currentName) =
                    UpdateService.currentVersion(fragment.requireContext())
                if (UpdateService.isUpToDate(currentCode, currentName, release)) {
                    showLatestVersionDialog()
                } else {
                    showNewVersionDialog(release)
                }
            }
        }
    }

    /** 已经是最新版本：只有一个确认按钮 */
    private fun showLatestVersionDialog() {
        AlertDialog.Builder(fragment.requireContext())
            .setMessage(R.string.update_latest)
            .setPositiveButton(R.string.confirm, null)
            .show()
    }

    /** 有新版本：取消 / 下载 */
    private fun showNewVersionDialog(release: UpdateService.LatestRelease) {
        val message = if (release.versionName.isBlank()) {
            fragment.getString(R.string.update_has_new)
        } else {
            fragment.getString(R.string.update_has_new_version, release.versionName)
        }
        AlertDialog.Builder(fragment.requireContext())
            .setMessage(message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.update_download) { _, _ -> downloadAndInstall(release) }
            .show()
    }

    /** 下载 apk，下完交给系统安装 */
    private fun downloadAndInstall(release: UpdateService.LatestRelease) {
        val apkUrl = release.apkUrl
        if (apkUrl == null) {
            Toast.makeText(
                fragment.requireContext(), R.string.update_no_apk, Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 下载要一会儿，给个进度条，免得被当成卡死
        val progressBar =
            ProgressBar(fragment.requireContext(), null, android.R.attr.progressBarStyleHorizontal)
        progressBar.max = 100
        val content = LinearLayout(fragment.requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), 0)
            addView(
                progressBar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val dialog = AlertDialog.Builder(fragment.requireContext())
            .setTitle(R.string.update_downloading)
            .setView(content)
            .setCancelable(false)
            .create()
        dialog.show()

        val context = fragment.requireContext().applicationContext
        thread {
            val file = try {
                UpdateService.downloadApk(context, apkUrl, release.versionName) { percent ->
                    fragment.activity?.runOnUiThread { progressBar.progress = percent }
                }
            } catch (e: Exception) {
                android.util.Log.w(UpdateService.LOG_TAG, "下载新版本失败", e)
                null
            }

            fragment.activity?.runOnUiThread {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
                if (!fragment.isAdded) {
                    return@runOnUiThread
                }
                if (file == null) {
                    Toast.makeText(context, R.string.update_download_failed, Toast.LENGTH_SHORT)
                        .show()
                } else {
                    installApk(file)
                }
            }
        }
    }

    /** 弹出系统安装界面（apk 在应用私有目录里，要用 FileProvider 授权给安装器） */
    private fun installApk(file: File) {
        try {
            fragment.startActivity(
                UpdateService.installIntent(fragment.requireContext(), file)
            )
        } catch (e: Exception) {
            Toast.makeText(
                fragment.requireContext(), R.string.update_install_failed, Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun dp(value: Int): Int =
        (value * fragment.resources.displayMetrics.density).toInt()
}
