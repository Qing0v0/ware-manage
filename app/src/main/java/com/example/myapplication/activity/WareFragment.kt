package com.example.myapplication.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderType
import com.example.myapplication.service.OrderBatchDatabase
import com.example.myapplication.utils.OrderBatchUtils
import com.example.myapplication.utils.StringUtils
import kotlinx.coroutines.launch
import kotlin.concurrent.thread


class WareFragment : Fragment() {
    private lateinit var db: OrderBatchDatabase

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_ware, container, false)
        db = OrderBatchDatabase.getDatabase(requireContext())

        val purchaseButton: Button = view.findViewById(R.id.purchase_button)
        val sellingButton: Button = view.findViewById(R.id.selling_button)

        // 入库点击
        purchaseButton.setOnClickListener {
            val dialogView = inflater.inflate(R.layout.purchase_order, null)
            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())

            val editTextArticleId: EditText = dialogView.findViewById<EditText>(R.id.article_id_fill)
            val editTextArticleName: EditText = dialogView.findViewById<EditText>(R.id.article_name_fill)
            val spinnerColor: Spinner = dialogView.findViewById<Spinner>(R.id.spinner_color)
            val editTextDealer: EditText = dialogView.findViewById<EditText>(R.id.dealer_fill)
            val editTextPrice: EditText = dialogView.findViewById<EditText>(R.id.price_fill)
            val size35Fill: EditText = dialogView.findViewById<EditText>(R.id.size35_fill)
            val size36Fill: EditText = dialogView.findViewById<EditText>(R.id.size36_fill)
            val size37Fill: EditText = dialogView.findViewById<EditText>(R.id.size37_fill)
            val size38Fill: EditText = dialogView.findViewById<EditText>(R.id.size38_fill)
            val size39Fill: EditText = dialogView.findViewById<EditText>(R.id.size39_fill)
            val size40Fill: EditText = dialogView.findViewById<EditText>(R.id.size40_fill)
            val size41Fill: EditText = dialogView.findViewById<EditText>(R.id.size41_fill)
            val size42Fill: EditText = dialogView.findViewById<EditText>(R.id.size42_fill)
            val size43Fill: EditText = dialogView.findViewById<EditText>(R.id.size43_fill)

            builder.setView(dialogView)
            builder.setPositiveButton("确定") { dialog, _ ->
                val articleIdText: String = editTextArticleId.text.toString()
                val articleNameText: String = editTextArticleName.text.toString()
                val color: String = spinnerColor.selectedItem.toString()
                val dealerText: String = editTextDealer.text.toString()
                val price: String = editTextPrice.text.toString()
                val size35: String = size35Fill.text.toString()
                val size36: String = size36Fill.text.toString()
                val size37: String = size37Fill.text.toString()
                val size38: String = size38Fill.text.toString()
                val size39: String = size39Fill.text.toString()
                val size40: String = size40Fill.text.toString()
                val size41: String = size41Fill.text.toString()
                val size42: String = size42Fill.text.toString()
                val size43: String = size43Fill.text.toString()

                val orderBatchUtils = OrderBatchUtils(
                    arrayOf(size35, size36, size37, size38, size39, size40, size41, size42, size43),
                    articleIdText,
                    articleNameText,
                    dealerText,
                    color,
                    price,
                    OrderType.ARTICLE_PURCHASE
                )

                val checkResult: String = orderBatchUtils.checkInputs()
                if (checkResult != StringUtils.checkOk) {
                    val inputErrorAlertBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    inputErrorAlertBuilder.setTitle(StringUtils.alertTitle)
                    inputErrorAlertBuilder.setMessage(checkResult)
                    inputErrorAlertBuilder.setPositiveButton("确定") { _, _ ->
                        purchaseButton.performClick()
                    }
                    inputErrorAlertBuilder.show()
                } else {
                    val orderBatch: OrderBatch = orderBatchUtils.generateOrderBatch()
                    thread {
                        val id = db.orderBatchDAO().insert(orderBatch)
                        val list: List<OrderBatch> = db.orderBatchDAO().query()
                        println(list.size)
                    }
//                    purchaseEvent(orderBatch)
                }
            }
            builder.setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss();
            }

            builder.show()
        }

        // 出库点击
        sellingButton.setOnClickListener {
            val dialogView = inflater.inflate(R.layout.purchase_order, null)
            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            val priceText: TextView = dialogView.findViewById(R.id.text_price)
            priceText.text = "售价"
            val dealerText: TextView = dialogView.findViewById(R.id.text_dealer)
            dealerText.visibility = View.GONE
            val dealerEditText: EditText = dialogView.findViewById(R.id.dealer_fill);
            dealerEditText.visibility = View.GONE

            val editTextArticleId: EditText = dialogView.findViewById<EditText>(R.id.article_id_fill)
            val editTextArticleName: EditText = dialogView.findViewById<EditText>(R.id.article_name_fill)
            val spinnerColor: Spinner = dialogView.findViewById<Spinner>(R.id.spinner_color)
            val editTextPrice: EditText = dialogView.findViewById<EditText>(R.id.price_fill)
            val size35Fill: EditText = dialogView.findViewById<EditText>(R.id.size35_fill)
            val size36Fill: EditText = dialogView.findViewById<EditText>(R.id.size36_fill)
            val size37Fill: EditText = dialogView.findViewById<EditText>(R.id.size37_fill)
            val size38Fill: EditText = dialogView.findViewById<EditText>(R.id.size38_fill)
            val size39Fill: EditText = dialogView.findViewById<EditText>(R.id.size39_fill)
            val size40Fill: EditText = dialogView.findViewById<EditText>(R.id.size40_fill)
            val size41Fill: EditText = dialogView.findViewById<EditText>(R.id.size41_fill)
            val size42Fill: EditText = dialogView.findViewById<EditText>(R.id.size42_fill)
            val size43Fill: EditText = dialogView.findViewById<EditText>(R.id.size43_fill)

            builder.setView(dialogView)
            builder.setPositiveButton("确定") { dialog, _ ->
                val articleIdText: String = editTextArticleId.text.toString()
                val articleNameText: String = editTextArticleName.text.toString()
                val color: String = spinnerColor.selectedItem.toString()
                val price: String = editTextPrice.text.toString()
                val size35: String = size35Fill.text.toString()
                val size36: String = size36Fill.text.toString()
                val size37: String = size37Fill.text.toString()
                val size38: String = size38Fill.text.toString()
                val size39: String = size39Fill.text.toString()
                val size40: String = size40Fill.text.toString()
                val size41: String = size41Fill.text.toString()
                val size42: String = size42Fill.text.toString()
                val size43: String = size43Fill.text.toString()

                val orderBatchUtils = OrderBatchUtils(
                    arrayOf(size35, size36, size37, size38, size39, size40, size41, size42, size43),
                    articleIdText,
                    articleNameText,
                    "",
                    color,
                    price,
                    OrderType.ARTICLE_PURCHASE
                )

                val checkResult = orderBatchUtils.checkInputs()
                if (checkResult != StringUtils.checkOk) {
                    val inputErrorAlertBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    inputErrorAlertBuilder.setTitle(StringUtils.alertTitle)
                    inputErrorAlertBuilder.setMessage(checkResult)
                    inputErrorAlertBuilder.setPositiveButton("确定") { _, _ ->
                        purchaseButton.performClick()
                    }
                    inputErrorAlertBuilder.show()
                } else {
                    val orderBatch: OrderBatch = orderBatchUtils.generateOrderBatch()
                    sellingEvent(orderBatch)
                }
            }

            builder.setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss();
            }

            builder.show()
        }

        return view
    }

    private fun purchaseEvent(orderBatch: OrderBatch) {

    }

    private fun sellingEvent(orderBatch: OrderBatch) {

    }
}