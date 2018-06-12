package com.llx278.oneline

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pointColor = Color.parseColor("#458B74")
        //val pointColor = Color.RED
        val lineColor = Color.parseColor("#BDBDBD")
        //val lineColor = Color.BLUE
        val pointInfo = arrayListOf(
                PointInfo(Point(3,0),pointColor,40f,null),
                PointInfo(Point(6,0),pointColor,40f,null),
                PointInfo(Point(6,3),pointColor,40f,null),
                PointInfo(Point(3,3),pointColor,40f,null),
                PointInfo(Point(3,0),pointColor,40f,null),
                PointInfo(Point(0,0),pointColor,40f,null),
                PointInfo(Point(0,6),pointColor,40f,null),
                PointInfo(Point(6,3),pointColor,40f,null),
                PointInfo(Point(6,6),pointColor,40f,null),
                PointInfo(Point(0,6),pointColor,40f,null)
        )

        val linesInfo = LinesInfo(pointInfos = pointInfo,color = lineColor,strokeWidth = 40f)
        one_line.setLineInfo(linesInfo)
        reset.setOnClickListener {
            one_line.pop()
        }
    }
}
