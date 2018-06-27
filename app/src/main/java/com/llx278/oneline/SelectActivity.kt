package com.llx278.oneline

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_select.*

class SelectActivity : AppCompatActivity() {

    lateinit var fragments : Array<Fragment>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)

        fragments = arrayOf(
                BasicFragment.getInstance(),
                AdvanceFragment.getInstance(),
                TestFragment.getInstance()
        )

        val myAdapter = MyPageAdapter()
        view_pager_container.adapter = myAdapter
        pager_indicator.setViewPager(view_pager_container)
    }

    private inner class MyPageAdapter : FragmentPagerAdapter(supportFragmentManager) {

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return 3
        }
    }
}