package com.llx278.oneline

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

class BackGroundView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint: Paint = Paint();

    private val lines = FloatArray(LINE_NUM * 4)

    companion object {
        const val LINE_NUM = 10
    }

    init {
        paint.color = Color.BLUE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        when {
            width == height -> super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            width > height -> setMeasuredDimension(height, height)
            else -> setMeasuredDimension(width, width)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null) {
            return
        }

        Log.d("main", "view.measuredWidth = $measuredWidth and view.measuredHeight = $measuredHeight")
        Log.d("main", "view.width = $width and view.height is $height")
        Log.d("main", "canvas.width = ${canvas.width} and canvas.height = ${canvas.height}")

        //val div = canvas.width.div(LINE_NUM)
        //val residue = canvas.width - div
        //val start = Math.ceil(residue.toDouble() / 2)

        val left = (canvas.width - measuredWidth) / 2
        val top = (canvas.height - measuredHeight) / 2
        val right = left + measuredWidth
        val bottom = top + measuredHeight
        Log.d("main", "left = $left top = $top right = $right bottom = $bottom")

        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)

        val width = right - left
        val height = bottom - top

        val div = width.div(LINE_NUM)
        val residue = width - div * LINE_NUM
        val start = Math.ceil(residue.toDouble() / 2).toFloat()

        val num = lines.indices
        Log.d("main", "num is $num div is $div residue is $residue start is $start")
        for (i in num step 4) {
            lines[i] = start + (i / 4) * div
            lines[i + 1] = top.toFloat()
            lines[i + 2] = start + (i / 4) * div
            lines[i + 3] = bottom.toFloat()
        }
        paint.strokeWidth = 10.0f
        paint.color = Color.RED
        canvas.drawLines(lines, paint)
    }


}