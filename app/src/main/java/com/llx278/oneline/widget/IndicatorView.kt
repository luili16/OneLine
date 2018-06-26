package com.llx278.oneline.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.llx278.oneline.R

class IndicatorView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val processColor: Int

    private val pointColor: Int

    private var itemCount: Int = 3

    private var pointPositions: ArrayList<PointF> = ArrayList()

    private val bgPaint : Paint = Paint()

    private val circlePaint : Paint = Paint()

    private val viewPort:Rect = Rect()

    init {
        val a = context.theme.obtainStyledAttributes(attrs!!,
                R.styleable.IndicatorView, 0, 0)
        try {
            processColor = a.getColor(R.styleable.IndicatorView_process_color, Color.BLACK)
            pointColor = a.getColor(R.styleable.IndicatorView_point_color, Color.BLUE)
        } finally {
            a.recycle()
        }

        bgPaint.color = Color.parseColor("#A9A9A9")
        bgPaint.style = Paint.Style.FILL

        circlePaint.color = Color.BLACK
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeWidth = 2f
        //bgPaint.strokeWidth = 2f
    }

    fun setItemCount(count: Int) {
        itemCount = count
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        viewPort.set(left,top,right,bottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (h * itemCount >= w) {
            Log.e("OneLine", "IndicatorView h * itemCount = ${h * itemCount} > w = $w!")
        }

        val innerSpace = (w - h * itemCount).toFloat() / (itemCount - 1).toFloat()
        val r = h.toFloat() / 2f
        var inc : Float = 0f
        for (i in 0..itemCount) {
            val point = PointF()
            inc += i * h + r + innerSpace
            point.x = inc
            point.y = r
            pointPositions.add(point)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)


        canvas!!.drawRect(viewPort,circlePaint)

        for (point in pointPositions) {
            canvas.drawCircle(point.x,point.y,point.y,circlePaint)
        }
    }


}