package com.example.myapplication.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderType
import com.example.myapplication.service.InventoryDatabase
import com.example.myapplication.ui.components.tableCellBackground
import com.example.myapplication.utils.SizeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 一律按东八区（北京时间）算日期、显示时间，不跟手机系统时区走 */
private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

private const val DEFAULT_DAYS = 30

/** 日期范围格子里显示的 yyyy-MM-dd */
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** 时间列：上排 MM-dd，下排 HH:mm，显示的是北京时间 */
private val dateLineFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd").withZone(zone)
private val timeLineFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(zone)

/** 表格列宽（和原布局里的 dp 值一一对应） */
private val ArticleIdCellWidth = 56.dp
private val ColorCellWidth = 44.dp
private val SizeCellWidth = 34.dp
private val TotalPriceCellWidth = 58.dp
private val TimeCellWidth = 56.dp
private val TypeCellWidth = 52.dp

/** 每行高度 */
private val RowHeight = 40.dp

/** 输入框里没有字时的提示语颜色（EditText 默认 hint 色） */
private val HintColor = Color(0xFF757575)

/** hintId 用 0 表示「不显示提示」 */
private const val HIDE_HINT = 0

/**
 * 账单页（Compose 版，对应原来的 res/layout/fragment_bills.xml）：
 * 按日期范围查询历史出入库订单，按时间从新到旧排。
 *
 * 一行一单：货号 | 颜色 | 34~44 码各多少双（中间可横滑）| 总价 | 时间 | 入库 / 出库 标签，
 * 最下面那一行不参与滚动，固定显示这段时间的总计利润。
 *
 * 页面复位（日期回默认、清空筛选和结果）由 [resetTick] 驱动，
 * BillsFragment 里每次切到账单页 +1，对应原来 resetPage() 的行为。
 */
