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
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderType
import com.example.myapplication.service.OrderBatchDatabase
import com.example.myapplication.utils.SizeUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

/**
 * 账单页：按日期范围查询历史出入库订单，按时间从新到旧排。
 *
 * 一行一单：单号 | 颜色 | 34~44 码各多少双（中间可横滑）| 总价 | 时间 | 入库 / 出库 标签
 * 最下面那一行不参与滚动，固定显示这段时间的总计利润。
 */
class BillsFragment : Fragment() {

    private lateinit var tableContainer: LinearLayout
    private lateinit var hintText: TextView
    private lateinit var startDateText: TextView
    private lateinit var endDateText: TextView
    private lateinit var totalProfitText: TextView

    /** 一律按东八区（北京时间）算日期、显示时间，不跟手机系统时区走 */
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    /** 查询范围（含首尾两天），默认最近 30 天 */
    private var startDate: LocalDate = LocalDate.now(zone).minusDays((DEFAULT_DAYS - 1).toLong())
    private var endDate: LocalDate = LocalDate.now(zone)

    private val orderIdCellWidth by lazy { dp(44) }
    private val colorCellWidth by lazy { dp(46) }
    private val sizeCellWidth by lazy { dp(34) }
    private val totalPriceCellWidth by lazy { dp(64) }
    private val timeCellWidth by lazy { dp(56) }
    private val typeCellWidth by lazy { dp(46) }
    private val rowHeight by lazy { dp(40) }

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** 时间格子分两行显示（省宽度），显示的是北京时间 */
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM-dd\nHH:mm").withZone(zone)

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

    /** 查询按钮：按日期范围去订单库查，查完从新到旧显示 */
    private fun queryOrders() {
        if (startDate.isAfter(endDate)) {
            hintText.setText(R.string.bills_date_error)
            hintText.visibility = View.VISIBLE
            return
        }

        val startTime = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        // 结束那一天也算进来，所以取第二天 0 点再减 1 毫秒
        val endTime = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val context = requireContext().applicationContext
        thread {
            val orders = OrderBatchDatabase.getDatabase(context).orderBatchDAO()
                .queryByDateRange(startTime, endTime)
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

    /** 左栏：单号 + 颜色 */
    private fun buildLeftPane(
        orders: List<OrderBatch>,
        headCell: Drawable?,
        bodyCell: Drawable?
    ): LinearLayout {
        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                orderIdCellWidth + colorCellWidth, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val head = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(cell(getString(R.string.bills_order_id), orderIdCellWidth, headCell, bold = true))
        head.addView(cell(getString(R.string.purchase_order_color), colorCellWidth, headCell, bold = true))
        column.addView(head)

        orders.forEach { order ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(cell(order.orderId.toString(), orderIdCellWidth, bodyCell))
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
            row.addView(
                cell(timeFormatter.format(order.date.toInstant()), timeCellWidth, bodyCell, textSize = 11f)
            )
            row.addView(buildTypeCell(order))
            column.addView(row)
        }
        return column
    }

    /** 最后一栏：浅绿色的「入库」或浅红色的「出库」矩形 */
    private fun buildTypeCell(order: OrderBatch): View {
        val badge = TextView(requireContext())
        badge.layoutParams = LinearLayout.LayoutParams(typeCellWidth - dp(10), rowHeight - dp(12))
        badge.gravity = Gravity.CENTER
        badge.textSize = 12f
        badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        if (order.orderType.increaseStock) {
            badge.setText(R.string.bill_type_in)
            badge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bill_badge_in)
        } else {
            badge.setText(R.string.bill_type_out)
            badge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bill_badge_out)
        }

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

    /** 总计利润 = 这段时间的出库总额 - 入库总额（破损、退货还没有入口，暂时不计入） */
    private fun calculateProfit(orders: List<OrderBatch>): BigDecimal {
        var soldTotal = BigDecimal.ZERO
        var purchasedTotal = BigDecimal.ZERO
        orders.forEach { order ->
            when (order.orderType) {
                OrderType.ARTICLE_SOLD -> soldTotal = soldTotal.add(totalPriceOf(order))
                OrderType.ARTICLE_PURCHASE -> purchasedTotal = purchasedTotal.add(totalPriceOf(order))
                else -> Unit
            }
        }
        return soldTotal.subtract(purchasedTotal)
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

    /** 表格里的一个格子：宽高统一，三栏才对得齐 */
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
        cellView.maxLines = 2
        cellView.ellipsize = TextUtils.TruncateAt.END
        cellView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        cellView.background = background
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
