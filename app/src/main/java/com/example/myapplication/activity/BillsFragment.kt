package com.example.myapplication.activity

import android.app.DatePickerDialog
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderType
import com.example.myapplication.service.OrderBatchDatabase
import com.example.myapplication.utils.DrawableUtils
import com.example.myapplication.utils.SizeUtils
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

/**
 * 账单页：按日期范围查询历史出入库订单，按时间从新到旧排。
 *
 * 一行一单：货号 | 颜色 | 34~44 码各多少双（中间可横滑）| 总价 | 时间 | 入库 / 出库 标签
 * 最下面那一行不参与滚动，固定显示这段时间的总计利润。
 */
class BillsFragment : Fragment() {

    private lateinit var tableContainer: LinearLayout
    private lateinit var hintText: TextView
    private lateinit var startDateText: TextView
    private lateinit var endDateText: TextView
    private lateinit var totalProfitText: TextView
    private lateinit var articleIdInput: EditText
    private lateinit var dealerInput: EditText
    private lateinit var payTypeDropdown: MaterialAutoCompleteTextView

    /** 账单页"支付方式"筛选当前匹配的订单类型名，默认全部 */
    private var selectedTypeNames: List<String> = OrderType.values().map { it.name }

    /** 一律按东八区（北京时间）算日期、显示时间，不跟手机系统时区走 */
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    /** 查询范围（含首尾两天），默认最近 30 天 */
    private var startDate: LocalDate = LocalDate.now(zone).minusDays((DEFAULT_DAYS - 1).toLong())
    private var endDate: LocalDate = LocalDate.now(zone)

