package com.llx278.oneline.widget

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import com.llx278.oneline.R
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

/**
 * 描述了一笔画所需要的几何图形
 */
class GeometricalPattern(
        /**
         * linesInfo所描述的点的绘制顺序就是一笔画图案的其中一个解
         */
        private val linesInfo: LinesInfo,
        private val viewRef: WeakReference<View>) :
        ValueAnimator.AnimatorUpdateListener {

    /**
     * 由Grid所形成的网格坐标所描述的线
     */
    private var abstractLines: Array<Point>? = null

    /**
     * 真实的屏幕坐标系的的线
     */
    private var screenLines: FloatArray? = null

    /**
     * 保存了提示点的相关信息
     */
    private val hintPoints = ArrayList<HintHolder>()

    /**
     * 几何图形的画笔
     */
    private val paint: Paint = Paint()

    /**
     * 提示信息的画笔
     */
    private val textPaint: Paint = Paint()

    /**
     * 结束时候的动画
     */
    private val finishAnimation: ValueAnimator

    private val pointsColor: IntArray
    private val linesColor: IntArray

    /**
     * 圆点的半径
     */
    private var defaultRadius: Float

    /**
     * 做动画的时候动态改变的半径
     */
    private var animateRadius: Float

    /**
     * 圆点的alpha值
     */
    private var alpha: Int = 255

    /**
     * 用来保存一个文本的边界
     */
    private val boundRect = Rect()

    private var showHint = false

    private val mainHandlerRef: WeakReference<MainHandler> =
            WeakReference(MainHandler((Looper.getMainLooper())))

    /**
     * 线的宽度
     */
    private val strokeWidth: Float

    private var currentPointColor: Int = 0

    private var currentLineColor: Int = 0

    /**
     * 提示文字线条的宽度
     */
    private var hintTextWidth: Float = 0f

    /**
     * 提示文字的大小
     */
    private var hintTextSize: Float = 0f

    /**
     * 提示文字的颜色
     */
    private var hintTextColor: Int = 0

    /**
     * 用来更新提示信息的索引
     */
    private var updateIndex: Int = 0

    init {

        val view = viewRef.get()
        val resources = view?.resources
        val pointsColorsArray = resources?.obtainTypedArray(R.array.point_colors)
        pointsColor = IntArray(pointsColorsArray!!.length()) {
            pointsColorsArray.getColor(it, 0)
        }
        pointsColorsArray.recycle()

        val linesColorsArray = resources.obtainTypedArray(R.array.line_colors)
        linesColor = IntArray(linesColorsArray.length()) {
            linesColorsArray.getColor(it, 0)
        }
        linesColorsArray.recycle()

        val radiusProperty = PropertyValuesHolder.ofFloat("defaultRadius",
                resources.getDimension(R.dimen.geometrical_point_radius),
                resources.getDimension(R.dimen.geometrical_point_radius_x4))
        val alphaProperty = PropertyValuesHolder.ofInt("alpha", 255, 0)
        finishAnimation = ValueAnimator.ofPropertyValuesHolder(radiusProperty, alphaProperty)
        finishAnimation.addUpdateListener(this)
        finishAnimation.duration = 1500
        finishAnimation.repeatCount = 0
        finishAnimation.interpolator = LinearInterpolator()

        defaultRadius = resources.getDimension(R.dimen.geometrical_point_radius)
        animateRadius = defaultRadius
        strokeWidth = resources.getDimension(R.dimen.geometrical_line_width)
        hintTextWidth = resources.getDimension(R.dimen.hint_text_width)
        hintTextSize = resources.getDimension(R.dimen.hint_text_size)
        hintTextColor = resources.getColor(R.color.hint_text_color)

        currentPointColor = pointsColor[0]
        currentLineColor = linesColor[0]

    }

    override fun onAnimationUpdate(animation: ValueAnimator?) {
        animation ?: return
        animateRadius = animation.getAnimatedValue("defaultRadius") as Float
        alpha = animation.getAnimatedValue("alpha") as Int
        viewRef.get()?.invalidate()
    }

    /**
     * 创建出真实的屏幕坐标和由Gride所识别的坐标
     */
    fun createPattern(grid: GridPattern) {

        val drawLineArray = ArrayList<Point>()
        val tempDrawLineArray = ArrayList<Float>()
        val points = this.linesInfo.points
        for (i in points.indices) {
            if (i == points.size - 1) {
                break
            }

            val point = points[i]
            val nextPoint = points[i + 1]
            val rect = grid.convertToSmallRect(point)
            val nextRect = grid.convertToSmallRect(nextPoint)
            val startX = rect.exactCenterX()
            val startY = rect.exactCenterY()
            val endX = nextRect.exactCenterX()
            val endY = nextRect.exactCenterY()

            drawLineArray.add(point)
            drawLineArray.add(nextPoint)

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

        // 生成提示信息
        for (i in points.indices) {

            val point = points[i]

            val index = i + 1
            val holder = HintHolder(point, arrayListOf(index), 0)
            if (hintPoints.isEmpty()) {
                // 直接加入
                hintPoints.add(holder)
            } else {
                val addedIndex = hintPoints.indexOf(holder)
                if (addedIndex < 0) {
                    // 没有重复的点
                    hintPoints.add(holder)
                } else {
                    val addedPoints = hintPoints[addedIndex]
                    addedPoints.strs.add(index)
                }
            }
        }
    }

    /**
     * 对这个图形做一些动画
     */
    fun showFinishAnimation() {
        finishAnimation.start()
    }

    /**
     * 显示提示信息
     */
    fun showHint() {
        showHint = true

        mainHandlerRef.get()?.postDelayed(object : Runnable {
            override fun run() {

                if (!showHint) {
                    return
                }

                for (holder in hintPoints) {
                    if (holder.strs.size > 1) {
                        holder.index = (updateIndex++).let {
                            val value: Int = if (it == Int.MAX_VALUE) {
                                0
                            } else {
                                it
                            }
                            value
                        } % holder.strs.size
                    }
                }
                viewRef.get()?.invalidate()
                mainHandlerRef.get()?.postDelayed(this, 2000)
            }
        }, 2000)
    }

    fun hideHint() {
        showHint = false
        viewRef.get()?.invalidate()
    }

    /**
     * 随机改变图形中点的颜色
     */
    fun changePointColor() {
        currentPointColor = pointsColor[Random().nextInt(10)]
        viewRef.get()?.invalidate()
    }

    /**
     * 随机改变图形中线的颜色
     */
    fun changeLineColor() {
        currentLineColor = linesColor[Random().nextInt(5)]
        viewRef.get()?.invalidate()
    }

    /**
     * 返回点的颜色
     */
    fun getPointColor(): Int {
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
        val pointInfos = linesInfo.points
        for (info in pointInfos) {
            paint.color = currentPointColor
            paint.alpha = 255
            val rect = grid.convertToSmallRect(info) ?: return
            canvas.drawCircle(rect.exactCenterX(), rect.exactCenterY(), defaultRadius, paint)
            if (finishAnimation.isStarted) {
                paint.alpha = alpha
                canvas.drawCircle(rect.exactCenterX(), rect.exactCenterY(), animateRadius, paint)
            }
        }
    }

    fun drawHint(canvas: Canvas, grid: GridPattern) {
        textPaint.style = Paint.Style.FILL
        if (showHint) {
            for (i in hintPoints.indices) {
                val holder = hintPoints[i]
                val point = holder.point
                val screenPointF = grid.convertToXY(point)
                textPaint.strokeWidth = 5f
                textPaint.textSize = 80f
                textPaint.color = Color.BLACK
                textPaint.textAlign = Paint.Align.CENTER
                val str = holder.strs[holder.index].toString()
                textPaint.getTextBounds(str, 0, str.length, boundRect)
                // 重新计算文本的x和y坐标
                val x = screenPointF.x
                val y = screenPointF.y + boundRect.height().div(2)
                canvas.drawText(str, x, y, textPaint)
            }
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

            val startPoint = abstractLines!![i]
            val endPoint = abstractLines!![i + 1]

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
        for (point in linesInfo.points) {
            if (point == startPoint) {
                validPoint = true
            }
        }

        return validPoint
    }

    private inner class HintHolder(val point: Point, val strs: ArrayList<Int>, var index: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HintHolder

            if (point != other.point) return false

            return true
        }

        override fun hashCode(): Int {
            return point.hashCode()
        }

        override fun toString(): String {
            return "HintHolder(point=$point, indexs=$strs)"
        }
    }

    inner class MainHandler(looper: Looper?) : Handler(looper) {

    }

}