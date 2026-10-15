package com.example.myapplication.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.model.Color as ShoeColor
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderType
import com.example.myapplication.service.InventoryDatabase
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
private val summaryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd")

/** 订单时间显示：日期 MM-dd、时间 HH:mm，统一按北京时间 */
private val dateLineFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd").withZone(zone)
private val timeLineFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(zone)

/** hintId 用 0 表示「不显示提示」 */
private const val HIDE_HINT = 0

/**
 * 账单页（Compose 版，对应原来的 res/layout/fragment_bills.xml）：
 * 按日期范围查询历史出入库账单，按时间从新到旧排。
 *
 * 账单以卡片显示，每笔账单的 34~44 码数量可横向滑动；
 * 底部固定显示所选日期范围的总计利润。
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
    val totalIncome = remember(orders) {
        orders.filter { !it.orderType.increaseStock }
            .fold(BigDecimal.ZERO) { total, order -> total.add(totalPriceOf(order)) }
    }
    val totalCost = remember(orders) {
        orders.filter { it.orderType.increaseStock }
            .fold(BigDecimal.ZERO) { total, order -> total.add(totalPriceOf(order)) }
    }

    /**
     * 账单类型筛选的选项：选项文字 -> 要匹配的账单类型名：
     * 其中「出库」这一项把三个收款方式 + 退货 + 老版本的「出库」都算上，方便一眼看总出库。
     */
    val orderTypeFilters = remember(context) {
        buildList {
            add(context.getString(R.string.bills_type_all) to OrderType.values().map { it.name })
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

    /** 选择日期范围后点查询，按条件查询账单（查库在 IO 线程，结果回主线程） */
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
        val orderTypes = orderTypeFilters[payTypeIndex].second
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F7F5))
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.bills_transaction_history),
                    color = Color(0xFF246B50),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.15.sp
                )
                Text(
                    text = stringResource(R.string.bills_title),
                    color = Color(0xFF19251F),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE9EEEA), RoundedCornerShape(17.dp))
                    .background(Color.White, RoundedCornerShape(17.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.bills_query_orders),
                    color = Color(0xFF77827B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterInput(
                        value = articleId,
                        onValueChange = { articleId = it },
                        hint = stringResource(R.string.purchase_order_article_id),
                        modifier = Modifier.weight(1f).height(36.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterInput(
                        value = dealer,
                        onValueChange = { dealer = it },
                        hint = stringResource(R.string.purchase_order_dealer),
                        modifier = Modifier.weight(1f).height(36.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OrderTypeDropdown(
                        selectedLabel = orderTypeFilters[payTypeIndex].first,
                        options = orderTypeFilters.map { it.first },
                        onSelect = { payTypeIndex = it },
                        modifier = Modifier.weight(1f).height(36.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateBox(
                        date = startDate,
                        onClick = { pickDate(startDate) { startDate = it } },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.bills_date_separator),
                        color = Color(0xFFA4ACA6),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                    DateBox(
                        date = endDate,
                        onClick = { pickDate(endDate) { endDate = it } },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { queryOrders() },
                        modifier = Modifier.height(39.dp),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF246B50),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 14.dp,
                            vertical = 0.dp
                        )
                    ) {
                        Text(stringResource(R.string.bills_query))
                    }
                }
            }
        }

        // 账单明细可上下滑，每笔账单的尺码行可独立左右滑
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.bills_order_details),
                        color = Color(0xFF26352B),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.bills_order_count, orders.size),
                        color = Color(0xFF89938C),
                        fontSize = 12.sp
                    )
                }
                if (hintId != HIDE_HINT) {
                    Text(
                        text = stringResource(hintId),
                        color = Color(0xFF69756D),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp)
                    )
                }
            }
            items(items = orders, key = { it.orderId }) { order ->
                OrderCard(order = order)
            }
        }

        // 固定在最下面：不跟着上面一起滚
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE9EEEA))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 10.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_profit_trend),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = stringResource(R.string.bills_total_profit),
                    color = Color(0xFF69756D),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${startDate.format(summaryDateFormatter)} — ${endDate.format(summaryDateFormatter)}",
                    color = Color(0xFF939D96),
                    fontSize = 12.sp
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 3.dp, end = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${if (totalProfit.signum() < 0) "-" else "+"}¥ ${amountText(totalProfit.abs())}",
                    color = colorResource(
                        if (totalProfit.signum() < 0) R.color.bill_out_text else R.color.bill_in_text
                    ),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.7).sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 3.dp)
                ) {
                    Text(
                        text = "${stringResource(R.string.selling_button)} ",
                        color = Color(0xFF909A93),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "¥${amountText(totalIncome)}",
                        color = Color(0xFF66736A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${stringResource(R.string.purchase_button)} ",
                        color = Color(0xFF909A93),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "¥${amountText(totalCost)}",
                        color = Color(0xFF66736A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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
        textStyle = TextStyle(color = Color(0xFF36443B), fontSize = 12.sp),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color(0xFFE9EEEA), RoundedCornerShape(10.dp))
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(text = hint, color = Color(0xFF8F9992), fontSize = 12.sp)
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
            .border(1.dp, Color(0xFFE9EEEA), RoundedCornerShape(11.dp))
            .background(Color.White, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.format(dateFormatter),
            color = Color(0xFF36443B),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

/** 订单类型下拉：一个带边框的格子，点开是选项列表 */
@Composable
private fun OrderTypeDropdown(
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
                .border(1.dp, Color(0xFFE9EEEA), RoundedCornerShape(10.dp))
                .background(Color.White, RoundedCornerShape(10.dp))
                .padding(horizontal = 9.dp),
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

@Composable
private fun OrderCard(order: OrderBatch) {
    val isPurchase = order.orderType.increaseStock
    val badgeTextColor = colorResource(
        if (isPurchase) R.color.bill_in_text else R.color.bill_out_text
    )
    val badgeBackground = colorResource(
        if (isPurchase) R.color.bill_in_bg else R.color.bill_out_bg
    )
    val instant = order.date.toInstant()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp)
            .border(1.dp, Color(0xFFE9EEEA), RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        colorResource(if (isPurchase) R.color.bill_in_bg else R.color.bill_out_bg),
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        if (isPurchase) R.drawable.ic_bill_in else R.drawable.ic_bill_out
                    ),
                    contentDescription = order.orderType.displayName,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.articleId,
                        color = Color(0xFF26352B),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = order.orderType.displayName,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .background(badgeBackground, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(colorSwatch(order.color), CircleShape)
                            .border(0.5.dp, Color(0x33000000), CircleShape)
                    )
                    Text(
                        text = "${order.color.displayName} · ${order.dealer}",
                        color = Color(0xFF7B877F),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 5.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥ ${amountText(totalPriceOf(order))}",
                    color = Color(0xFF26352B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "¥${amountText(order.price)} / 双",
                    color = Color(0xFF8A958D),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 9.dp)
                .background(Color(0xFFF6F8F6), RoundedCornerShape(10.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val sizes = SizeUtils.sizesOf(order)
            for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                Column(
                    modifier = Modifier.width(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = SizeUtils.sizeName(index).toString(),
                        color = Color(0xFF919B94),
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (sizes[index] == 0) "—" else countText(sizes[index]),
                        color = if (sizes[index] == 0) Color(0xFFC8CFCA) else Color(0xFF34443A),
                        fontSize = 11.sp,
                        fontWeight = if (sizes[index] == 0) FontWeight.Normal else FontWeight.Bold,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFF0F2F0))
        )
        Text(
            text = "${dateLineFormatter.format(instant)}  ${timeLineFormatter.format(instant)}",
            color = Color(0xFF89938C),
            fontSize = 10.sp,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

private fun colorSwatch(color: ShoeColor): Color = when (color) {
    ShoeColor.BLACK -> Color(0xFF252525)
    ShoeColor.WHITE -> Color(0xFFFFFFFF)
    ShoeColor.GRAY -> Color(0xFF92999B)
    ShoeColor.YELLOW -> Color(0xFFFFD54F)
    ShoeColor.GREEN -> Color(0xFF4CAF50)
    ShoeColor.BLUE -> Color(0xFF4285D4)
    ShoeColor.RED -> Color(0xFFE55353)
    ShoeColor.BROWN -> Color(0xFF8D5A3B)
    ShoeColor.COFFEE -> Color(0xFF6F4E37)
    ShoeColor.OTHER -> Color(0xFFB7BDB8)
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
