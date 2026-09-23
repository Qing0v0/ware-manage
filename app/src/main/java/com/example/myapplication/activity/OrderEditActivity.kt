package com.example.myapplication.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.OrderRowAdapter
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderRowData
import com.example.myapplication.model.OrderType
import com.example.myapplication.service.InventoryDatabase
import com.example.myapplication.service.InventoryService
import com.example.myapplication.service.OrderBatchDatabase
import com.example.myapplication.utils.ImageUtils
import com.example.myapplication.utils.OrderBatchUtils
import com.example.myapplication.utils.StringUtils
import com.google.android.material.textfield.MaterialAutoCompleteTextView

import kotlin.concurrent.thread

/**
 * 入库 / 出库界面（布局：R.layout.purchase_order）。
 *
 * - 入库：OrderType.ARTICLE_PURCHASE，填写货号、经销商、进价
 * - 出库：支付宝 / 微信 / 现金 / 退货（OrderType.soldTypes），不需要经销商，价格是售价
 *
 * 表头是整张单据的公共信息，颜色不在这里选；表格里每一行 = 一种颜色 + 34~44 码的数量（即一批），
 * 点确定时每一行都会生成一条 [OrderBatch]。
 */
class OrderEditActivity : AppCompatActivity() {

    private lateinit var orderType: OrderType

    private lateinit var articleIdFill: EditText
    private lateinit var dealerFill: EditText
    private lateinit var priceFill: EditText
    private lateinit var payTypeDropdown: MaterialAutoCompleteTextView

    private lateinit var orderRowTable: RecyclerView
    private lateinit var orderRowAdapter: OrderRowAdapter

    /** 从仓库页面的 ＋ / － 进来时锁定的颜色，为 null 表示货号 / 颜色由用户自己填 */
    private var fixedColor: String? = null

    /** 从仓库页面带过来的图片路径（出库时只显示不能改） */
    private var fixedImagePath: String? = null

    /** 正在等哪一行选图片（相册选完回来要用），null 表示没在等 */
    private var pendingImageRowPosition: Int? = null

    /** 相册选图：选完把图挂到刚才那一行上，点确定时才真正压缩存盘 */
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val position = pendingImageRowPosition
        pendingImageRowPosition = null
        if (uri != null && position != null) {
            orderRowAdapter.setRowImage(position, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.purchase_order)

        val rootView: View = findViewById(R.id.order_edit_root)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 键盘弹出来的时候把底部的按钮顶上去，避免被挡住
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }

        orderType = readOrderType(intent)

        articleIdFill = findViewById(R.id.article_id_fill)
        dealerFill = findViewById(R.id.dealer_fill)
        priceFill = findViewById(R.id.price_fill)
        payTypeDropdown = findViewById(R.id.dropdown_pay_type)

        val titleText: TextView = findViewById(R.id.text_order_title)
        val priceLabel: TextView = findViewById(R.id.text_price)
        if (orderType.increaseStock) {
            titleText.setText(R.string.order_editor_title_purchase)
            priceLabel.setText(R.string.purchase_order_price)
            priceFill.setHint(R.string.purchase_price_hint)
            // 入库没有"支付方式"这一行
            findViewById<View>(R.id.row_pay_type).visibility = View.GONE
        } else {
            titleText.setText(R.string.order_editor_title_selling)
            // 出库不记录经销商
            findViewById<View>(R.id.text_dealer).visibility = View.GONE
            dealerFill.visibility = View.GONE
            priceLabel.setText(R.string.selling_order_price)
            priceFill.setHint(R.string.selling_price_hint)
        }

        // 支付方式下拉：支付宝 / 微信 / 现金 / 退货（不选的话默认第一项）
        payTypeDropdown.setSimpleItems(OrderType.soldTypes.map { it.displayName }.toTypedArray())
        payTypeDropdown.setText(OrderType.soldTypes.first().displayName, false)
        payTypeDropdown.setOnClickListener { payTypeDropdown.showDropDown() }

