package com.example.myapplication.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.LruCache
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.model.OrderType
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.service.BackupService
import com.example.myapplication.service.InventoryDatabase
import com.example.myapplication.utils.DrawableUtils
import com.example.myapplication.utils.ImageUtils
import com.example.myapplication.utils.SizeUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import org.json.JSONObject

/**
 * 仓库页面：按货号分组的存量表。
 *
 * 一个货号一张表：
 * - 第一排：货号（左，占满剩余宽度）| 经销商 | 数量 |
 * - 第二排开始每个颜色一行：图片（占位）| 颜色 | 34 35 …… 44 码（可以横向滑动）| 小计 | ＋ －
 *
 * 左栏（图片 + 颜色）和右栏（小计 + ＋ －）固定不动，只有中间的尺码是横向可滑动的，
 * 这样一屏里总能看清是哪个货号、哪个颜色。
 * 行尾的 ＋ / － 会打开 [OrderEditActivity]，货号、颜色、经销商都是锁定的（灰化显示），只能填数量。
 */
class WareFragment : Fragment() {

    private lateinit var tableContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var searchInput: EditText

    /** 数据库里查出来的全部存量（搜索过滤前的原始数据） */
    private var allInventories: List<ShoeInventory> = emptyList()

    /** 屏幕上这张表画的是哪批行；和刚算出来的一样就说明不用重画（初值 null，保证第一次一定画） */
    private var renderedInventories: List<ShoeInventory>? = null

    /** 画这张表时用的表格宽度；宽度变了（转屏 / 分屏）才需要按新宽度重画一次 */
    private var renderedWidth = 0

