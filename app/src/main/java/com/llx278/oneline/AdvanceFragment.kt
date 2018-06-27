package com.llx278.oneline

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class AdvanceFragment :Fragment() {

    companion object {
        fun getInstance() : AdvanceFragment {
            return AdvanceFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_advance,container,false)
    }
}