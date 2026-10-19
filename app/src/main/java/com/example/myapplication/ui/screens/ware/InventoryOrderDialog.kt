package com.example.myapplication.ui.screens.ware

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderRowData
import com.example.myapplication.model.OrderType
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.R
import com.example.myapplication.service.InventoryDatabase
import com.example.myapplication.service.InventoryService
import com.example.myapplication.ui.components.AppModalDialog
import com.example.myapplication.ui.components.DropdownSelector
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography
import com.example.myapplication.utils.ImageUtils
import com.example.myapplication.utils.OrderBatchUtils
import com.example.myapplication.utils.SizeUtils
import java.util.UUID
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InventoryOrderDialog(
    orderType: OrderType,
    fixedInventory: ShoeInventory?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val colorOptions = remember {
        context.resources.getStringArray(R.array.color_array).toList()
    }
    var articleId by remember { mutableStateOf(fixedInventory?.articleId.orEmpty()) }
    var dealer by remember { mutableStateOf(fixedInventory?.dealer.orEmpty()) }
    var price by remember { mutableStateOf("") }
    var selectedPaymentType by remember { mutableStateOf(OrderType.soldTypes.first()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var pendingImageRowId by remember { mutableStateOf<String?>(null) }
    val rows = remember {
        mutableStateListOf(
            OrderDraftRow(
                color = fixedInventory?.color?.displayName ?: colorOptions.first(),
                imagePath = fixedInventory?.imagePath
            )
        )
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val rowId = pendingImageRowId
        pendingImageRowId = null
        val row = rows.firstOrNull { it.id == rowId }
        if (uri != null && row != null) {
            row.imageUri = uri
            errorMessage = null
        }
    }

    val title = stringResource(
        if (orderType.increaseStock) {
            R.string.order_editor_title_purchase
        } else {
            R.string.order_editor_title_selling
        }
    )
    val subtitle = if (fixedInventory != null) {
        stringResource(
            R.string.order_editor_fixed_subtitle,
            fixedInventory.articleId,
            fixedInventory.color.displayName
        )
    } else {
        stringResource(R.string.order_editor_subtitle)
    }

    AppModalDialog(
        title = title,
        subtitle = subtitle,
        onDismiss = { if (!isSaving) onDismiss() },
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp12)
            ) {
                OrderDialogButton(
                    text = stringResource(R.string.cancel),
                    primary = false,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                )
                OrderDialogButton(
                    text = stringResource(R.string.confirm),
                    primary = true,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val orderTypeForSave =
                            if (orderType.increaseStock) OrderType.ARTICLE_PURCHASE
                            else selectedPaymentType
                        val orderRows = rows.map { draft ->
                            OrderRowData(draft.color, draft.imagePath).also { row ->
                                draft.sizes.forEachIndexed { index, value ->
                                    row.sizes[index] = value
                                }
                                row.imageUri = draft.imageUri
                            }
                        }
                        if (orderRows.isEmpty()) {
                            errorMessage = context.getString(R.string.order_row_empty)
                            return@OrderDialogButton
                        }

                        val batches = ArrayList<OrderBatch>(orderRows.size)
                        for ((index, row) in orderRows.withIndex()) {
                            val batchUtils = OrderBatchUtils(
                                row.sizes.toTypedArray(),
                                articleId.trim(),
                                dealer.trim(),
                                row.color,
                                price.trim(),
                                orderTypeForSave
                            )
                            val validation = batchUtils.checkInputs(context)
                            if (validation != context.getString(R.string.checkOk)) {
                                errorMessage = if (
                                    validation == context.getString(R.string.sizeTypeError) ||
                                    validation == context.getString(R.string.sizeArrayEmpty)
                                ) {
                                    context.getString(R.string.order_row_error, index + 1, validation)
                                } else {
                                    validation
                                }
                                return@OrderDialogButton
                            }
                            batches.add(batchUtils.buildOrderBatch())
                        }

                        isSaving = true
                        errorMessage = null
                        val appContext = context.applicationContext
                        thread {
                            val imagePaths = orderRows.map { row ->
                                if (orderType.increaseStock && row.imageUri != null) {
                                    ImageUtils.saveToAppStorage(appContext, row.imageUri!!)
                                        ?: row.imagePath
                                } else {
                                    row.imagePath
                                }
                            }
                            val errors = try {
                                InventoryService(
                                    InventoryDatabase.getDatabase(appContext),
                                    appContext
                                ).apply(batches, imagePaths)
                            } catch (_: Exception) {
                                listOf(appContext.getString(R.string.order_save_failed))
                            }
                            Handler(Looper.getMainLooper()).post {
                                isSaving = false
                                if (errors.isNotEmpty()) {
                                    errorMessage = errors.joinToString("\n")
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.order_saved, batches.size),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onSaved()
                                }
                            }
                        }
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp12)
        ) {
            OrderField(
                label = stringResource(R.string.purchase_order_article_id),
                value = articleId,
                hint = stringResource(R.string.article_id_hint),
                enabled = fixedInventory == null,
                onValueChange = { articleId = it }
            )

            if (orderType.increaseStock) {
                OrderField(
                    label = stringResource(R.string.purchase_order_dealer),
                    value = dealer,
                    hint = stringResource(R.string.dealer_hint),
                    enabled = fixedInventory == null,
                    onValueChange = { dealer = it }
                )
            }

            OrderField(
                label = stringResource(
                    if (orderType.increaseStock) {
                        R.string.purchase_order_price
                    } else {
                        R.string.selling_order_price
                    }
                ),
                value = price,
                hint = stringResource(
                    if (orderType.increaseStock) {
                        R.string.purchase_price_hint
                    } else {
                        R.string.selling_price_hint
                    }
                ),
                keyboardType = KeyboardType.Decimal,
                onValueChange = { price = it }
            )

            if (!orderType.increaseStock) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp12)
                ) {
                    FormFieldLabel(stringResource(R.string.order_pay_type))
                    DropdownSelector(
                        selectedLabel = selectedPaymentType.displayName,
                        options = OrderType.soldTypes.map { it.displayName },
                        onSelect = { selectedPaymentType = OrderType.soldTypes[it] },
                        modifier = Modifier.weight(1f).height(AppDimensions.Size.Dp40)
                    )
                }
            }

            rows.forEach { row ->
                OrderDraftCard(
                    row = row,
                    colorOptions = colorOptions,
                    colorFixed = fixedInventory != null,
                    imageEditable = orderType.increaseStock,
                    showDelete = fixedInventory == null && rows.size > 1,
                    onColorChange = { row.color = it },
                    onDelete = {
                        rows.remove(row)
                        errorMessage = null
                    },
                    onPickImage = {
                        pendingImageRowId = row.id
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            if (fixedInventory == null) {
                OrderDialogButton(
                    text = stringResource(R.string.order_add_color),
                    primary = false,
                    enabled = rows.size < colorOptions.size,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val usedColors = rows.map { it.color }
                        val color = colorOptions.firstOrNull { it !in usedColors }
                        if (color != null) {
                            rows.add(OrderDraftRow(color))
                            errorMessage = null
                        } else {
                            errorMessage = context.getString(R.string.rows_max_num)
                        }
                    }
                )
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = AppColors.Red700,
                    style = AppTypography.Size12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppDimensions.Corner.small)
                        .background(AppColors.Red100)
                        .padding(AppDimensions.Padding.Dp12)
                )
            }
        }
    }
}

