package com.example.myapplication.activity

import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.util.LruCache
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.model.OrderType
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.service.InventoryDatabase
import com.example.myapplication.utils.ImageUtils
import com.example.myapplication.utils.SizeUtils
import kotlin.concurrent.thread

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

    /** 存量缩略图缓存：路径 -> Bitmap，避免每次回到页面都重新解码 */
    private val pictureCache = LruCache<String, Bitmap>(64)

    private val pictureCellWidth by lazy { dp(44) }
    private val colorCellWidth by lazy { dp(60) }
    private val leftPaneWidth by lazy { pictureCellWidth + colorCellWidth }
    private val sizeCellWidth by lazy { dp(44) }
    private val subtotalCellWidth by lazy { dp(52) }
    private val stockButtonWidth by lazy { dp(32) }
    private val rightPaneWidth by lazy { subtotalCellWidth + stockButtonWidth * 2 }
    private val amountCellWidth by lazy { dp(110) }
    private val dealerCellWidth by lazy { dp(110) }
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
            startActivity(OrderEditActivity.createIntent(requireContext(), OrderType.ARTICLE_SOLD))
        }

        return view
    }

    /** 每次回到这个页面（包括从出入库界面返回）都重新查一遍存量 */
    override fun onResume() {
        super.onResume()
        loadInventory()
    }

    /** 从利润页切回来时也重建一遍，免得界面还是切走前那次测量出来的样子 */
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
                showInventory(inventories)
            }
        }
    }

    private fun showInventory(inventories: List<ShoeInventory>) {
        tableContainer.removeAllViews()

        var visibleRowCount = 0
        // 一个货号一张表（queryAll 已经按货号、颜色排好序，分组的先后顺序和查询一致）
        for ((articleId, rows) in inventories.groupBy { it.articleId }) {
            // 11 个尺码全是 0 的颜色不显示，不然会以为这个颜色还有货
            val visibleRows = rows.filter { inventory -> SizeUtils.sizesOf(inventory).any { it > 0 } }
            if (visibleRows.isEmpty()) {
                continue
            }

            visibleRowCount += visibleRows.size
            tableContainer.addView(buildArticleHeader(visibleRows))
            tableContainer.addView(buildSizeTable(articleId, visibleRows))
            tableContainer.addView(space(dp(12)))
        }

        emptyText.visibility = if (visibleRowCount == 0) View.VISIBLE else View.GONE
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
        titleRow.addView(cell(getString(R.string.purchase_order_dealer), dealerCellWidth, 0f, headCell))
        titleRow.addView(cell(getString(R.string.inventory_amount), amountCellWidth, 0f, headCell))

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
        valueRow.addView(cell(dealerText(dealer), dealerCellWidth, 0f, headCell))
        valueRow.addView(cell(quantity.toString(), amountCellWidth, 0f, headCell, bold = true))

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                tableWidth(), ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(titleRow)
            addView(valueRow)
        }
    }

    /** 经销商没填就显示一个 \ */
    private fun dealerText(dealer: String): String {
        if (dealer.isEmpty()) {
            return "\\"
        }
        return dealer
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
        cellView.background = background
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
        imageView.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_border)
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
                    requireContext(), OrderType.ARTICLE_SOLD,
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
        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_border)
        button.isClickable = true
        button.isFocusable = true
        return button
    }
}
