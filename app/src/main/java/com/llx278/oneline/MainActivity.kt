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
        val pointInfo = arrayListOf(PointInfo(Point(0,0),pointColor,40f,null),
                PointInfo(Point(0,3),pointColor,40f,null),
                PointInfo(Point(0,6),pointColor,40f,null),
                PointInfo(Point(4,3),pointColor,40f,null))

        val lineInfo = arrayListOf(LineInfo(Point(0,0), Point(0,3),lineColor,40f),
                LineInfo(Point(0,3),Point(0,6),lineColor,40f),
                LineInfo(Point(0,6),Point(4,3),lineColor,40f))
        val drawInfo = DrawInfo(pointInfo = pointInfo,lineInfo = lineInfo)

        one_line.setDrawInfo(drawInfo)
    }
}
