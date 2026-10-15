package com.example.myapplication.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.R

/** 表格格子的填充色：res/drawable/edit_border.xml 里写死的 #EFEFEF（没有对应 color 资源，照抄一份） */
private val EditBorderFill = Color(0xFFEFEFEF)

/** 细边框 / 分隔线颜色：drawable 里的 #505050，设置侧边栏的分隔线也用它 */
val HairlineColor = Color(0xFF505050)

/** edit_border 的圆角：3dp（table_head_cell 没写圆角，所以表头格子是直角） */
private val CellCornerRadius = 3.dp

/**
 * 表格格子 / 输入框的背景，替代原来的 @drawable/edit_border 和 @drawable/table_head_cell：
 * 填充 + 0.5px（物理像素，和 drawable 里一样）细边框，普通格子 3dp 圆角，表头格子米色直角。
 *
 * @param head true = 表头格子（米色底，table_head_cell），false = 普通格子 / 输入框（edit_border）
 */
@Composable
fun Modifier.tableCellBackground(head: Boolean = false): Modifier {
    val headFill = colorResource(R.color.table_head_bg)
    return drawBehind {
        val radius = if (head) 0f else CellCornerRadius.toPx()
        val corner = CornerRadius(radius, radius)
        drawRoundRect(
            color = if (head) headFill else EditBorderFill,
            cornerRadius = corner
        )
        drawRoundRect(
            color = HairlineColor,
            cornerRadius = corner,
            style = Stroke(width = 0.5f)
        )
    }
}