package com.example.myapplication.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.ui.screens.bill.BillsScreen

/**
 * 账单页的宿主 Fragment：界面和查询逻辑都在 [BillsScreen]（Compose）里，
 * 这里负责「页面复位」信号——每次切到账单页时通知 Compose 侧清空上次查询结果，
 * 以及右上角「更多」菜单的触发信号与备份 / 更新操作（[WareDrawerPanel]）。
 *
 * （MainActivity 目前靠 show/hide 切页，见 [MainActivity]；等页面全部 Compose 化后再收掉这些壳。）
 */
class BillsFragment : Fragment() {

    /**
     * 复位信号：每次 resetPage() 加 1。
     * BillsScreen 里用 LaunchedEffect 盯着它，一变就把日期、筛选条件、订单表和总计
     * 恢复默认，对应原来 resetPage() 里清空各个 View 的那套逻辑。
     */
    private val resetTick = mutableIntStateOf(0)

    /**
     * 右上角「更多」菜单的触发信号：每次 openMoreMenu() 加 1。
     * BillsScreen 里用 LaunchedEffect 盯着它，一变就弹出更多菜单。
     */
    private val moreMenuRequest = mutableIntStateOf(0)

    /**
     * 备份恢复、版本更新的系统回调。
     * 必须在这里就 new 出来：里面的 registerForActivityResult 要求在 Fragment
     * 初始化阶段注册，放到 onCreateView 里就晚了。
     */
    private val drawerPanel = WareDrawerPanel(this) { resetPage() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            id = R.id.bills_compose_view
            setContent {
                BillsScreen(
                    resetTick = resetTick.intValue,
                    moreMenuRequest = moreMenuRequest.intValue,
                    onBackupClick = drawerPanel::showBackupDialog,
                    onUpdateClick = drawerPanel::showUpdateDialog,
                    dialog = drawerPanel.dialogState.value,
                    onDialogDismiss = drawerPanel::dismissDialog
                )
            }
        }
    }

    /**
     * 页面复位：日期回到默认范围、清空筛选条件、订单表、总计归零。
     * 每次切到账单页（或再点一次账单 tab）都会调一次，免得看到上一次查询留下的旧结果。
     */
    fun resetPage() {
        resetTick.intValue++
    }

    /** 右上角「更多」菜单：在当前页面弹出，不切页 */
    fun openMoreMenu() {
        moreMenuRequest.intValue++
    }

    /** 从别的页面切回来时把页面复位 */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            resetPage()
        }
    }
}
