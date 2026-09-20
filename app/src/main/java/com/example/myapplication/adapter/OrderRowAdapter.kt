package com.example.myapplication.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.OrderRowData

/**
 * 入库 / 出库界面表格的适配器，一行对应 [R.layout.order_row]（一种颜色 + 34~44 码的数量）。
 *
 * 加号：purchase_order.xml 中的 btn_add_line，点击后调用 [addRow]
 * 减号：order_row.xml 中的 btn_delete_row，点击后回调 [onDeleteRow]，由外面删除对应行
 */
class OrderRowAdapter(
    private val colorNames: Array<String>,
    private val onDeleteRow: (position: Int) -> Unit
) : RecyclerView.Adapter<OrderRowAdapter.OrderRowViewHolder>() {

    private val rows = mutableListOf<OrderRowData>()
    private val rowsMaxNum = 10

    /** 加号：末尾新增一行，返回新行的下标 */
    fun addRow(color: String): Int {
        rows.add(OrderRowData(color))
        val position = rows.size - 1
        notifyItemInserted(position)
        return position
    }

    /** 减号：删除某一行，返回删除后剩余的行数 */
    fun removeRow(position: Int): Int {
        if (position !in rows.indices) {
            return rows.size
        }
        rows.removeAt(position)
        notifyItemRemoved(position)
        if (position < rows.size) {
            // 被删行后面的每一行都往前挪了一格，通知 RecyclerView 重新绑定
            notifyItemRangeChanged(position, rows.size - position)
        }
        return rows.size
    }

    /** 当前所有行的数据，提交时使用 */
    fun getAllRows(): List<OrderRowData> = rows.toList()

    /**
     * 把当前显示在屏幕上的行的控件内容同步回数据，点“确定”前调用一次。
     */
    fun syncVisibleRows(recyclerView: RecyclerView) {
        for (index in 0 until recyclerView.childCount) {
            val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(index))
            if (viewHolder is OrderRowViewHolder) {
                viewHolder.syncFromViews()
            }
        }
    }

    override fun getItemCount(): Int = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderRowViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.order_row, parent, false)
        return OrderRowViewHolder(itemView, colorNames, onDeleteRow)
    }

    override fun onBindViewHolder(holder: OrderRowViewHolder, position: Int) {
        holder.bind(rows[position])
    }

    class OrderRowViewHolder(
        itemView: View,
        private val colorNames: Array<String>,
        private val onDeleteRow: (position: Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val colorSpinner: Spinner = itemView.findViewById(R.id.sp_color)
        private val deleteButton: Button = itemView.findViewById(R.id.btn_delete_row)
        private val sizeEdits: List<EditText> = SIZE_EDIT_IDS.map { itemView.findViewById(it) }

        /** 当前这一行绑定的数据：输入框内容直接写回这里，列表滚动复用 ViewHolder 也不会丢数据 */
        private var rowData: OrderRowData? = null

        init {
            // 下拉框显示的是哪个颜色，这一行就存哪个颜色。
            // 这里故意不用“正在绑定”之类的标志位去拦截回调：onItemSelected 报出来的
            // 永远是下拉框当前真正选中的项，直接写回数据，显示和数据就不会打架。
            colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val data = rowData ?: return
                    val colorName = colorNameAt(position) ?: return
                    if (data.color != colorName) {
                        data.color = colorName
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

            sizeEdits.forEachIndexed { index, editText ->
                editText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) = Unit

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) = Unit

                    override fun afterTextChanged(s: Editable?) {
                        rowData?.sizes?.set(index, s?.toString() ?: "")
                    }
                })
            }

            // 减号：把当前行在列表里的位置回调出去
            deleteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteRow(position)
                }
            }
        }

        fun bind(data: OrderRowData) {
            rowData = data

            // 只有下拉框当前显示的颜色与一行不一致时这才改，避免多余的 setSelection
            val colorPosition = colorNames.indexOf(data.color)
            if (colorPosition >= 0 && colorSpinner.selectedItemPosition != colorPosition) {
                colorSpinner.setSelection(colorPosition)
            }
            sizeEdits.forEachIndexed { index, editText ->
                editText.setText(data.sizes[index])
            }
        }

        /**
         * 把界面上当前的内容同步回数据（点“确定”前调用）。
         * 万一某个控件的回调没触发，也能保证保存的就是用户看到的内容。
         */
        fun syncFromViews() {
            val data = rowData ?: return

            val colorName = colorNameAt(colorSpinner.selectedItemPosition)
            if (colorName != null && data.color != colorName) {
                data.color = colorName
            }
            sizeEdits.forEachIndexed { index, editText ->
                data.sizes[index] = editText.text.toString()
            }
        }

        private fun colorNameAt(position: Int): String? =
            if (position in colorNames.indices) colorNames[position] else null
    }

    companion object {
        /** 顺序必须与 order_row.xml 中的输入框顺序、以及 OrderBatch.size34 ~ size44 保持一致 */
        private val SIZE_EDIT_IDS = intArrayOf(
            R.id.size34_fill, R.id.size35_fill, R.id.size36_fill,
            R.id.size37_fill, R.id.size38_fill, R.id.size39_fill,
            R.id.size40_fill, R.id.size41_fill, R.id.size42_fill,
            R.id.size43_fill, R.id.size44_fill
        )
    }
}
