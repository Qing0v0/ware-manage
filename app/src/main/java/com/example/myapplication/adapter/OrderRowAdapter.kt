package com.example.myapplication.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.OrderRowData
import com.google.android.material.textfield.MaterialAutoCompleteTextView

/**
 * 入库 / 出库界面表格的适配器，一行对应 [R.layout.order_row]（一种颜色 + 34~44 码的数量）。
 *
 * 加号：purchase_order.xml 中的 btn_add_line，点击后调用 [addRow]
 * 减号：order_row.xml 中的 btn_delete_row，点击后回调 [onDeleteRow]，由外面删除对应行
 * 颜色：order_row.xml 中的 dropdown_row_color（Material 的 ExposedDropdownMenu，自带下拉箭头），
 *      点一下展开颜色列表；格子里的文字由代码 setText(.., false) 写入，保证一定回显
 */
class OrderRowAdapter(
    private val colorNames: Array<String>,
    private val onDeleteRow: (position: Int) -> Unit
) : RecyclerView.Adapter<OrderRowAdapter.OrderRowViewHolder>() {

    private val rows = mutableListOf<OrderRowData>()
    private val rowsMaxNum = colorNames.size

    /** 加号：末尾新增一行，返回新行的下标 */
    fun addRow(color: String): Int {
        if (rows.size >= rowsMaxNum) {
            return -1
        }
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

    /** 还没用过的颜色里的第一个，给新增的行当默认颜色（一种颜色只应该有一行） */
    fun firstUnusedColor(): String {
        val usedColors = rows.map { it.color }
        return colorNames.firstOrNull { it !in usedColors } ?: colorNames[0]
    }

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

        private val colorDropdown: MaterialAutoCompleteTextView =
            itemView.findViewById(R.id.dropdown_row_color)
        private val deleteButton: Button = itemView.findViewById(R.id.btn_delete_row)
        private val sizeEdits: List<EditText> = SIZE_EDIT_IDS.map { itemView.findViewById(it) }

        /** 当前这一行绑定的数据：输入框内容直接写回这里，列表滚动复用 ViewHolder 也不会丢数据 */
        private var rowData: OrderRowData? = null

        init {
            // 点颜色格子（或右边的下拉箭头）展开颜色列表，选中后立刻写进数据并显示出来
            colorDropdown.setSimpleItems(colorNames)
            colorDropdown.setOnClickListener { colorDropdown.showDropDown() }
            colorDropdown.setOnItemClickListener { _, _, position, _ ->
                val data = rowData ?: return@setOnItemClickListener
                data.color = colorNames[position]
                showColor(data.color)
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

            // 颜色由代码直接写进格子里，不会出现选完不回显、或者整格空白的情况
            if (data.color !in colorNames) {
                data.color = colorNames[0]
            }
            showColor(data.color)
            // 复用这一行的时候，把上一次可能还开着的下拉列表收起来
            colorDropdown.dismissDropDown()

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

            val colorName = colorDropdown.text.toString()
            if (colorName in colorNames) {
                data.color = colorName
            }
            sizeEdits.forEachIndexed { index, editText ->
                data.sizes[index] = editText.text.toString()
            }
        }

        /**
         * 把颜色写进格子。第二个参数 false 表示不要触发 AutoCompleteTextView 的过滤，
         * 免得只是显示一下就又把下拉列表弹出来。
         */
        private fun showColor(colorName: String) {
            colorDropdown.setText(colorName, false)
        }
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
