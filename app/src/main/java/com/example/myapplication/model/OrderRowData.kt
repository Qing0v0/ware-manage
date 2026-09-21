package com.example.myapplication.model

import android.net.Uri

/**
 * 入库 / 出库界面表格中“一行”的数据，只用于界面层，不参与 Room 存储。
 *
 * sizes 的顺序与 [OrderBatch] 里的 size34 ~ size44 一一对应：
 * sizes[0] = 34 码，sizes[1] = 35 码 …… sizes[10] = 44 码；空字符串代表这一格没填。
 */
class OrderRowData(
    /** 颜色，取值与 @array/color_array 中的文字一致（黑色 / 白色 / 灰色 / 其他） */
    var color: String,
    /** 存量里已有的图片路径（从仓库页面的 ＋ / － 进来时带过来的），出库时只能看不能改 */
    var imagePath: String? = null
) {
    /** 各尺码的数量 */
    val sizes: MutableList<String> = MutableList(SIZE_AMOUNT) { "" }

    /** 刚在相册里选好的图片，还没落盘；点确定保存时才会压缩存到本地并写进存量 */
    var imageUri: Uri? = null

    companion object {
        /** 34 ~ 44 一共 11 个尺码 */
        const val SIZE_AMOUNT = 11
    }
}
