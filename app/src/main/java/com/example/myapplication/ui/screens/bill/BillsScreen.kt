package com.example.myapplication.ui.screens.bill

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.R
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderType
import com.example.myapplication.service.InventoryDatabase
import com.example.myapplication.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val DEFAULT_BILL_RANGE_DAYS = 30

@Composable
fun BillsScreen(
    resetTick: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialDate = remember { LocalDate.now(billZone) }

    var startDate by remember {
        mutableStateOf(initialDate.minusDays((DEFAULT_BILL_RANGE_DAYS - 1).toLong()))
    }
    var endDate by remember { mutableStateOf(initialDate) }
    var articleId by remember { mutableStateOf("") }
    var dealer by remember { mutableStateOf("") }
    var selectedTypeIndex by remember { mutableIntStateOf(0) }
    var bills by remember { mutableStateOf<List<OrderBatch>>(emptyList()) }
    var hintId by remember { mutableIntStateOf(R.string.bills_hint_before_query) }

    val billTypeFilters = remember(context) {
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

    LaunchedEffect(resetTick) {
        val resetDate = LocalDate.now(billZone)
        startDate = resetDate.minusDays((DEFAULT_BILL_RANGE_DAYS - 1).toLong())
        endDate = resetDate
        articleId = ""
        dealer = ""
        selectedTypeIndex = 0
        bills = emptyList()
        hintId = R.string.bills_hint_before_query
    }

    fun pickDate(current: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onPicked(LocalDate.of(year, month + 1, dayOfMonth))
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    fun queryBills() {
        if (startDate.isAfter(endDate)) {
            hintId = R.string.bills_date_error
            return
        }

        val startTime = startDate.atStartOfDay(billZone).toInstant().toEpochMilli()
        val endTime = endDate.plusDays(1).atStartOfDay(billZone).toInstant().toEpochMilli() - 1
        val articleIdLike = "%${articleId.trim()}%"
        val dealerLike = "%${dealer.trim()}%"
        val orderTypes = billTypeFilters[selectedTypeIndex].second
        val appContext = context.applicationContext

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                InventoryDatabase.getDatabase(appContext).orderBatchDAO()
                    .queryOrders(startTime, endTime, articleIdLike, dealerLike, orderTypes)
            }
            bills = result
            hintId = if (result.isEmpty()) R.string.bills_empty else HIDE_BILL_HINT
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.GrayF5F7F5)
    ) {
        BillQueryCard(
            startDate = startDate,
            endDate = endDate,
            articleId = articleId,
            dealer = dealer,
            selectedTypeIndex = selectedTypeIndex,
            orderTypeFilters = billTypeFilters,
            onArticleIdChange = { articleId = it },
            onDealerChange = { dealer = it },
            onTypeSelected = { selectedTypeIndex = it },
            onStartDateClick = { pickDate(startDate) { startDate = it } },
            onEndDateClick = { pickDate(endDate) { endDate = it } },
            onQuery = { queryBills() }
        )
        BillOrderDetails(
            bills = bills,
            hintId = hintId,
            modifier = Modifier.weight(1f)
        )
        BillProfitSummary(
            startDate = startDate,
            endDate = endDate,
            bills = bills
        )
    }
}
