package com.example.myapplication.activity

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.ui.screens.main.MainScreen
import com.example.myapplication.ui.screens.main.MainScreenSection


class MainActivity : AppCompatActivity() {
    private lateinit var wareFragment: WareFragment
    private lateinit var billsFragment: BillsFragment
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val (root, header, content, navigation) = createMainLayout()
        setContentView(root)
        applySystemBarInsets(root)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        savedInstanceState?.let {
            mainViewModel.selectTab(it.getInt(KEY_TAB, TAB_WAREHOUSE))
        }
        wareFragment = supportFragmentManager.findFragmentByTag(TAG_WAREHOUSE) as? WareFragment ?: WareFragment()
        billsFragment = supportFragmentManager.findFragmentByTag(TAG_BILLS) as? BillsFragment ?: BillsFragment()
        removeStaleFragments()

        header.setContent {
            MainScreen(
                MainScreenSection.Header(
                    onSettingsClick = {
                        if (mainViewModel.selectedTab != TAB_WAREHOUSE) {
                            selectFragment(TAB_WAREHOUSE)
                        }
                        wareFragment.openMoreMenu()
                    }
                )
            )
        }
        navigation.setContent {
            MainScreen(
                MainScreenSection.BottomNavigation(
                    selectedItemId =
                        if (mainViewModel.selectedTab == TAB_BILLS) R.id.bills else R.id.warehouse,
                    onItemSelected = { itemId ->
                        when (itemId) {
                            R.id.warehouse -> selectFragment(TAB_WAREHOUSE)
                            R.id.bills -> selectFragment(TAB_BILLS)
                        }
                    }
                )
            )
        }

        content.post {
            if (!isDestroyed && !supportFragmentManager.isStateSaved) {
                selectFragment(mainViewModel.selectedTab, resetBills = false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAB, mainViewModel.selectedTab)
    }

    /**
     * 切页：hide 掉另一页、show 出这一页，容器里始终只有这两页，不会叠。
     */
    private fun selectFragment(tab: Int, resetBills: Boolean = true) {
        mainViewModel.selectTab(tab)
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

        if (tab == TAB_BILLS && resetBills) {
            billsFragment.resetPage()
        }
    }

    private fun applySystemBarInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            target.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    private fun createMainLayout(): MainLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F7F5.toInt())
        }
        val header = ComposeView(this).apply {
            id = R.id.main_header
        }
        val content = FragmentContainerView(this).apply {
            id = R.id.content
        }
        val navigation = ComposeView(this).apply {
            id = R.id.main_navigation
        }
        val navigationHeight = (BOTTOM_NAVIGATION_HEIGHT_DP * resources.displayMetrics.density).toInt()

        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            content,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        root.addView(
            navigation,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                navigationHeight
            )
        )
        return MainLayout(root, header, content, navigation)
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
        private const val BOTTOM_NAVIGATION_HEIGHT_DP = 70
    }

    private data class MainLayout(
        val root: LinearLayout,
        val header: ComposeView,
        val content: FragmentContainerView,
        val navigation: ComposeView
    )
}

class MainViewModel : ViewModel() {
    var selectedTab by mutableIntStateOf(0)
        private set

    fun selectTab(tab: Int) {
        selectedTab = tab
    }
}
