package com.example.myapplication.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.myapplication.R
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {
    private lateinit var navigationView: BottomNavigationView
    private lateinit var wareFragment: WareFragment
    private lateinit var billsFragment: BillsFragment

    /** 当前显示的是哪一页，Activity 重建后用它复原（和底部导航的选中项保持一致） */
    private var currentTab = TAB_WAREHOUSE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        wareFragment = supportFragmentManager.findFragmentByTag(TAG_WAREHOUSE) as? WareFragment ?: WareFragment()
        billsFragment = supportFragmentManager.findFragmentByTag(TAG_BILLS) as? BillsFragment ?: BillsFragment()
        removeStaleFragments()

        navigationView = findViewById(R.id.bottom_navigation)
        navigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.warehouse -> selectFragment(TAB_WAREHOUSE)
                R.id.bills -> selectFragment(TAB_BILLS)
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        // 重建之后接着显示上次那一页，第一次进来默认仓库页
        selectFragment(savedInstanceState?.getInt(KEY_TAB, TAB_WAREHOUSE) ?: TAB_WAREHOUSE)
        // 底部导航的高亮：系统有时会恢复、有时不恢复，这里显式对齐一次，保证高亮和显示的页面是同一页
        navigationView.menu.findItem(
            if (currentTab == TAB_BILLS) R.id.bills else R.id.warehouse
        )?.isChecked = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAB, currentTab)
    }

    /**
     * 切页：hide 掉另一页、show 出这一页，容器里始终只有这两页，不会叠。
     */
    private fun selectFragment(tab: Int) {
        currentTab = tab
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        if (tab == TAB_BILLS) {
            fragmentTransaction.hide(wareFragment)
            if (!billsFragment.isAdded) {
                fragmentTransaction.add(R.id.content, billsFragment, TAG_BILLS)
            }
            fragmentTransaction.show(billsFragment)
        } else {
            fragmentTransaction.hide(billsFragment)
            if (!wareFragment.isAdded) {
                fragmentTransaction.add(R.id.content, wareFragment, TAG_WAREHOUSE)
            }
            fragmentTransaction.show(wareFragment)
        }
        fragmentTransaction.commitNow()

        if (tab == TAB_BILLS) {
            billsFragment.resetPage()
        }
    }

    /**
     * 把没被认领的页面 remove 掉。
     */
    private fun removeStaleFragments() {
        val stale: List<Fragment> = supportFragmentManager.fragments.filter {
            it !== wareFragment && it !== billsFragment
        }
        if (stale.isEmpty()) {
            return
        }
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        stale.forEach { fragmentTransaction.remove(it) }
        fragmentTransaction.commitNow()
    }

    companion object {
        private const val TAG_WAREHOUSE = "warehouse_page"
        private const val TAG_BILLS = "bills_page"

        /** 保存 / 恢复当前显示的是哪一页 */
        private const val KEY_TAB = "current_tab"
        private const val TAB_WAREHOUSE = 0
        private const val TAB_BILLS = 1
    }
}

