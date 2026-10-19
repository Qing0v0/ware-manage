package com.example.myapplication.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.myapplication.R
import com.example.myapplication.ui.screens.main.MainScreen


class MainActivity : AppCompatActivity() {
    private lateinit var wareFragment: WareFragment
    private lateinit var billsFragment: BillsFragment

    /** 当前显示的是哪一页，Activity 重建后用它复原（和底部导航的选中项保持一致） */
    private var currentTab by mutableIntStateOf(TAB_WAREHOUSE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        wareFragment = supportFragmentManager.findFragmentByTag(TAG_WAREHOUSE) as? WareFragment ?: WareFragment()
        billsFragment = supportFragmentManager.findFragmentByTag(TAG_BILLS) as? BillsFragment ?: BillsFragment()
        removeStaleFragments()

        currentTab = savedInstanceState?.getInt(KEY_TAB, TAB_WAREHOUSE) ?: TAB_WAREHOUSE
        setContent {
            MainScreen(
                selectedItemId = if (currentTab == TAB_BILLS) R.id.bills else R.id.warehouse,
                onItemSelected = { itemId ->
                    when (itemId) {
                        R.id.warehouse -> selectFragment(TAB_WAREHOUSE)
                        R.id.bills -> selectFragment(TAB_BILLS)
                    }
                },
                onSettingsClick = {
                    if (currentTab != TAB_WAREHOUSE) {
                        selectFragment(TAB_WAREHOUSE)
                    }
                    wareFragment.openMoreMenu()
                },
                onFragmentContainerReady = {
                    if (!isDestroyed) {
                        selectFragment(currentTab)
                    }
                }
            )
        }
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
