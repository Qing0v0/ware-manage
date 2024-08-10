package com.example.myapplication.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.myapplication.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView


class MainActivity : AppCompatActivity() {
    private lateinit var navigationView: BottomNavigationView
    private var wareFragment: WareFragment = WareFragment()
    private var profitFragment: ProfitFragment = ProfitFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        navigationView = findViewById(R.id.bottom_navigation)
        navigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.warehouse -> selectFragment(0)
                R.id.profit -> selectFragment(1)
            }
            true
        }

        // 初始化为仓库界面
        selectFragment(0)
    }

    private fun selectFragment(fragmentId: Int) {
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        when (fragmentId) {
            0 -> {
                fragmentTransaction.hide(profitFragment)
                if (!wareFragment.isAdded) {
                    fragmentTransaction.add(R.id.content, wareFragment)
                }
                fragmentTransaction.show(wareFragment)
            }

            1 -> {
                fragmentTransaction.hide(wareFragment)
                if (!profitFragment.isAdded) {
                    fragmentTransaction.add(R.id.content, profitFragment)
                }
                fragmentTransaction.show(profitFragment)
            }
        }

        fragmentTransaction.commit()
    }
}