package com.llx278.oneline

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

class GridPattern {

    private lateinit var viewPort: Rect

    /**
     * 所有小矩形的容器,这个是在viewPort的基础上裁剪而来的,因为Viewport内部的空间可能无法整除RECT_NUM*RECT_NUM个
     * 小矩形,所以需要重新计算出一个新的矩形空间
     */
    private val clippedRect = Rect()

    /**
     * kotlin的二维数组太TM扯淡了
     *
     * 每个小矩形代表一个点，共有 RECT_NUM * RECT_NUM个点
     */
    private val smallRects = Array(Point.RECT_NUM * Point.RECT_NUM) {
        Rect()
    }

    private val verticalLines = FloatArray(Point.RECT_NUM * 4)
    private val horizontalLines = FloatArray(Point.RECT_NUM * 4)

    fun decideSize(left: Int, top: Int, right: Int, bottom: Int) {

        viewPort = Rect(left,top,right,bottom)
        clipViewPort(left,top,right,bottom)
    }

    private fun clipViewPort(left: Int, top: Int, right: Int, bottom: Int) {

        val width = right - left
        val div = width.div(Point.RECT_NUM)
        val residue = width - div * Point.RECT_NUM
        val begin = Math.ceil(residue.toDouble() / 2).toInt()

        val num = verticalLines.indices

        // 因为传入的一定是一个矩形，这里面就将垂直和水平的一起计算了，因为值是相同的
        for (i in num step 4) {
            verticalLines[i] = (left + begin + (i / 4) * div).toFloat()
            verticalLines[i + 1] = top.toFloat()
            verticalLines[i + 2] = (left + begin + (i / 4) * div).toFloat()
            verticalLines[i + 3] = bottom.toFloat()

            horizontalLines[i] = left.toFloat()
            horizontalLines[i + 1] = (top + begin + (i / 4) * div).toFloat()
            horizontalLines[i + 2] = right.toFloat()
            horizontalLines[i + 3] = (top + begin + (i / 4) * div).toFloat()
        }

        clippedRect.left = left + begin
        clippedRect.right = clippedRect.left + div * Point.RECT_NUM
        clippedRect.top = top + begin
        clippedRect.bottom = clippedRect.top + div * Point.RECT_NUM

        for (i in 0 until Point.RECT_NUM) {
            for (j in 0 until Point.RECT_NUM) {
                val smallLeft = clippedRect.left + i * div
                val smallTop = clippedRect.top + j * div
                val smallRight = smallLeft + div
                val smallBottom = smallTop + div
                smallRects[i * Point.RECT_NUM + j].set(smallLeft, smallTop, smallRight, smallBottom)
            }
        }
    }

    /**
     * 将屏幕上的坐标转换为圆点的位置，如果存在就返回指定的Point，不存在就返回null
     */
    fun convertToPoint(x: Float, y: Float): Point? {
        val index = smallRects.indices
        for (i in index) {
            val rect = smallRects[i]
            if (rect.contains(x.toInt(), y.toInt())) {
                return com.llx278.oneline.Point(i / Point.RECT_NUM, i % Point.RECT_NUM)
            }
        }
        return null
    }


    fun convertToSmallRect(point: Point): Rect? {
        return smallRects[point.x * Point.RECT_NUM + point.y]
    }

    fun drawGride(canvas: Canvas, paint: Paint) {
    }
}