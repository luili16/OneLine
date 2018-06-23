package com.llx278.oneline

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.llx278.oneline.widget.LinesInfo
import com.llx278.oneline.widget.Point
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
       /* val pointInfo = arrayListOf(
                Point(3, 0),
                Point(6, 0),
                Point(6, 3),
                Point(3, 3),
                Point(3, 0),
                Point(0, 0),
                Point(0, 6),
                Point(6, 3),
                Point(6, 6),
                Point(0, 6)
        )

        val linesInfo = LinesInfo(points = pointInfo)
        one_line.setLineInfo(linesInfo)
        reset.setOnClickListener {
            one_line.pop()
        }

        one_line.finishCallback =  {
            Log.d("main","绘制完成 回调 lamda")
        }*/

    }
}