@Composable
private fun OrderDraftCard(
    row: OrderDraftRow,
    colorOptions: List<String>,
    colorFixed: Boolean,
    imageEditable: Boolean,
    showDelete: Boolean,
    onColorChange: (String) -> Unit,
    onDelete: () -> Unit,
    onPickImage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppDimensions.Corner.large)
            .background(AppColors.White)
            .border(1.dp, AppColors.Gray100, AppDimensions.Corner.large)
            .padding(AppDimensions.Padding.Dp12),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp12)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp12)
        ) {
            FormFieldLabel(stringResource(R.string.inventory_size_stock))
            if (colorFixed) {
                Text(
                    text = row.color,
                    color = AppColors.Gray900,
                    style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold)
                )
            } else {
                DropdownSelector(
                    selectedLabel = row.color,
                    options = colorOptions,
                    onSelect = { onColorChange(colorOptions[it]) },
                    modifier = Modifier.width(112.dp).height(35.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (showDelete) {
                Text(
                    text = stringResource(R.string.delete_order_row),
                    color = AppColors.Red700,
                    style = AppTypography.Size16,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AppColors.Red100)
                        .clickable(onClick = onDelete)
                        .padding(horizontal = AppDimensions.Padding.Dp12, vertical = AppDimensions.Padding.Dp8)
                )
            }
        }
        row.sizes.chunked(6).forEachIndexed { rowIndex, sizeRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp8)
            ) {
                sizeRow.forEachIndexed { columnIndex, count ->
                    val index = rowIndex * 6 + columnIndex
                    val cellShape = AppDimensions.Corner.small
                    val hasQuantity = count.isNotBlank()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(AppDimensions.Size.Dp56)
                            .clip(cellShape)
                            .background(if (hasQuantity) AppColors.Green50 else AppColors.White)
                            .border(
                                1.dp,
                                if (hasQuantity) AppColors.Green500 else AppColors.Gray100,
                                cellShape
                            )
                            .padding(horizontal = AppDimensions.Padding.Dp4, vertical = AppDimensions.Padding.Dp8),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = SizeUtils.sizeName(index).toString(),
                            color = AppColors.Gray500,
                            style = AppTypography.Size10
                        )
                        BasicTextField(
                            value = count,
                            onValueChange = { row.sizes[index] = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = AppTypography.Size12.copy(
                                color = AppColors.Green800,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AppDimensions.Padding.Dp8)
                        )
                    }
                }
                if (rowIndex == 1 && (imageEditable || !row.imagePath.isNullOrEmpty() || row.imageUri != null)) {
                    OrderImageCell(
                        row = row,
                        editable = imageEditable,
                        onClick = onPickImage
                    )
                }
            }
        }

    }
}

