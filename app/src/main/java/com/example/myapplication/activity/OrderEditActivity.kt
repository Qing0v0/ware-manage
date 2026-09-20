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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.OrderRowAdapter
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderType
import com.example.myapplication.service.OrderBatchDatabase
import com.example.myapplication.utils.OrderBatchUtils
import com.example.myapplication.utils.StringUtils

import kotlin.concurrent.thread

/**
 * 入库 / 出库界面（布局：R.layout.purchase_order）。
 *
 * - 入库：OrderType.ARTICLE_PURCHASE，填写货号、货名、经销商、进价
 * - 出库：OrderType.ARTICLE_SOLD，不需要经销商，价格改成售价
 *
 * 表头是整张单据的公共信息，颜色不在这里选；表格里每一行 = 一种颜色 + 34~44 码的数量（即一批），
 * 点确定时每一行都会生成一条 [OrderBatch]。
 */
class OrderEditActivity : AppCompatActivity() {

    private lateinit var orderType: OrderType

    private lateinit var articleIdFill: EditText
    private lateinit var articleNameFill: EditText
    private lateinit var dealerFill: EditText
    private lateinit var priceFill: EditText

    private lateinit var orderRowTable: RecyclerView
    private lateinit var orderRowAdapter: OrderRowAdapter

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
        articleNameFill = findViewById(R.id.article_name_fill)
        dealerFill = findViewById(R.id.dealer_fill)
        priceFill = findViewById(R.id.price_fill)

        val titleText: TextView = findViewById(R.id.text_order_title)
        val priceLabel: TextView = findViewById(R.id.text_price)
        if (orderType == OrderType.ARTICLE_PURCHASE) {
            titleText.setText(R.string.order_editor_title_purchase)
            priceLabel.setText(R.string.purchase_order_price)
            priceFill.setHint(R.string.purchase_price_hint)
        } else {
            titleText.setText(R.string.order_editor_title_selling)
            // 出库不记录经销商
            findViewById<View>(R.id.text_dealer).visibility = View.GONE
            dealerFill.visibility = View.GONE
            priceLabel.setText(R.string.selling_order_price)
            priceFill.setHint(R.string.selling_price_hint)
        }

        // 表格：加号在布局里，减号在每一行里；颜色和 34~44 码的数量都在行里，一行就是一批
        orderRowAdapter = OrderRowAdapter(resources.getStringArray(R.array.color_array)) { position ->
            orderRowAdapter.removeRow(position)
        }
        orderRowTable = findViewById(R.id.rv_purchase_table)
        orderRowTable.layoutManager = LinearLayoutManager(this)
        orderRowTable.adapter = orderRowAdapter

        findViewById<Button>(R.id.btn_add_line).setOnClickListener { addOrderRow() }
        findViewById<Button>(R.id.btn_cancel_order).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_confirm_order).setOnClickListener { confirmOrder() }

        // 默认先放一行，打开界面就能直接填
        addOrderRow()
    }

    /** 加号：新增一行（一种颜色 = 一批），默认用还没用过的颜色，进界面后点颜色格子可以改 */
    private fun addOrderRow() {
        val newPosition = orderRowAdapter.addRow(orderRowAdapter.firstUnusedColor())
        if (newPosition == -1) {
            showTip(getString(R.string.rows_max_num))
            return
        }
        orderRowTable.smoothScrollToPosition(newPosition)
    }

    /** 确定：先校验表头和每一行，全部通过后再写库 */
    private fun confirmOrder() {
        val articleId = articleIdFill.text.toString().trim()
        val articleName = articleNameFill.text.toString().trim()
        val dealer = if (orderType == OrderType.ARTICLE_PURCHASE) {
            dealerFill.text.toString().trim()
        } else {
            ""
        }
        val price = priceFill.text.toString().trim()

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
                articleName,
                dealer,
                row.color,
                price,
                orderType
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

        saveOrderBatches(orderBatches)
    }

    private fun saveOrderBatches(orderBatches: List<OrderBatch>) {
        val database = OrderBatchDatabase.getDatabase(applicationContext)
        thread {
            orderBatches.forEach { database.orderBatchDAO().insert(it) }
            // TODO 下一步：这里要同步增加（入库）或减少（出库）ShoeInventory 里的存量
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

        fun createIntent(context: Context, orderType: OrderType): Intent {
            return Intent(context, OrderEditActivity::class.java)
                .putExtra(EXTRA_ORDER_TYPE, orderType.name)
        }
    }
}