    private val articleIdCellWidth by lazy { dp(56) }
    private val colorCellWidth by lazy { dp(44) }
    private val sizeCellWidth by lazy { dp(34) }
    private val totalPriceCellWidth by lazy { dp(58) }
    private val timeCellWidth by lazy { dp(56) }
    private val typeCellWidth by lazy { dp(52) }
    private val rowHeight by lazy { dp(40) }

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** 时间列：上排 MM-dd，下排 HH:mm；用两个单行格子拼起来，显示的是北京时间 */
    private val dateLineFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM-dd").withZone(zone)
    private val timeLineFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withZone(zone)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bills, container, false)
        tableContainer = view.findViewById(R.id.bills_table)
        hintText = view.findViewById(R.id.bills_hint)
        startDateText = view.findViewById(R.id.text_start_date)
        endDateText = view.findViewById(R.id.text_end_date)
        totalProfitText = view.findViewById(R.id.text_total_profit)
        articleIdInput = view.findViewById(R.id.bills_article_id)
        dealerInput = view.findViewById(R.id.bills_dealer)
        payTypeDropdown = view.findViewById(R.id.bills_pay_type)

        // 支付方式筛选：全部 / 入库 / 出库 / 支付宝 / 微信 / 现金 / 退货
        payTypeDropdown.setSimpleItems(payTypeFilters().map { it.first }.toTypedArray())
        payTypeDropdown.setText(getString(R.string.bill_type_all), false)
        payTypeDropdown.setOnClickListener { payTypeDropdown.showDropDown() }
        payTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedTypeNames = payTypeFilters()[position].second
        }

        startDateText.setOnClickListener {
            pickDate(startDate) { picked ->
                startDate = picked
                showDates()
            }
        }
        endDateText.setOnClickListener {
            pickDate(endDate) { picked ->
                endDate = picked
                showDates()
            }
        }
        view.findViewById<Button>(R.id.button_query).setOnClickListener { queryOrders() }

        resetPage()
        return view
    }

    /**
     * 页面复位：日期回到默认范围、清空订单表、总计归零。
     * 每次切到账单页（或再点一次账单 tab）都会调一次，免得看到上一次查询留下的旧结果。
     */
    fun resetPage() {
        if (view == null) {
            return
        }
        startDate = LocalDate.now(zone).minusDays((DEFAULT_DAYS - 1).toLong())
        endDate = LocalDate.now(zone)
        showDates()

        // 筛选条件也一起复位
        articleIdInput.setText("")
        dealerInput.setText("")
        selectedTypeNames = OrderType.values().map { it.name }
        payTypeDropdown.setText(getString(R.string.bill_type_all), false)

        tableContainer.removeAllViews()
        hintText.setText(R.string.bills_hint_before_query)
        hintText.visibility = View.VISIBLE
        showTotal(BigDecimal.ZERO)
    }

    /** 从别的页面切回来时把页面复位 */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            resetPage()
        }
    }

    private fun showDates() {
        startDateText.text = startDate.format(dateFormatter)
        endDateText.text = endDate.format(dateFormatter)
    }

    /** 点日期就弹日历 */
    private fun pickDate(current: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth -> onPicked(LocalDate.of(year, month + 1, dayOfMonth)) },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    /**
     * "支付方式"筛选的选项：选项文字 -> 要匹配的订单类型名。
     * 其中"出库"这一项把三个收款方式 + 退货 + 老版本的"出库"都算上，方便一眼看总出库。
     */
    private fun payTypeFilters(): List<Pair<String, List<String>>> {
        val options = ArrayList<Pair<String, List<String>>>()
        options.add(getString(R.string.bill_type_all) to OrderType.values().map { it.name })
        options.add(
            OrderType.ARTICLE_PURCHASE.displayName to listOf(OrderType.ARTICLE_PURCHASE.name)
        )
        options.add(OrderType.ARTICLE_SOLD.displayName to soldTypeNames())
        OrderType.soldTypes.forEach { options.add(it.displayName to listOf(it.name)) }
        return options
    }

    /** 所有"出库"类型的枚举名：支付宝 / 微信 / 现金 / 退货，以及老版本的出库 */
    private fun soldTypeNames(): List<String> {
        return OrderType.values().filter { !it.increaseStock }.map { it.name }
    }

    /** 选择日期范围后点查询，按条件去订单库查 */
    private fun queryOrders() {
        if (startDate.isAfter(endDate)) {
            hintText.setText(R.string.bills_date_error)
            hintText.visibility = View.VISIBLE
            return
        }

        val startTime = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        // 结束那一天也算进来，所以取第二天 0 点再减 1 毫秒
        val endTime = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        // 货号 / 经销商做模糊匹配；类型用下拉里选的那一组（不会为空，SQL 的 IN 不接受空列表）
        val articleIdLike = "%" + articleIdInput.text.toString().trim() + "%"
        val dealerLike = "%" + dealerInput.text.toString().trim() + "%"
        val orderTypes = selectedTypeNames

        val context = requireContext().applicationContext
        thread {
            val orders = OrderBatchDatabase.getDatabase(context).orderBatchDAO()
                .queryOrders(startTime, endTime, articleIdLike, dealerLike, orderTypes)
            activity?.runOnUiThread {
                if (!isAdded) {
                    return@runOnUiThread
                }
                showOrders(orders)
            }
        }
    }

    private fun showOrders(orders: List<OrderBatch>) {
        tableContainer.removeAllViews()

        if (orders.isEmpty()) {
            hintText.setText(R.string.bills_empty)
            hintText.visibility = View.VISIBLE
            showTotal(BigDecimal.ZERO)
            return
        }

        hintText.visibility = View.GONE
        tableContainer.addView(buildOrdersTable(orders))
        showTotal(calculateProfit(orders))
    }

    /** 订单表：左栏（单号 + 颜色）和右栏（总价 + 时间 + 入/出）固定，中间尺码栏可以横滑 */
    private fun buildOrdersTable(orders: List<OrderBatch>): View {
        val headCell = ContextCompat.getDrawable(requireContext(), R.drawable.table_head_cell)
        val bodyCell = ContextCompat.getDrawable(requireContext(), R.drawable.edit_border)

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                tableWidth(), ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(buildLeftPane(orders, headCell, bodyCell))
            addView(buildSizePane(orders, headCell, bodyCell))
            addView(buildRightPane(orders, headCell, bodyCell))
        }
    }

    /** 左栏：货号 + 颜色 */
    private fun buildLeftPane(
        orders: List<OrderBatch>,
        headCell: Drawable?,
        bodyCell: Drawable?
    ): LinearLayout {
        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                articleIdCellWidth + colorCellWidth, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val head = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(
            cell(
                getString(R.string.purchase_order_article_id), articleIdCellWidth, headCell,
                bold = true
            )
        )
        head.addView(cell(getString(R.string.purchase_order_color), colorCellWidth, headCell, bold = true))
        column.addView(head)

        orders.forEach { order ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            // 货号一般是 4~5 位字母 + 数字，字号小一点才塞得下
            row.addView(cell(order.articleId, articleIdCellWidth, bodyCell, textSize = 11f))
            row.addView(cell(order.color.displayName, colorCellWidth, bodyCell))
            column.addView(row)
        }
        return column
    }

    /** 中间：34 ~ 44 码这一单各交易了多少双，横向可滑 */
    private fun buildSizePane(
        orders: List<OrderBatch>,
        headCell: Drawable?,
        bodyCell: Drawable?
    ): HorizontalScrollView {
        val pane = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val head = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        for (index in 0 until SizeUtils.SIZE_AMOUNT) {
            head.addView(
                cell(SizeUtils.sizeName(index).toString(), sizeCellWidth, headCell, bold = true)
            )
        }
        pane.addView(head)

        orders.forEach { order ->
            val sizes = SizeUtils.sizesOf(order)
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                row.addView(cell(countText(sizes[index]), sizeCellWidth, bodyCell))
            }
            pane.addView(row)
        }

        return HorizontalScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            isHorizontalScrollBarEnabled = false
            addView(pane)
        }
    }

    /** 右栏：总价 + 时间 + 入库 / 出库 标签 */
    private fun buildRightPane(
        orders: List<OrderBatch>,
        headCell: Drawable?,
        bodyCell: Drawable?
    ): LinearLayout {
        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                totalPriceCellWidth + timeCellWidth + typeCellWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val head = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(
            cell(getString(R.string.bills_total_price), totalPriceCellWidth, headCell, bold = true)
        )
        head.addView(cell(getString(R.string.bills_time), timeCellWidth, headCell, bold = true))
        head.addView(cell(getString(R.string.bills_type), typeCellWidth, headCell, bold = true))
        column.addView(head)

        orders.forEach { order ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(cell(amountText(totalPriceOf(order)), totalPriceCellWidth, bodyCell))
            row.addView(buildTimeCell(order))
            row.addView(buildTypeCell(order))
            column.addView(row)
        }
        return column
    }

    /**
     * 时间格子：上下两个单行格子拼起来（MM-dd / HH:mm）。
     * 之前是把两行文字塞进一个 TextView，在固定高度的格子里会被排到偏下的位置（看着像整格缩到右下角），
     * 所以改成两个单行 TextView，位置就和其他格子一样稳了。
     */
    private fun buildTimeCell(order: OrderBatch): View {
        val instant = order.date.toInstant()
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(timeCellWidth, rowHeight)
            background = ContextCompat.getDrawable(requireContext(), R.drawable.edit_border)
        }
        container.addView(lineText(dateLineFormatter.format(instant)))
        container.addView(lineText(timeLineFormatter.format(instant)))
        return container
    }

    /** 时间格子里的一行小字 */
    private fun lineText(text: String): TextView {
        val line = TextView(requireContext())
        line.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, rowHeight / 2
        )
        line.text = text
        line.gravity = Gravity.CENTER
        line.textSize = 11f
        line.maxLines = 1
        line.setIncludeFontPadding(false)
        line.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        return line
    }

    /** 最后一栏：按订单类型显示标签（入库=浅绿，支付宝/微信/现金/退货=浅红） */
    private fun buildTypeCell(order: OrderBatch): View {
        val badge = TextView(requireContext())
        badge.layoutParams = LinearLayout.LayoutParams(typeCellWidth - dp(10), rowHeight - dp(12))
        badge.gravity = Gravity.CENTER
        // "支付宝"是三个字，字号放小一点才塞得下
        badge.textSize = 10.5f
        badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        badge.text = order.orderType.displayName
        val backgroundRes = if (order.orderType.increaseStock) {
            R.drawable.bill_badge_in
        } else {
            R.drawable.bill_badge_out
        }
        badge.background =
            DrawableUtils.ownDrawable(
                ContextCompat.getDrawable(requireContext(), backgroundRes), resources
            )

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(typeCellWidth, rowHeight)
            gravity = Gravity.CENTER
            addView(badge)
        }
    }

    /** 一单的总价 = 单价 × 这一单的总双数 */
    private fun totalPriceOf(order: OrderBatch): BigDecimal {
        return order.price.multiply(BigDecimal(SizeUtils.sizesOf(order).sum()))
    }

    /** 总计利润 = 这段时间收到的钱（卖出 / 退货） - 花掉的钱（进货） */
    private fun calculateProfit(orders: List<OrderBatch>): BigDecimal {
        var income = BigDecimal.ZERO
        var cost = BigDecimal.ZERO
        orders.forEach { order ->
            if (order.orderType.increaseStock) {
                cost = cost.add(totalPriceOf(order))
            } else {
                income = income.add(totalPriceOf(order))
            }
        }
        return income.subtract(cost)
    }

    private fun showTotal(profit: BigDecimal) {
        totalProfitText.text = amountText(profit)
        // 亏了显示红色，赚了显示绿色
        val colorRes = if (profit.signum() < 0) R.color.bill_out_text else R.color.bill_in_text
        totalProfitText.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    /** 金额统一用这个格式：去掉多余的小数位 */
    private fun amountText(amount: BigDecimal): String {
        return amount.stripTrailingZeros().toPlainString()
    }

    /** 数量为 0 就显示空白，免得看成一格一格的数字 */
    private fun countText(count: Int): String {
        if (count == 0) {
            return ""
        }
        return count.toString()
    }

    /** 表格里的一个格子：宽高统一、单行居中，三栏才对得齐 */
    private fun cell(
        text: String,
        width: Int,
        background: Drawable?,
        bold: Boolean = false,
        textSize: Float = 13f
    ): TextView {
        val cellView = TextView(requireContext())
        cellView.layoutParams = LinearLayout.LayoutParams(width, rowHeight)
        cellView.text = text
        cellView.gravity = Gravity.CENTER
        cellView.textSize = textSize
        cellView.maxLines = 1
        cellView.ellipsize = TextUtils.TruncateAt.END
        cellView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        // 背景必须给每个格子单独一份：共用一个 Drawable 的话，重绘时会按别的格子留下的 bounds 画
        cellView.background = DrawableUtils.ownDrawable(background, resources)
        if (bold) {
            cellView.setTypeface(null, Typeface.BOLD)
        }
        return cellView
    }

    /** 表格的固定宽度，让里面的 weight 有确定参照物（和仓库页同一个道理） */
    private fun tableWidth(): Int {
        val innerWidth = tableContainer.width - tableContainer.paddingLeft - tableContainer.paddingRight
        if (innerWidth > 0) {
            return innerWidth
        }
        return maxOf(dp(280), resources.displayMetrics.widthPixels - dp(20))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        /** 默认查最近 30 天 */
        private const val DEFAULT_DAYS = 30
    }
}
