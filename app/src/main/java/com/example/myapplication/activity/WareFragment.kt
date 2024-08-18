package com.example.myapplication.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.example.myapplication.R
import com.example.myapplication.model.Color
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.OrderType
import com.example.myapplication.utils.EnumUtils
import java.math.BigDecimal
import java.util.Date


class WareFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_ware, container, false)

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
            builder.setPositiveButton("确定") { dialog, which ->
                val articleIdText: String = editTextArticleId.text.toString()
                val articleNameText: String = editTextArticleName.text.toString()
                val color: String = spinnerColor.selectedItem.toString()
                val dealerText: String = editTextDealer.text.toString()
                val price: String = editTextPrice.text.toString()
                val size35: Int= size35Fill.text.toString().toInt()
                val size36: Int = size36Fill.text.toString().toInt()
                val size37: Int = size37Fill.text.toString().toInt()
                val size38: Int = size38Fill.text.toString().toInt()
                val size39: Int = size39Fill.text.toString().toInt()
                val size40: Int = size40Fill.text.toString().toInt()
                val size41: Int = size41Fill.text.toString().toInt()
                val size42: Int = size42Fill.text.toString().toInt()
                val size43: Int = size43Fill.text.toString().toInt()

                val orderBatch: OrderBatch = OrderBatch(
                    orderId = null,
                    articleId = articleIdText,
                    articleName = articleNameText,
                    color = EnumUtils.matchColor(color),
                    size35 = size35, size36 = size36, size37 = size37,
                    size38 = size38, size39 = size39, size40 = size40,
                    size41 = size41, size42 = size42, size43 = size43,
                    orderType = OrderType.ARTICLE_PURCHASE,
                    dealer = dealerText,
                    price = BigDecimal(price),
                    date = Date()
                )
                purchaseEvent()
            }
            builder.setNegativeButton("取消") { dialog, which ->
                dialog.dismiss()
            }

            builder.show()
        }

        // 出库点击
        sellingButton.setOnClickListener {
            sellingEvent()
        }

        return view
    }

    private fun purchaseEvent() {
    }

    private fun sellingEvent() {

    }
}