@Composable
private fun RowScope.OrderImageCell(
    row: OrderDraftRow,
    editable: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        row.imageUri,
        row.imagePath
    ) {
        value = withContext(Dispatchers.IO) {
            when {
                row.imageUri != null ->
                    ImageUtils.decodeFromUri(context, row.imageUri!!, ImageUtils.MAX_SHOW_SIZE)
                !row.imagePath.isNullOrEmpty() ->
                    ImageUtils.decodeFromPath(row.imagePath!!, ImageUtils.MAX_SHOW_SIZE)
                else -> null
            }?.asImageBitmap()
        }
    }
    val shape = AppDimensions.Corner.small
    Box(
        modifier = Modifier
            .weight(1f)
            .height(AppDimensions.Size.Dp56)
            .clip(shape)
            .clickable(enabled = editable, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = stringResource(R.string.row_picture_hint),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape)
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = AppColors.Green500,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                    )
                )
            }
            Text(
                text = "+",
                color = AppColors.Gray500,
                style = AppTypography.Size24.copy(fontWeight = FontWeight.Light)
            )
        }
    }
}

@Composable
private fun OrderField(
    label: String,
    value: String,
    hint: String,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp12)
    ) {
        FormFieldLabel(label, Modifier.width(58.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(AppDimensions.Size.Dp40)
                .clip(AppDimensions.Corner.small)
                .background(if (enabled) AppColors.White else AppColors.Gray50)
                .border(1.dp, AppColors.Gray100, AppDimensions.Corner.small)
                .padding(horizontal = AppDimensions.Padding.Dp12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        color = AppColors.Gray450,
                        style = AppTypography.Size12
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = AppTypography.Size12.copy(color = AppColors.Gray900),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FormFieldLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        color = AppColors.Gray800,
        style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold),
        modifier = modifier
    )
}

@Composable
private fun OrderDialogButton(
    text: String,
    primary: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = AppDimensions.Corner.small
    Box(
        modifier = modifier
            .height(AppDimensions.Size.Dp40)
            .clip(shape)
            .background(
                when {
                    !enabled -> AppColors.Gray100
                    primary -> AppColors.Green800
                    else -> AppColors.Green50
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                !enabled -> AppColors.Gray450
                primary -> AppColors.White
                else -> AppColors.Green800
            },
            style = AppTypography.Size12.copy(fontWeight = FontWeight.Bold)
        )
    }
}

private class OrderDraftRow(
    color: String,
    imagePath: String? = null
) {
    val id: String = UUID.randomUUID().toString()
    var color by mutableStateOf(color)
    val sizes = mutableStateListOf<String>().apply {
        repeat(SizeUtils.SIZE_AMOUNT) { add("") }
    }
    var imagePath: String? = imagePath
    var imageUri by mutableStateOf<Uri?>(null)
}