@Composable
fun BillsScreen(
    /** 复位信号：变化一次就把页面恢复默认 */
    resetTick: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var startDate by remember {
        mutableStateOf(LocalDate.now(zone).minusDays((DEFAULT_DAYS - 1).toLong()))
    }
    var endDate by remember { mutableStateOf(LocalDate.now(zone)) }
    var articleId by remember { mutableStateOf("") }
    var dealer by remember { mutableStateOf("") }
    var payTypeIndex by remember { mutableIntStateOf(0) }
    var orders by remember { mutableStateOf<List<OrderBatch>>(emptyList()) }
    var hintId by remember { mutableIntStateOf(R.string.bills_hint_before_query) }
    var totalProfit by remember { mutableStateOf(BigDecimal.ZERO) }

    /**
     * "支付方式"筛选的选项：选项文字 -> 要匹配的订单类型名，和原来的 payTypeFilters() 一致：
     * 其中「出库」这一项把三个收款方式 + 退货 + 老版本的「出库」都算上，方便一眼看总出库。
     */
    val payTypeFilters = remember(context) {
        buildList {
            add(context.getString(R.string.bill_type_all) to OrderType.values().map { it.name })
            add(
                OrderType.ARTICLE_PURCHASE.displayName to listOf(OrderType.ARTICLE_PURCHASE.name)
            )
            add(
                OrderType.ARTICLE_SOLD.displayName to
                    OrderType.values().filter { !it.increaseStock }.map { it.name }
            )
            OrderType.soldTypes.forEach { add(it.displayName to listOf(it.name)) }
        }
    }

    /**
     * 页面复位：日期回到默认范围、清空订单表、总计归零。
     * 每次切到账单页（或再点一次账单 tab）Fragments 就让 resetTick +1，
     * 免得看到上一次查询留下的旧结果。
     */
    LaunchedEffect(resetTick) {
        startDate = LocalDate.now(zone).minusDays((DEFAULT_DAYS - 1).toLong())
        endDate = LocalDate.now(zone)
        articleId = ""
        dealer = ""
        payTypeIndex = 0
        orders = emptyList()
        hintId = R.string.bills_hint_before_query
        totalProfit = BigDecimal.ZERO
    }

    /** 点日期就弹日历 */
    fun pickDate(current: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth -> onPicked(LocalDate.of(year, month + 1, dayOfMonth)) },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    /** 选择日期范围后点查询，按条件去订单库查（查库在 IO 线程，结果回主线程） */
    fun queryOrders() {
        if (startDate.isAfter(endDate)) {
            hintId = R.string.bills_date_error
            return
        }

        val startTime = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        // 结束那一天也算进来，所以取第二天 0 点再减 1 毫秒
        val endTime = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        // 货号 / 经销商做模糊匹配；类型用下拉里选的那一组（不会为空，SQL 的 IN 不接受空列表）
        val articleIdLike = "%${articleId.trim()}%"
        val dealerLike = "%${dealer.trim()}%"
        val orderTypes = payTypeFilters[payTypeIndex].second
        val appContext = context.applicationContext

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                InventoryDatabase.getDatabase(appContext).orderBatchDAO()
                    .queryOrders(startTime, endTime, articleIdLike, dealerLike, orderTypes)
            }
            if (result.isEmpty()) {
                orders = emptyList()
                hintId = R.string.bills_empty
                totalProfit = BigDecimal.ZERO
            } else {
                hintId = HIDE_HINT
                orders = result
                totalProfit = calculateProfit(result)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 第一行：标题
        Text(
            text = stringResource(R.string.bills_title),
            color = Color.Black,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp)
        )

        // 日期范围 + 查询
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateBox(
                date = startDate,
                onClick = { pickDate(startDate) { startDate = it } },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.bills_date_separator),
                color = Color.Black,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            DateBox(
                date = endDate,
                onClick = { pickDate(endDate) { endDate = it } },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { queryOrders() }) {
                Text(stringResource(R.string.bills_query))
            }
        }

        // 第二行筛选：货号 / 经销商 / 支付方式
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterInput(
                value = articleId,
                onValueChange = { articleId = it },
                hint = stringResource(R.string.purchase_order_article_id),
                modifier = Modifier.weight(1f).height(44.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilterInput(
                value = dealer,
                onValueChange = { dealer = it },
                hint = stringResource(R.string.purchase_order_dealer),
                modifier = Modifier.weight(1f).height(44.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            PayTypeDropdown(
                selectedLabel = payTypeFilters[payTypeIndex].first,
                options = payTypeFilters.map { it.first },
                onSelect = { payTypeIndex = it },
                modifier = Modifier.weight(1.2f).height(44.dp)
            )
        }

        // 提示（查询前 / 日期填错 / 没查到订单）
        if (hintId != HIDE_HINT) {
            Text(
                text = stringResource(hintId),
                color = Color.Black,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        }

        // 订单表：可上下滑；查询结果出来前是空的
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
        ) {
            if (orders.isNotEmpty()) {
                OrdersTable(orders = orders)
            }
        }

        // 固定在最下面：不跟着上面一起滚
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.table_head_bg))
                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.bills_total_profit),
                color = Color.Black,
                fontSize = 16.sp
            )
            Text(
                text = amountText(totalProfit),
                // 亏了显示红色，赚了显示绿色
                color = colorResource(
                    if (totalProfit.signum() < 0) R.color.bill_out_text else R.color.bill_in_text
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 表格里的一个格子：宽高统一、单行居中，几栏才对得齐（对应原来的 cell()） */
@Composable
private fun BillsCell(
    text: String,
    width: Dp,
    modifier: Modifier = Modifier,
    head: Boolean = false,
    bold: Boolean = false,
    fontSize: TextUnit = 13.sp
) {
    Box(
        modifier = modifier
            .width(width)
            .height(RowHeight)
            .tableCellBackground(head),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = fontSize,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 筛选行里的输入框：带边框（edit_border），没字时显示灰灰的提示语 */
@Composable
private fun FilterInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .tableCellBackground()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(text = hint, color = HintColor, fontSize = 14.sp)
                }
                innerTextField()
            }
        }
    )
}

