package com.example.myapplication.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.model.OrderType

/**
 * 仓库页面：目前只有入库和出库两个入口，存量列表后面再加。
 */
class WareFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ware, container, false)

        // 入库
        view.findViewById<Button>(R.id.purchase_button).setOnClickListener {
            startActivity(OrderEditActivity.createIntent(requireContext(), OrderType.ARTICLE_PURCHASE))
        }

        // 出库
        view.findViewById<Button>(R.id.selling_button).setOnClickListener {
            startActivity(OrderEditActivity.createIntent(requireContext(), OrderType.ARTICLE_SOLD))
        }

        return view
    }
}
