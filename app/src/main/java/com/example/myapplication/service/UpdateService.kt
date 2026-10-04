package com.example.myapplication.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * 版本更新：问 GitHub 要最新 release、比较版本、下载 apk、拼安装用的 Intent。
 *
 * 只管数据，不弹框（提示框、进度框在 WareDrawerPanel 里）。
 * release 名字是工作流（.github/workflows/android.yml）拼出来的：
 * 现在发出来的是 "Release 1.1.2"（Release + versionName）；
 * 如果名字里带上 versionCode（形如 "Release 4 v1.1.2"）也认，两种格式都能比。
 */
object UpdateService {

    /** 检查 / 下载的 log 标签：adb logcat -s WareUpdate */
    const val LOG_TAG = "WareUpdate"

    /** 版本更新检查用的仓库 */
    private const val REPO = "Qing0v0/ware-manage"

    /** GitHub API：最新 release（信息最全，优先用） */
    private const val API_LATEST = "https://api.github.com/repos/$REPO/releases/latest"

    /** 网页兜底地址（会 302 到具体 tag，api.github.com 连不上时用） */
    private const val WEB_LATEST = "https://github.com/$REPO/releases/latest"

    /** 工作流上传的 apk 名字（app/build/outputs/apk/release 下的产物名） */
    private const val APK_NAME = "app-release.apk"

    /** 下载的新版 apk 放在 cacheDir 的这个子目录里（和 res/xml/file_paths.xml 对应） */
    private const val UPDATE_DIR = "update"

    /** 和 AndroidManifest 里 FileProvider 的 authorities 后缀保持一致 */
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    private const val TIMEOUT_MS = 15_000
    private const val MIN_APK_BYTES = 10L * 1024
    private const val USER_AGENT = "ware-manage-android-update-check"

    /** 网页跳转的状态码 */
    private val REDIRECT_CODES = intArrayOf(301, 302, 303, 307, 308)

    /**
     * release 名字里带 versionCode 的格式："Release 4 v1.1.2"。
     * versionCode 和版本号之间必须有空格，不然 "Release 1.1.2" 会被拆成 1 和 1.2。
     */
    private val RELEASE_WITH_CODE = Regex("""(?i)release\s+(\d+)\s+v?(\d+(?:\.\d+)+)""")

    /** release 名字里只有 versionName 的格式："Release 1.1.2" */
    private val VERSION_NAME_PATTERN = Regex("""\d+(?:\.\d+)+""")

    /** GitHub 上最新 release 的信息 */
    data class LatestRelease(
        /** release 名字里写了 versionCode 才有 */
        val versionCode: Long?,
        /** 形如 1.1.2；名字里没有就是空串 */
        val versionName: String,
        /** release 里那个 apk 资源的下载地址；没挂 apk 就是 null */
        val apkUrl: String?
    )

    /**
     * 问 GitHub 要最新 release。
     *
     * 先走 API（信息最全：release 名字 + 资源列表）；失败就走网页兜底
     * （api.github.com 在有些网络下连不上，但 github.com 通常能连。
     * github.com/.../releases/latest 会 302 到具体 tag，从跳转地址把 tag 抠出来）。
     * 两条路都拿不到就抛异常，异常消息会显示在提示框里，方便直接看出原因。
     */
    fun fetchLatestRelease(): LatestRelease {
        var apiError: Exception? = null
        try {
            return fetchFromApi()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "GitHub API 拿不到，改用网页兜底", e)
            apiError = e
        }
        return try {
            fetchFromWeb()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "网页兜底也失败", e)
            // API 的错误更有信息量（比如 404 = 还没发版本），优先报它
            throw apiError ?: e
        }
    }

    /** GitHub API：/repos/{repo}/releases/latest */
    private fun fetchFromApi(): LatestRelease {
        val connection = (URL(API_LATEST).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            // GitHub API 不带 User-Agent 会被直接拒掉
            setRequestProperty("User-Agent", USER_AGENT)
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
        val connection = (URL(WEB_LATEST).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            // 只看跳转地址，不要跟着跳
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", USER_AGENT)
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
        "https://github.com/$REPO/releases/download/$tag/$APK_NAME"

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

    /** 本机安装包的 versionCode / versionName */
    fun currentVersion(context: Context): Pair<Long, String> {
        val info: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info) to (info.versionName ?: "")
    }

    /** 本机是不是已经不比线上旧了 */
    fun isUpToDate(currentCode: Long, currentName: String, release: LatestRelease): Boolean {
        val latestCode = release.versionCode
        if (latestCode != null) {
            return currentCode >= latestCode
        }
        // release 名字里只有 versionName（工作流现在就是这么发的），那就按版本号比
        return compareVersionName(currentName, release.versionName) >= 0
    }

    /** 版本号比较：按小数点分段比数字，缺的段按 0 算（1.1.10 > 1.1.2） */
    fun compareVersionName(left: String, right: String): Int {
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

    /**
     * 把 apk 下到 cacheDir/update 下。
     *
     * 下载前会把这个目录里上一次留下的安装包清掉：一次只留一个，
     * 免得升级几次以后越堆越多（本次要下的这个不删）。
     * 服务端给了长度就回报百分比。
     */
    fun downloadApk(
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

        // 一次只留一个安装包：把上一次下的（旧版本）清掉，免得越堆越多。
        dir.listFiles()?.forEach { old ->
            if (old.isFile && old.name != target.name) {
                old.delete()
            }
        }

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
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
            if (target.length() < MIN_APK_BYTES || !looksLikeApk(target)) {
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

    /** 拼一个"安装这个 apk"的 Intent（apk 在应用私有目录里，要用 FileProvider 授权） */
    fun installIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context, context.packageName + FILE_PROVIDER_SUFFIX, file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
