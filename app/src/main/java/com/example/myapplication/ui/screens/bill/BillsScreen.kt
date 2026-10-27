@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.myapplication.ui.screens.bill

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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderType
import com.example.myapplication.service.InventoryDatabase
import com.example.myapplication.ui.components.AppAlertDialog
import com.example.myapplication.ui.components.AppBottomSheetMenu
import com.example.myapplication.ui.components.CustomCalendarDialog
import com.example.myapplication.ui.components.AppBottomSheetMenuItem
import com.example.myapplication.ui.components.AppDialogSpec
import com.example.myapplication.ui.components.hideKeyboardOnAnyPress
import com.example.myapplication.ui.screens.main.PageTitleHeader
import com.example.myapplication.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val DEFAULT_BILL_RANGE_DAYS = 30
private val localDateSaver = Saver<LocalDate, String>(
    save = { it.toString() },
    restore = { LocalDate.parse(it) }
)

private enum class BillDateTarget {
    Start,
    End
}

@Composable
fun BillsScreen(
    resetTick: Int,
    moreMenuRequest: Int = 0,
    onBackupClick: () -> Unit = {},
    onUpdateClick: () -> Unit = {},
    dialog: AppDialogSpec? = null,
    onDialogDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialDate = remember { LocalDate.now(billZone) }

    var startDate by rememberSaveable(stateSaver = localDateSaver) {
        mutableStateOf(initialDate.minusDays((DEFAULT_BILL_RANGE_DAYS - 1).toLong()))
    }
    var endDate by rememberSaveable(stateSaver = localDateSaver) { mutableStateOf(initialDate) }
    var articleId by rememberSaveable { mutableStateOf("") }
    var dealer by rememberSaveable { mutableStateOf("") }
    var selectedTypeIndex by rememberSaveable { mutableIntStateOf(0) }
    var bills by remember { mutableStateOf<List<OrderBatch>>(emptyList()) }
    var hintId by rememberSaveable { mutableIntStateOf(R.string.bills_hint_before_query) }
    var datePickerTarget by rememberSaveable { mutableStateOf<BillDateTarget?>(null) }
    var previousResetTick by remember { mutableIntStateOf(resetTick) }
    var showMoreMenu by remember { mutableStateOf(false) }

    LaunchedEffect(moreMenuRequest) {
        if (moreMenuRequest > 0) {
            showMoreMenu = true
        }
    }

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
        if (resetTick == previousResetTick) {
            return@LaunchedEffect
        }
        previousResetTick = resetTick
        val resetDate = LocalDate.now(billZone)
        startDate = resetDate.minusDays((DEFAULT_BILL_RANGE_DAYS - 1).toLong())
        endDate = resetDate
        articleId = ""
        dealer = ""
        selectedTypeIndex = 0
        bills = emptyList()
        hintId = R.string.bills_hint_before_query
        datePickerTarget = null
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
            .background(AppColors.Gray50)
            .hideKeyboardOnAnyPress()
    ) {
        PageTitleHeader(
            eyebrow = stringResource(R.string.bills_transaction_history),
            title = stringResource(R.string.bills_title)
        )
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
            onStartDateClick = { datePickerTarget = BillDateTarget.Start },
            onEndDateClick = { datePickerTarget = BillDateTarget.End },
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

    datePickerTarget?.let { target ->
        CustomCalendarDialog(
            initialDate = if (target == BillDateTarget.Start) startDate else endDate,
            onDismiss = { datePickerTarget = null },
            onDateSelected = { selectedDate ->
                when (target) {
                    BillDateTarget.Start -> startDate = selectedDate
                    BillDateTarget.End -> endDate = selectedDate
                }
                datePickerTarget = null
            }
        )
    }

    AppBottomSheetMenu(
        visible = showMoreMenu,
        title = stringResource(R.string.more_options),
        items = listOf(
            AppBottomSheetMenuItem(
                label = stringResource(R.string.settings_backup),
                onClick = onBackupClick
            ),
            AppBottomSheetMenuItem(
                label = stringResource(R.string.settings_update),
                onClick = onUpdateClick
            )
        ),
        onDismissRequest = { showMoreMenu = false }
    )

    AppAlertDialog(
        dialog = dialog,
        onDismissRequest = onDialogDismiss
    )
}
