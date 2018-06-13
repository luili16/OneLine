package com.llx278.oneline

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.animation.LinearInterpolator
import java.lang.ref.WeakReference
import java.util.*

/**
 * 描述了一笔画所需要的几何图形
 */
class GeometricalPattern(private val linesInfo: LinesInfo,
                         private val viewRef: WeakReference<View>) :
        ValueAnimator.AnimatorUpdateListener {

    /**
     * 由Gride所形成的网格坐标所描述的线
     */
    private var abstractLines: Array<PointInfo>? = null

    /**
     * 真实的屏幕坐标系的的线
     */
    private var screenLines: FloatArray? = null
        private set

    /**
     * 缓存一个画笔
     */
    private val paint: Paint = Paint()

    private val animator = ValueAnimator.ofFloat(0f, 200f)

    private val pointsColor : IntArray
    private val linesColor : IntArray

    /**
     * 圆点的半径
     */
    private var radius : Float

    /**
     * 线的宽度
     */
    private val strokeWidth : Float

    private var currentPointColor : Int = 0

    private var currentLineColor : Int = 0

    init {
        animator.addUpdateListener(this)
        animator.interpolator = LinearInterpolator() as TimeInterpolator?
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = 1
        animator.duration = 1500

        val view = viewRef.get()
        val resources =  view?.resources
        val pointsColorsArray =resources?.obtainTypedArray(R.array.point_colors)
        pointsColor = IntArray(pointsColorsArray!!.length()) {
            pointsColorsArray.getColor(it,0)
        }
        pointsColorsArray.recycle()

        val linesColorsArray = resources.obtainTypedArray(R.array.line_colors)
        linesColor = IntArray(linesColorsArray.length()) {
            linesColorsArray.getColor(it,0)
        }
        linesColorsArray.recycle()

        radius = resources.getDimension(R.dimen.geometrical_paint_radius)
        strokeWidth = resources.getDimension(R.dimen.geometrical_line_width)

        currentPointColor = pointsColor[0]
        currentLineColor = linesColor[0]
    }

    override fun onAnimationUpdate(animation: ValueAnimator?) {
        animation ?: return
        val pointInfos = linesInfo.pointInfos
        radius = animation.animatedValue as Float
        viewRef.get()?.invalidate()
    }

    /**
     * 创建出真实的屏幕坐标和由Gride所识别的坐标
     */
    fun createAbsAndScreenPoints(grid: GridPattern) {

        val drawLineArray = ArrayList<PointInfo>()
        val tempDrawLineArray = ArrayList<Float>()
        val pointInfos = this.linesInfo.pointInfos
        for (i in pointInfos.indices) {
            if (i == pointInfos.size - 1) {
                break
            }

            val pointInfo = pointInfos[i]
            val nextPointInfo = pointInfos[i + 1]
            val rect = grid.convertToSmallRect(pointInfo.point)
            val nextRect = grid.convertToSmallRect(nextPointInfo.point)
            val startX = rect!!.exactCenterX()
            val startY = rect.exactCenterY()
            val endX = nextRect!!.exactCenterX()
            val endY = nextRect.exactCenterY()

            drawLineArray.add(pointInfo)
            drawLineArray.add(nextPointInfo)

            tempDrawLineArray.add(startX)
            tempDrawLineArray.add(startY)
            tempDrawLineArray.add(endX)
            tempDrawLineArray.add(endY)
        }

        this.screenLines = FloatArray(tempDrawLineArray.size) {
            tempDrawLineArray[it]
        }

        this.abstractLines = Array(drawLineArray.size) {
            drawLineArray[it]
        }
    }

    /**
     * 对这个图形做一些动画
     */
    fun showAnimation() {
        animator.start()
    }

    fun changePointColor() {
        currentPointColor = pointsColor[Random().nextInt(10)]
    }

    fun changeLineColor() {
        currentLineColor = linesColor[Random().nextInt(5)]
    }

    fun getPointColor() : Int {
        return currentPointColor
    }

    /**
     * 这个只是绘制线条，如果与drawPoints()写在一起，会导致
     * 绘制的点无法将线条压住
     */
    fun draw(canvas: Canvas, grid: GridPattern) {
        paint.style = Paint.Style.STROKE
        paint.color = currentLineColor
        paint.strokeWidth = strokeWidth

        canvas.drawLines(screenLines, paint)
    }

    /**
     * 点只能在最后画，只有这样才能遮住所有的连接点!
     */
    fun drawPoints(canvas: Canvas, grid: GridPattern) {
        paint.style = Paint.Style.FILL

        val pointInfos = linesInfo.pointInfos
        for (info in pointInfos) {
            paint.color = currentPointColor
            val rect = grid.convertToSmallRect(info.point) ?: return
            canvas.drawCircle(rect.exactCenterX(), rect.exactCenterY(), radius, paint)
        }
    }

    /**
     * 找到所有指向此点的线的起点
     */
    fun findConnectLines(point: Point): ArrayList<Point> {

        val points = ArrayList<Point>()

        if (abstractLines == null) {
            return points
        }

        for (i in abstractLines!!.indices) {

            if (i == abstractLines!!.size - 1) {
                break
            }

            val startPoint = abstractLines!![i].point
            val endPoint = abstractLines!![i + 1].point

            if (startPoint == point) {
                points.add(endPoint)
            } else if (endPoint == point) {
                points.add(startPoint)
            }
        }
        return points
    }

    fun getNumOfLine(): Int {
        return abstractLines?.size?.div(2) ?: 0
    }

    /**
     * 判断给出的点是不是在图形中，是 返回true 不是 返回false
     */
    fun isValidPoint(startPoint: Point): Boolean {
        var validPoint = false
        for (info in linesInfo.pointInfos) {
            if (info.point == startPoint) {
                validPoint = true
            }
        }

        return validPoint
    }
}