    /** 导出备份：让用户自己挑保存位置（系统文件选择器，不需要存储权限） */
    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            exportBackup(uri)
        }
    }

    /** 导入备份：让用户挑备份文件 */
    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importBackup(uri)
        }
    }

    /** 存量缩略图缓存：路径 -> Bitmap，避免每次回到页面都重新解码 */
    private val pictureCache = LruCache<String, Bitmap>(64)

    private val pictureCellWidth by lazy { dp(44) }
    private val colorCellWidth by lazy { dp(60) }
    private val leftPaneWidth by lazy { pictureCellWidth + colorCellWidth }
    private val sizeCellWidth by lazy { dp(44) }
    private val subtotalCellWidth by lazy { dp(52) }
    private val stockButtonWidth by lazy { dp(32) }
    private val rightPaneWidth by lazy { subtotalCellWidth + stockButtonWidth * 2 }
    private val rowHeight by lazy { dp(44) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ware, container, false)
        tableContainer = view.findViewById(R.id.inventory_table)
        emptyText = view.findViewById(R.id.inventory_empty)

        // 整单入库 / 出库：货号自己填，用来开一个新的货号
        view.findViewById<Button>(R.id.purchase_button).setOnClickListener {
            startActivity(OrderEditActivity.createIntent(requireContext(), OrderType.ARTICLE_PURCHASE))
        }
        view.findViewById<Button>(R.id.selling_button).setOnClickListener {
            startActivity(
                OrderEditActivity.createIntent(requireContext(), OrderType.ARTICLE_SOLD_CASH)
            )
        }

        drawerLayout = view.findViewById(R.id.ware_drawer)
        // 搜索框：边打字边过滤（货号 / 经销商），删掉关键字马上恢复全部
        searchInput = view.findViewById(R.id.search_inventory)
        searchInput.doAfterTextChanged { searchInventory() }

        // 设置侧边栏
        view.findViewById<ImageButton>(R.id.button_settings).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        view.findViewById<View>(R.id.row_backup).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showBackupDialog()
        }
        view.findViewById<View>(R.id.row_update).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showUpdateDialog()
        }

        return view
    }

    /**
     * 检查更新
     */
    private fun showUpdateDialog() {
        val context = requireContext().applicationContext
        thread {
            var reason: String? = null
            val release = try {
                fetchLatestRelease()
            } catch (e: Exception) {
                Log.w(UPDATE_LOG_TAG, "检查更新失败", e)
                reason = e.message ?: e.javaClass.simpleName
                null
            }
            activity?.runOnUiThread {
                if (!isAdded) {
                    return@runOnUiThread
                }
                if (release == null) {
                    val message = getString(
                        R.string.update_check_failed,
                        reason ?: getString(R.string.update_check_unknown_reason)
                    )
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val (currentCode, currentName) = currentVersion()
                if (isUpToDate(currentCode, currentName, release)) {
                    showLatestVersionDialog()
                } else {
                    showNewVersionDialog(release)
                }
            }
        }
    }

    /** GitHub 上最新 release 的信息 */
    private data class LatestRelease(
        val versionCode: Long?,
        val versionName: String,
        val apkUrl: String?
    )

    private fun currentVersion(): Pair<Long, String> {
        val context = requireContext()
        val info: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info) to (info.versionName ?: "")
    }

    private fun isUpToDate(currentCode: Long, currentName: String, release: LatestRelease): Boolean {
        val latestCode = release.versionCode
        if (latestCode != null) {
            return currentCode >= latestCode
        }
        // release 名字里只有 versionName（工作流现在就是这么发的），那就按版本号比
        return compareVersionName(currentName, release.versionName) >= 0
    }

    /** 版本号比较：按小数点分段比数字，缺的段按 0 算（1.1.10 > 1.1.2） */
    private fun compareVersionName(left: String, right: String): Int {
        val leftParts = left.split(".")
        val rightParts = right.split(".")
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val leftValue = leftParts.getOrNull(index)?.toIntOrNull() ?: 0
            val rightValue = rightParts.getOrNull(index)?.toIntOrNull() ?: 0
            if (leftValue != rightValue) {
                return leftValue - rightValue
            }
        }
        return 0
    }

    /** 已经是最新版本：只有一个确认按钮 */
    private fun showLatestVersionDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.update_latest)
            .setPositiveButton(R.string.confirm, null)
            .show()
    }

    /** 有新版本：取消 / 下载 */
    private fun showNewVersionDialog(release: LatestRelease) {
        val message = if (release.versionName.isBlank()) {
            getString(R.string.update_has_new)
        } else {
            getString(R.string.update_has_new_version, release.versionName)
        }
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.update_download) { _, _ -> downloadAndInstall(release) }
            .show()
    }

    /** 问 GitHub 要最新 release。*/
    private fun fetchLatestRelease(): LatestRelease {
        var apiError: Exception? = null
        try {
            return fetchFromApi()
        } catch (e: Exception) {
            Log.w(UPDATE_LOG_TAG, "GitHub API 拿不到，改用网页兜底", e)
            apiError = e
        }
        return try {
            fetchFromWeb()
        } catch (e: Exception) {
            Log.w(UPDATE_LOG_TAG, "网页兜底也失败", e)
            throw apiError ?: e
        }
    }

    /** GitHub API：/repos/{repo}/releases/latest */
    private fun fetchFromApi(): LatestRelease {
        val connection = (URL(UPDATE_API_LATEST).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = UPDATE_TIMEOUT_MS
            readTimeout = UPDATE_TIMEOUT_MS
            // GitHub API 不带 User-Agent 会被直接拒掉
            setRequestProperty("User-Agent", UPDATE_USER_AGENT)
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        return try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException(httpErrorText(code))
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseLatestRelease(JSONObject(body))
                ?: throw IOException("release 名字里解析不出版本号")
        } finally {
            connection.disconnect()
        }
    }

    /** 网页兜底：拿最新 tag，再按工作流的产物名拼下载地址 */
    private fun fetchFromWeb(): LatestRelease {
        val tag = latestTagFromWeb()
            ?: throw IOException("找不到已发布的版本（还没发 release 或仓库不公开）")
        val (versionCode, versionName) = parseVersion(tag)
        if (versionCode == null && versionName.isEmpty()) {
            throw IOException("tag 里解析不出版本号：" + tag)
        }
        return LatestRelease(versionCode, versionName, apkUrlOf(tag))
    }

    /** releases/latest 会跳到 .../releases/tag/<tag>，末尾那段就是 tag */
    private fun latestTagFromWeb(): String? {
        val connection = (URL(UPDATE_WEB_LATEST).openConnection() as HttpURLConnection).apply {
            connectTimeout = UPDATE_TIMEOUT_MS
            readTimeout = UPDATE_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", UPDATE_USER_AGENT)
        }
        return try {
            val code = connection.responseCode
            if (code !in REDIRECT_CODES) {
                throw IOException(httpErrorText(code))
            }
            connection.getHeaderField("Location")
                ?.trimEnd('/')
                ?.substringAfterLast('/')
                ?.ifBlank { null }
        } finally {
            connection.disconnect()
        }
    }

    /** 按工作流的产物名拼下载地址（app/build/outputs/apk/release/app-release.apk） */
    private fun apkUrlOf(tag: String): String =
        "https://github.com/$UPDATE_REPO/releases/download/$tag/$UPDATE_APK_NAME"

    /** 把 HTTP 状态码翻成能看懂的话 */
    private fun httpErrorText(code: Int): String {
        return if (code == HttpURLConnection.HTTP_NOT_FOUND) {
            "GitHub 上还没有发布任何版本（HTTP 404）"
        } else {
            "GitHub 返回 HTTP $code"
        }
    }

    /** 从 release 名字 / tag 里解析版本号：能拿到 versionCode 就返回它，否则只拿 versionName */
    private fun parseVersion(title: String): Pair<Long?, String> {
        // 先试带 versionCode 的写法："Release 4 v1.1.2"
        val withCode = RELEASE_WITH_CODE.find(title)
        if (withCode != null) {
            return withCode.groupValues[1].toLongOrNull() to withCode.groupValues[2]
        }
        // 再试只写 versionName 的写法："Release 1.1.2" / tag "v1.1.2"
        VERSION_NAME_PATTERN.find(title)?.let { return null to it.value }
        // 最后兜一下纯数字（tag "v4" 这种），当 versionCode 用
        val digits = title.filter { it.isDigit() }
        return if (digits.isEmpty()) null to "" else digits.toLongOrNull() to ""
    }

    /** 解析 API 回来的 release JSON */
    private fun parseLatestRelease(json: JSONObject): LatestRelease? {
        // name 是 release 标题，没写就用 tag 兜底
        val title = json.optString("name").ifBlank { json.optString("tag_name") }
        val (versionCode, versionName) = parseVersion(title)
        if (versionCode == null && versionName.isEmpty()) {
            return null
        }

        // 安装包：release 里第一个 .apk 资源（工作流传的是 app-release.apk）
        var apkUrl: String? = null
        val assets = json.optJSONArray("assets")
        for (index in 0 until (assets?.length() ?: 0)) {
            val asset = assets?.optJSONObject(index) ?: continue
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url").ifBlank { null }
                break
            }
        }
        // API 里没挂 apk 就按 tag 拼一个（工作流固定传 app-release.apk）
        if (apkUrl == null) {
            val tag = json.optString("tag_name")
            if (tag.isNotBlank()) {
                apkUrl = apkUrlOf(tag)
            }
        }
        return LatestRelease(versionCode, versionName, apkUrl)
    }

    /** 下载 apk，下完交给系统安装 */
    private fun downloadAndInstall(release: LatestRelease) {
        val apkUrl = release.apkUrl
        if (apkUrl == null) {
            Toast.makeText(requireContext(), R.string.update_no_apk, Toast.LENGTH_SHORT).show()
            return
        }

        val progressBar =
            ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal)
        progressBar.max = 100
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), 0)
            addView(
                progressBar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.update_downloading)
            .setView(content)
            .setCancelable(false)
            .create()
        dialog.show()

        val context = requireContext().applicationContext
        thread {
            val file = try {
                downloadApk(context, apkUrl, release.versionName) { percent ->
                    activity?.runOnUiThread { progressBar.progress = percent }
                }
            } catch (e: Exception) {
                null
            }

            activity?.runOnUiThread {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
                if (!isAdded) {
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

    /** 把 apk 下到 cacheDir/update 下；服务端给了长度就回报百分比 */
    private fun downloadApk(
        context: Context,
        url: String,
        versionName: String,
        onProgress: (Int) -> Unit
    ): File {
        val dir = File(context.cacheDir, UPDATE_DIR)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("create " + dir.absolutePath + " failed")
        }
        val target = File(dir, "ware-manage-" + versionName.ifBlank { "latest" } + ".apk")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = UPDATE_TIMEOUT_MS
            readTimeout = UPDATE_TIMEOUT_MS
            setRequestProperty("User-Agent", UPDATE_USER_AGENT)
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP " + connection.responseCode)
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) {
                            break
                        }
                        output.write(buffer, 0, count)
                        downloaded += count
                        if (total > 0) {
                            onProgress(((downloaded * 100) / total).toInt())
                        }
                    }
                }
            }
            // 下回来的必须是个 apk（apk 就是 zip，开头是 PK），不然多半是错误页面
            if (target.length() < UPDATE_MIN_APK_BYTES || !looksLikeApk(target)) {
                target.delete()
                throw IOException("not a apk")
            }
            return target
        } finally {
            connection.disconnect()
        }
    }

    /** apk 就是个 zip，开头两个字节是 "PK" */
    private fun looksLikeApk(file: File): Boolean {
        return try {
            FileInputStream(file).use { input ->
                val head = ByteArray(2)
                input.read(head) == 2 &&
                    head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    /** 弹出系统安装界面（apk 在应用私有目录里，要用 FileProvider 授权给安装器） */
    private fun installApk(file: File) {
        val context = requireContext()
        val uri = FileProvider.getUriForFile(
            context, context.packageName + FILE_PROVIDER_SUFFIX, file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.update_install_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /** 数据备份 / 恢复：让用户选导出还是导入 */
    private fun showBackupDialog() {
        val items = arrayOf(
            getString(R.string.settings_backup_export),
            getString(R.string.settings_backup_import)
        )
        AlertDialog.Builder(requireContext())
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
        AlertDialog.Builder(requireContext())
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
        val context = requireContext().applicationContext
        thread {
            val success = try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    BackupService(context).exportTo(output)
                } ?: false
            } catch (e: Exception) {
                false
            }
            activity?.runOnUiThread {
                if (!isAdded) {
                    return@runOnUiThread
                }
                val messageRes =
                    if (success) R.string.backup_export_success else R.string.backup_export_failed
                Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importBackup(uri: Uri) {
        val context = requireContext().applicationContext
        thread {
            val success = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BackupService(context).restoreFrom(input)
                } ?: false
            } catch (e: Exception) {
                false
            }
            activity?.runOnUiThread {
                if (!isAdded) {
                    return@runOnUiThread
                }
                val messageRes =
                    if (success) R.string.backup_import_success else R.string.backup_import_failed
                Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
                if (success) {
                    // 数据库连接已经在恢复时关掉了，这里重新查一遍就是新数据
                    loadInventory()
                }
            }
        }
    }

    /** 每次回到这个页面（包括从出入库界面返回）都重新查一遍存量 */
    override fun onResume() {
        super.onResume()
        loadInventory()
    }

    /** 从账单页切回来时也重建一遍，免得界面还是切走前那次测量出来的样子 */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            loadInventory()
        }
    }

    private fun loadInventory() {
        val context = requireContext().applicationContext
        thread {
            val inventories = InventoryDatabase.getDatabase(context).shoeInventoryDAO().queryAll()

            // 缩略图先在后台解码好，界面上直接取缓存
            inventories.forEach { inventory ->
                val path = inventory.imagePath
                if (!path.isNullOrEmpty() && pictureCache.get(path) == null) {
                    ImageUtils.decodeFromPath(path, ImageUtils.MAX_SHOW_SIZE)
                        ?.let { pictureCache.put(path, it) }
                }
            }

            activity?.runOnUiThread {
                if (!isAdded) {
                    return@runOnUiThread
                }
                allInventories = inventories
                searchInventory()
            }
        }
    }

    /** 按搜索框里的关键字过滤（货号 / 经销商，忽略大小写），过滤完重新画表格 */
    private fun searchInventory() {
        val keyword = searchInput.text.toString().trim()
        val visible = if (keyword.isEmpty()) {
            allInventories
        } else {
            allInventories.filter { inventory ->
                inventory.articleId.contains(keyword, ignoreCase = true) ||
                    inventory.dealer.contains(keyword, ignoreCase = true)
            }
        }
        showInventory(visible)
    }

    private fun showInventory(inventories: List<ShoeInventory>) {
        // 11 个尺码全是 0 的颜色不显示，不然会以为这个颜色还有货
        val visibleRows = inventories.filter { inventory ->
            SizeUtils.sizesOf(inventory).any { it > 0 }
        }
        val width = tableWidth()

        if (visibleRows == renderedInventories && width == renderedWidth) {
            emptyText.visibility = if (visibleRows.isEmpty()) View.VISIBLE else View.GONE
            return
        }

        tableContainer.removeAllViews()

        // 一个货号一张表（queryAll 已经按货号、颜色排好序，分组的先后顺序和查询一致）
        for ((articleId, rows) in visibleRows.groupBy { it.articleId }) {
            tableContainer.addView(buildArticleHeader(rows))
            tableContainer.addView(buildSizeTable(articleId, rows))
            tableContainer.addView(space(dp(12)))
        }

        renderedInventories = visibleRows
        renderedWidth = width
        emptyText.visibility = if (visibleRows.isEmpty()) View.VISIBLE else View.GONE
    }

    /** 货号这一块的表头：第一排列名（货号 / 经销商 / 数量），第二排数值 */
    private fun buildArticleHeader(rows: List<ShoeInventory>): View {
        val articleId = rows.first().articleId
        val dealer = rows.first().dealer
        val quantity = rows.sumOf { inventory -> SizeUtils.sizesOf(inventory).sum() }
        val headCell = ContextCompat.getDrawable(requireContext(), R.drawable.table_head_cell)

        val titleRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        titleRow.addView(
            cell(
                getString(R.string.purchase_order_article_id), 0, 1f, headCell,
                gravity = Gravity.CENTER_VERTICAL or Gravity.START, paddingStart = dp(6)
            )
        )
        titleRow.addView(cell(getString(R.string.purchase_order_dealer), 0, 1f, headCell))
        titleRow.addView(cell(getString(R.string.inventory_amount), 0, 1f, headCell))

        val valueRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        valueRow.addView(
            cell(
                articleId, 0, 1f, headCell, bold = true,
                gravity = Gravity.CENTER_VERTICAL or Gravity.START, paddingStart = dp(6)
            )
        )
        valueRow.addView(cell(dealer, 0, 1f, headCell))
        valueRow.addView(cell(quantity.toString(), 0, 1f, headCell, bold = true))

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                tableWidth(), ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(titleRow)
            addView(valueRow)
        }
    }

    /** 数量为 0 一律显示空白，免得看成一格一格的数字以为有货 */
    private fun countText(count: Int): String {
        if (count == 0) {
            return ""
        }
        return count.toString()
    }

    /**
     * 表格里的一个格子：宽高统一写死，三栏才能对齐。
     * width 传 0 表示用 weight 撑开（货号那一列就是这么做的）。
     */
    private fun cell(
        text: String,
        width: Int,
        weight: Float,
        background: Drawable?,
        bold: Boolean = false,
        gravity: Int = Gravity.CENTER,
        paddingStart: Int = 0
    ): TextView {
        val cellView = TextView(requireContext())
        cellView.layoutParams = LinearLayout.LayoutParams(width, rowHeight).apply {
            this.weight = weight
        }
        cellView.text = text
        cellView.gravity = gravity
        cellView.textSize = 14f
        cellView.maxLines = 1
        cellView.ellipsize = TextUtils.TruncateAt.END
        cellView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        // 背景必须给每个格子单独一份：共用一个 Drawable 的话，重绘时会按别的格子留下的 bounds 画
        cellView.background = DrawableUtils.ownDrawable(background, resources)
        if (paddingStart > 0) {
            cellView.setPaddingRelative(paddingStart, 0, 0, 0)
        }
        if (bold) {
            cellView.setTypeface(null, Typeface.BOLD)
        }
        return cellView
    }

    private fun space(height: Int): View = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /**
     * 每张表的固定宽度。
     *
     * 表格里的「货号格」和「中间尺码栏」用的是 weight，如果承载它们的行没有确定宽度，
     * weight 就没有参照物，从别的页面切回来重新测量时会算歪（第一行后半截会跑出屏幕）。
     * 所以这里优先用容器的真实宽度，容器还没测量出来时先用屏幕宽度兜底。
     */
    private fun tableWidth(): Int {
        val innerWidth = tableContainer.width - tableContainer.paddingLeft - tableContainer.paddingRight
        if (innerWidth > 0) {
            return innerWidth
        }
        return maxOf(dp(280), resources.displayMetrics.widthPixels - dp(20))
    }

    /**
     * 一个货号下面的颜色表：左栏（图片 + 颜色）和右栏（小计 + ＋ －）固定，
     * 中间的 34 ~ 44 码放在横向滚动里；每行高度一样，所以三栏能对齐。
     */
    private fun buildSizeTable(articleId: String, rows: List<ShoeInventory>): View {
        val headCell = ContextCompat.getDrawable(requireContext(), R.drawable.table_head_cell)
        val bodyCell = ContextCompat.getDrawable(requireContext(), R.drawable.edit_border)

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                tableWidth(), ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(buildLeftColumn(rows, headCell, bodyCell))
            addView(buildSizeScroll(rows, headCell, bodyCell))
            addView(buildRightColumn(articleId, rows, headCell, bodyCell))
        }
    }

    /** 左栏：图片（先占位）+ 颜色，固定不动 */
    private fun buildLeftColumn(
        rows: List<ShoeInventory>,
        headCell: Drawable?,
        bodyCell: Drawable?
    ): LinearLayout {
        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(leftPaneWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val head = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(cell(getString(R.string.inventory_picture), pictureCellWidth, 0f, headCell))
        head.addView(cell(getString(R.string.purchase_order_color), colorCellWidth, 0f, headCell))
        column.addView(head)

        rows.forEach { inventory ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(buildPictureCell(inventory))
            row.addView(cell(inventory.color.displayName, colorCellWidth, 0f, bodyCell))
            column.addView(row)
        }
        return column
    }

    /** 图片格子：有图就显示缩略图（后台已经解码好放在缓存里），没图就是一个空的带边框格子 */
    private fun buildPictureCell(inventory: ShoeInventory): ImageView {
        val imageView = ImageView(requireContext())
        imageView.layoutParams = LinearLayout.LayoutParams(pictureCellWidth, rowHeight)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.background =
            DrawableUtils.ownDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.edit_border), resources
            )
        imageView.setPadding(dp(2), dp(2), dp(2), dp(2))
        imageView.contentDescription = getString(R.string.row_picture_hint)

        val path = inventory.imagePath
        if (!path.isNullOrEmpty()) {
            pictureCache.get(path)?.let { imageView.setImageBitmap(it) }
        }
        return imageView
    }

    /** 中间：34 ~ 44 码，横向可滑动 */
    private fun buildSizeScroll(
        rows: List<ShoeInventory>,
        headCell: Drawable?,
        bodyCell: Drawable?
    ): HorizontalScrollView {
        val sizeTable = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val head = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        for (index in 0 until SizeUtils.SIZE_AMOUNT) {
            head.addView(cell(SizeUtils.sizeName(index).toString(), sizeCellWidth, 0f, headCell))
        }
        sizeTable.addView(head)

        rows.forEach { inventory ->
            val sizes = SizeUtils.sizesOf(inventory)
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                row.addView(cell(countText(sizes[index]), sizeCellWidth, 0f, bodyCell))
            }
            sizeTable.addView(row)
        }

        return HorizontalScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            isHorizontalScrollBarEnabled = false
            addView(sizeTable)
        }
    }

    /** 右栏：小计 + 行尾的 ＋ / －，固定不动 */
    private fun buildRightColumn(
        articleId: String,
        rows: List<ShoeInventory>,
        headCell: Drawable?,
        bodyCell: Drawable?
    ): LinearLayout {
        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(rightPaneWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val head = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(cell(getString(R.string.inventory_subtotal), subtotalCellWidth, 0f, headCell))
        head.addView(cell("", stockButtonWidth * 2, 0f, headCell))
        column.addView(head)

        rows.forEach { inventory ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(
                cell(countText(SizeUtils.sizesOf(inventory).sum()), subtotalCellWidth, 0f, bodyCell)
            )
            row.addView(buildStockButtons(articleId, inventory))
            column.addView(row)
        }
        return column
    }

    /** 行尾的 ＋ / －：打开出入库界面，货号、颜色、经销商都是锁死的 */
    private fun buildStockButtons(articleId: String, inventory: ShoeInventory): View {
        val buttonRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(stockButtonWidth * 2, rowHeight)
            gravity = Gravity.CENTER
        }

        val addButton = actionButton(getString(R.string.stock_add_button))
        addButton.setOnClickListener {
            startActivity(
                OrderEditActivity.createIntent(
                    requireContext(), OrderType.ARTICLE_PURCHASE,
                    articleId, inventory.color.displayName, inventory.dealer, inventory.imagePath
                )
            )
        }

        val minusButton = actionButton(getString(R.string.stock_minus_button))
        minusButton.setOnClickListener {
            startActivity(
                OrderEditActivity.createIntent(
                    requireContext(), OrderType.ARTICLE_SOLD_CASH,
                    articleId, inventory.color.displayName, inventory.dealer, inventory.imagePath
                )
            )
        }

        buttonRow.addView(addButton)
        buttonRow.addView(minusButton)
        return buttonRow
    }

    /** 行尾的小按钮：用 TextView 做，尺寸比较好控制 */
    private fun actionButton(text: String): TextView {
        val button = TextView(requireContext())
        button.layoutParams = LinearLayout.LayoutParams(stockButtonWidth, rowHeight - dp(8))
        button.text = text
        button.gravity = Gravity.CENTER
        button.textSize = 16f
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        button.background =
            DrawableUtils.ownDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.edit_border), resources
            )
        button.isClickable = true
        button.isFocusable = true
        return button
    }

    companion object {
        private const val UPDATE_REPO = "Qing0v0/ware-manage"
        private const val UPDATE_API_LATEST = "https://api.github.com/repos/$UPDATE_REPO/releases/latest"
        private const val UPDATE_WEB_LATEST = "https://github.com/$UPDATE_REPO/releases/latest"
        private const val UPDATE_APK_NAME = "app-release.apk"
        private const val UPDATE_LOG_TAG = "WareUpdate"
        private val REDIRECT_CODES = intArrayOf(301, 302, 303, 307, 308)

        /** 下载的新版 apk 放在 cacheDir 的这个子目录里（和 res/xml/file_paths.xml 对应） */
        private const val UPDATE_DIR = "update"

        /** 和 AndroidManifest 里 FileProvider 的 authorities 后缀保持一致 */
        private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

        private const val UPDATE_TIMEOUT_MS = 15_000
        private const val UPDATE_MIN_APK_BYTES = 10L * 1024
        private const val UPDATE_USER_AGENT = "ware-manage-android-update-check"

        /**
         * release 名字里带 versionCode 的格式："Release 4 v1.1.2"。
         * versionCode 和版本号之间必须有空格，不然 "Release 1.1.2" 会被拆成 1 和 1.2。
         */
        private val RELEASE_WITH_CODE = Regex("""(?i)release\s+(\d+)\s+v?(\d+(?:\.\d+)+)""")

        /** release 名字里只有 versionName 的格式："Release 1.1.2" */
        private val VERSION_NAME_PATTERN = Regex("""\d+(?:\.\d+)+""")
    }
}