/** 日期格子：显示 yyyy-MM-dd，点了弹日历 */
@Composable
private fun DateBox(
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .tableCellBackground()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = date.format(dateFormatter), color = Color.Black, fontSize = 14.sp)
    }
}

/** 「支付方式」下拉：一个带边框的格子，点开是选项列表 */
@Composable
private fun PayTypeDropdown(
    selectedLabel: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.clickable { expanded = true }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .tableCellBackground()
                .padding(start = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = selectedLabel,
                color = Color.Black,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** 订单表：左栏（货号 + 颜色）和右栏（总价 + 时间 + 入/出）固定，中间尺码栏可以横滑 */
@Composable
private fun OrdersTable(orders: List<OrderBatch>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // 左栏：货号 + 颜色（固定不动）
        Column(modifier = Modifier.width(ArticleIdCellWidth + ColorCellWidth)) {
            Row {
                BillsCell(
                    stringResource(R.string.purchase_order_article_id),
                    ArticleIdCellWidth,
                    head = true,
                    bold = true
                )
                BillsCell(
                    stringResource(R.string.purchase_order_color),
                    ColorCellWidth,
                    head = true,
                    bold = true
                )
            }
            orders.forEach { order ->
                Row {
                    // 货号一般是 4~5 位字母 + 数字，字号小一点才塞得下
                    BillsCell(order.articleId, ArticleIdCellWidth, fontSize = 11.sp)
                    BillsCell(order.color.displayName, ColorCellWidth)
                }
            }
        }

        // 中间：34 ~ 44 码这一单各交易了多少双，横向可滑
        Column(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            Row {
                for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                    BillsCell(
                        SizeUtils.sizeName(index).toString(),
                        SizeCellWidth,
                        head = true,
                        bold = true
                    )
                }
            }
            orders.forEach { order ->
                val sizes = SizeUtils.sizesOf(order)
                Row {
                    for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                        BillsCell(countText(sizes[index]), SizeCellWidth)
                    }
                }
            }
        }

        // 右栏：总价 + 时间 + 入库 / 出库 标签（固定不动）
        Column(
            modifier = Modifier.width(TotalPriceCellWidth + TimeCellWidth + TypeCellWidth)
        ) {
            Row {
                BillsCell(
                    stringResource(R.string.bills_total_price),
                    TotalPriceCellWidth,
                    head = true,
                    bold = true
                )
                BillsCell(
                    stringResource(R.string.bills_time),
                    TimeCellWidth,
                    head = true,
                    bold = true
                )
                BillsCell(
                    stringResource(R.string.bills_type),
                    TypeCellWidth,
                    head = true,
                    bold = true
                )
            }
            orders.forEach { order ->
                Row {
                    BillsCell(amountText(totalPriceOf(order)), TotalPriceCellWidth)
                    TimeCell(order)
                    TypeCell(order)
                }
            }
        }
    }
}

/**
 * 时间格子：上下两行（MM-dd / HH:mm）拼在一个带边框的格子里，显示的是北京时间。
 * 拆成两行是因为塞进一个 TextView 时在固定高度的格子里会排到偏下的位置。
 */
@Composable
private fun TimeCell(order: OrderBatch) {
    val instant = order.date.toInstant()
    Box(
        modifier = Modifier
            .width(TimeCellWidth)
            .height(RowHeight)
            .tableCellBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dateLineFormatter.format(instant),
                    color = Color.Black,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeLineFormatter.format(instant),
                    color = Color.Black,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** 最后一栏：按订单类型显示标签（入库=浅绿，支付宝 / 微信 / 现金 / 退货=浅红） */
@Composable
private fun TypeCell(order: OrderBatch) {
    val backgroundRes =
        if (order.orderType.increaseStock) R.color.bill_in_bg else R.color.bill_out_bg

    Box(
        modifier = Modifier.width(TypeCellWidth).height(RowHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(TypeCellWidth - 10.dp)
                .height(RowHeight - 12.dp)
                .background(colorResource(backgroundRes), RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            // 「支付宝」是三个字，字号放小一点才塞得下
            Text(
                text = order.orderType.displayName,
                color = Color.Black,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