        // 从仓库页面的 ＋ / － 进来时，货号、颜色、经销商都是定死的：灰掉不给改
        val fixedArticleId = intent.getStringExtra(EXTRA_ARTICLE_ID)
        fixedColor = intent.getStringExtra(EXTRA_COLOR)
        if (fixedArticleId != null && fixedColor != null) {
            articleIdFill.setText(fixedArticleId)
            articleIdFill.isEnabled = false

            // 经销商也是这一行定死的：出库界面本来会把经销商隐藏，这里改成显示出来但灰掉
            findViewById<View>(R.id.text_dealer).visibility = View.VISIBLE
            dealerFill.visibility = View.VISIBLE
            val fixedDealer = intent.getStringExtra(EXTRA_DEALER)
            if (!fixedDealer.isNullOrEmpty()) {
                dealerFill.setText(fixedDealer)
                dealerFill.isEnabled = false
            }

            // 图片也从存量带过来：入库可以换，出库只给看
            fixedImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)

            // 颜色定死了，加行 / 删行都没有意义
            findViewById<Button>(R.id.btn_add_line).visibility = View.GONE
            titleText.text = "${titleText.text} $fixedArticleId $fixedColor"
        }

        // 表格：加号在布局里，减号在每一行里；颜色和 34~44 码的数量都在行里，一行就是一批
        orderRowAdapter = OrderRowAdapter(
            resources.getStringArray(R.array.color_array),
            { position -> orderRowAdapter.removeRow(position) },
            { position -> pickImage(position) },
            colorFixed = fixedColor != null,
            // 入库可以加图片，出库不行
            imageEditable = orderType == OrderType.ARTICLE_PURCHASE
        )
        orderRowTable = findViewById(R.id.rv_purchase_table)
        orderRowTable.layoutManager = LinearLayoutManager(this)
        orderRowTable.adapter = orderRowAdapter

        findViewById<Button>(R.id.btn_add_line).setOnClickListener { addOrderRow() }
        findViewById<Button>(R.id.btn_cancel_order).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_confirm_order).setOnClickListener { confirmOrder() }

        // 默认先放一行，打开界面就能直接填
        addOrderRow()
    }

    /** 加号：新增一行（一种颜色 = 一批）；颜色锁死时只能用锁定的那个颜色 */
    private fun addOrderRow() {
        val newPosition =
            orderRowAdapter.addRow(fixedColor ?: orderRowAdapter.firstUnusedColor(), fixedImagePath)
        if (newPosition == -1) {
            showTip(getString(R.string.rows_max_num))
            return
        }
        orderRowTable.smoothScrollToPosition(newPosition)
    }

    /** 点行里的图片方框：记下是哪一行，然后调系统相册 */
    private fun pickImage(position: Int) {
        pendingImageRowPosition = position
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    /** 确定：先校验表头和每一行，全部通过后再写库 */
    private fun confirmOrder() {
        val articleId = articleIdFill.text.toString().trim()
        // 出库界面本来不填经销商；但锁定模式（从仓库页面的 ＋ / － 进来）经销商会灰着显示出来，
        // 这种情况就一并记进订单：界面显示什么就存什么
        val dealer = dealerFill.text.toString().trim()
        val price = priceFill.text.toString().trim()

        // 入库固定是入库；出库按界面上选的支付方式（支付宝 / 微信 / 现金 / 退货）
        val orderTypeForSave = if (orderType.increaseStock) {
            OrderType.ARTICLE_PURCHASE
        } else {
            OrderType.matchDisplayName(payTypeDropdown.text.toString())
                ?: OrderType.ARTICLE_SOLD_CASH
        }

        // 先把手上的内容同步回数据，保证保存的就是屏幕上看到的
        orderRowAdapter.syncVisibleRows(orderRowTable)

        val rows = orderRowAdapter.getAllRows()
        if (rows.isEmpty()) {
            showTip(getString(R.string.order_row_empty))
            return
        }

        val orderBatches = ArrayList<OrderBatch>(rows.size)
        rows.forEachIndexed { index, row ->
            val orderBatchUtils = OrderBatchUtils(
                row.sizes.toTypedArray(),
                articleId,
                dealer,
                row.color,
                price,
                orderTypeForSave
            )
            val checkResult = orderBatchUtils.checkInputs()
            if (checkResult != StringUtils.checkOk) {
                val errorTips = if ((checkResult == StringUtils.sizeTypeError) || (checkResult == StringUtils.sizeArrayEmpty)) {
                    getString(R.string.order_row_error, index + 1, checkResult)
                } else {
                    checkResult
                }
                showTip(errorTips)
                return
            }
            orderBatches.add(orderBatchUtils.buildOrderBatch())
        }

        saveOrderBatches(orderBatches, rows)
    }

    private fun saveOrderBatches(orderBatches: List<OrderBatch>, rows: List<OrderRowData>) {
        val orderDatabase = OrderBatchDatabase.getDatabase(applicationContext)
        val inventoryService = InventoryService(InventoryDatabase.getDatabase(applicationContext))
        val imageEditable = orderType.increaseStock
        thread {
            // 1. 先查存量：出库减完小于 0 会在这里被拦下来，有问题就什么都不写
            val inventoryErrors = inventoryService.check(orderBatches)
            if (inventoryErrors.isNotEmpty()) {
                runOnUiThread { showTip(inventoryErrors.joinToString("\n")) }
                return@thread
            }

            // 2. 图片：入库时新选的图压缩后存到应用私有目录，数据库里只存路径；
            //    订单库不存图片（省空间），图片只跟着存量走
            val imagePaths = rows.map { row ->
                if (imageEditable && row.imageUri != null) {
                    ImageUtils.saveToAppStorage(applicationContext, row.imageUri!!) ?: row.imagePath
                } else {
                    row.imagePath
                }
            }

            // 3. 存量没问题：写订单流水，再加 / 减存量（入库加，出库、破损、退货减）
            orderBatches.forEach { orderDatabase.orderBatchDAO().insert(it) }
            inventoryService.apply(orderBatches, imagePaths)

            runOnUiThread {
                Toast.makeText(
                    this,
                    getString(R.string.order_saved, orderBatches.size),
                    Toast.LENGTH_SHORT
                ).show()
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun showTip(message: String) {
        AlertDialog.Builder(this)
            .setTitle(StringUtils.alertTitle)
            .setMessage(message)
            .setPositiveButton(R.string.confirm) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun readOrderType(intent: Intent): OrderType {
        val orderTypeName = intent.getStringExtra(EXTRA_ORDER_TYPE)
        return OrderType.values().firstOrNull { it.name == orderTypeName }
            ?: OrderType.ARTICLE_PURCHASE
    }

    companion object {
        private const val EXTRA_ORDER_TYPE = "extra_order_type"
        private const val EXTRA_ARTICLE_ID = "extra_article_id"
        private const val EXTRA_COLOR = "extra_color"
        private const val EXTRA_DEALER = "extra_dealer"
        private const val EXTRA_IMAGE_PATH = "extra_image_path"

        /** 货号自己填：整单入库 / 出库，用来开一个新的货号 */
        fun createIntent(context: Context, orderType: OrderType): Intent {
            return Intent(context, OrderEditActivity::class.java)
                .putExtra(EXTRA_ORDER_TYPE, orderType.name)
        }

        /** 从仓库页面的 ＋ / － 进来：货号、颜色、经销商都定死，只能填数量和（入库时）换图片 */
        fun createIntent(
            context: Context,
            orderType: OrderType,
            articleId: String,
            colorName: String,
            dealer: String,
            imagePath: String?
        ): Intent {
            return createIntent(context, orderType)
                .putExtra(EXTRA_ARTICLE_ID, articleId)
                .putExtra(EXTRA_COLOR, colorName)
                .putExtra(EXTRA_DEALER, dealer)
                .putExtra(EXTRA_IMAGE_PATH, imagePath)
        }
    }
}
