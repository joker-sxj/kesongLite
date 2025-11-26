package com.example.kesonglite.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.kesonglite.R
import com.example.kesonglite.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 默认加载 CommunityFragment
        if (savedInstanceState == null) {
            replaceFragment(CommunityFragment())
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // 首页 Tab 默认位于“社区”，这里简化为直接加载 CommunityFragment
                    replaceFragment(CommunityFragment())
                    true
                }
                R.id.navigation_me -> {
                    // “我”页面，简化为一个空白 Fragment
                    replaceFragment(MeFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
