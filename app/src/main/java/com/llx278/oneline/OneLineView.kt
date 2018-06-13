package com.llx278.oneline

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import java.lang.ref.WeakReference
import java.util.*

class OneLineView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

    private val trackPaint = Paint()
    private val backTracking = Stack<TrackLine>()

    private var trackLine: TrackLine? = null

    private val hintAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 80f)

    private var radius: Float = -1f

    private val grid: GridPattern = GridPattern()
    private var pattern: GeometricalPattern? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        hintAnimator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hintAnimator.cancel()
    }

    init {
        hintAnimator.addUpdateListener(this)
        hintAnimator.interpolator = LinearInterpolator()
        hintAnimator.repeatMode = ValueAnimator.REVERSE
        hintAnimator.repeatCount = ValueAnimator.INFINITE
        hintAnimator.duration = 600
    }

    fun setLineInfo(linesInfo: LinesInfo) {
        pattern = GeometricalPattern(linesInfo, WeakReference(this))
        requestLayout()
    }

    override fun onAnimationUpdate(animation: ValueAnimator?) {
        if (backTracking.isEmpty()) {
            return
        }
        radius = animation?.animatedValue as Float
        invalidate()
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val left = (w - measuredWidth) / 2
        val top = (h - measuredHeight) / 2
        val right = left + measuredWidth
        val bottom = top + measuredHeight
        grid.decideSize(left, top, right, bottom)
        pattern?.createAbsAndScreenPoints(grid)
    }

    fun pop() {
        if (backTracking.isNotEmpty()) {
            backTracking.pop()
            if (!hintAnimator.isStarted) {
                hintAnimator.start()
            }
            invalidate()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event!!.x
        val y = event.y
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {

                if (pattern == null) {
                    return true
                }

                // 将(x,y)转换为Point
                val startPoint = grid.convertToPoint(x, y) ?: return true
                if (!pattern!!.isValidPoint(startPoint)) {
                    return true
                }

                // 规则:
                // 1. 如果是第一个点，那么选择任意一点为起始点
                // 2. 如果不是第一个点，那么这个起始点就应该是上一条绘制的线的终点作为起始点

                // 是第一个点
                if (backTracking.isEmpty()) {
                    // 先创建一个trackLine
                    trackLine = TrackLine()
                    trackLine!!.startPoint = startPoint
                    return true
                } else {
                    // 不是第一个点,那么就要判断这个点是不是上一条线的终点
                    val preLine = backTracking.peek()

                    // 是，创建一个trackLine
                    if (preLine.endPoint == startPoint) {
                        trackLine = TrackLine()
                        trackLine!!.startPoint = startPoint
                        return true
                    }
                    // 不是，那么忽略此次点击
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {

                if (trackLine == null || pattern == null) {
                    return true
                }

                // 规则:
                //  1. 移动过程中如果手指触碰到了一个点，并且这个两个点的连线是在路径上，那么就把这条线
                //     记录下来，并重新生成一个新的路线
                val movingPoint = grid.convertToPoint(x, y)

                if (movingPoint == null) {
                    trackLine!!.trackX = x
                    trackLine!!.trackY = y
                    invalidate()
                    return true
                }

                if (!pattern!!.isValidPoint(movingPoint)) {
                    trackLine!!.trackX = x
                    trackLine!!.trackY = y
                    invalidate()
                    return true
                }
                // 根据这个线的startPoint来推断可能的下一个点，这个点需要满足以下规则
                // 1 : 下一个点不能是这个线的起始点
                // 2 : 下一个点应该与起始点是连通的
                // 3 : 下一个点不应该是已经保存的trackLine的点

                // 下一个点不能是这个线的起点
                if (trackLine!!.startPoint == movingPoint) {
                    // 持续绘制
                    trackLine!!.trackX = x
                    trackLine!!.trackY = y
                    invalidate()
                    return true
                }

                // 找到所有的连通点
                val connectPoints = pattern!!.findConnectLines(trackLine!!.startPoint!!)
                // 没有找到连通点（应该是不存在的，除非坐标输入错了!）
                if (connectPoints.isEmpty()) {
                    trackLine!!.trackX = x
                    trackLine!!.trackY = y
                    invalidate()
                    return true
                }
                // 判断movingPoint是否与其中的一个连通点相同
                var isConnect = false
                for (point in connectPoints) {
                    if (point == movingPoint) {
                        isConnect = true
                    }
                }

                // 不是连通的
                if (!isConnect) {
                    // 持续绘制
                    trackLine!!.trackX = x
                    trackLine!!.trackY = y
                    invalidate()
                    return true
                }

                // 如果是其中的一个连通点,那么这个点不应该是上一个trackLine的起点
                // 还没已经保存的线，那么这个点可以直接绘制

                // 直接绘制
                if (backTracking.isEmpty()) {
                    trackLine!!.endPoint = movingPoint
                    backTracking.push(trackLine!!)

                    // 新建一个新的trackLine,新trackline的起点就是上一条线的终点
                    trackLine = TrackLine()
                    trackLine!!.startPoint = movingPoint
                    invalidate()
                    return true
                }

                // 确保此条线没有被绘制过
                val iterator = backTracking.iterator()
                val startPoint = trackLine!!.startPoint
                while (iterator.hasNext()) {
                    val trackLine1 = iterator.next()
                    if ((trackLine1.startPoint == startPoint
                                    && trackLine1.endPoint == movingPoint) ||
                            (trackLine1.startPoint == movingPoint
                                    && trackLine1.endPoint == startPoint)) {
                        trackLine!!.trackX = x
                        trackLine!!.trackY = y
                        invalidate()
                        return true
                    }
                }

                // 将这个绘制的线保存下来
                trackLine!!.endPoint = movingPoint
                backTracking.push(trackLine!!)
                if (checkFinish()) {
                    Log.d("main", "绘制完成!!")
                    hintAnimator.cancel()
                    pattern?.showAnimation()
                    invalidate()
                    return true
                }
                trackLine = TrackLine()
                trackLine!!.startPoint = movingPoint
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                // 当手指抬起以后，已经结束了一次绘制，清空trackLine
                trackLine = null
                invalidate()
                return true
            }

            else -> return super.onTouchEvent(event)
        }
    }

    // push成功以后需要判断是不是已经绘制完成了，实际上在移动过程中所做的种种限制就保证了
    // 当所有的线条都画完了以后，所得的路径就是由一笔画出的，当然，这个路径可能与linesInfo
    // 所规定的有所不同，因为答案并不是唯一的
    private fun checkFinish(): Boolean {
        return backTracking.size == pattern!!.getNumOfLine()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        pattern?.draw(canvas!!, grid)
        drawBackStack(canvas!!, trackPaint)
        drawTrackLine(canvas, trackPaint)
        pattern?.drawPoints(canvas,grid)

        if (backTracking.isNotEmpty()) {
            val lastPoint = backTracking.peek().endPoint
            val rect = grid.convertToSmallRect(lastPoint!!)
            val x = rect?.exactCenterX() ?: return
            val y = rect.exactCenterY()
            trackPaint.style = Paint.Style.FILL
            trackPaint.color = pattern?.getPointColor() ?: return
            canvas.drawCircle(x,y,radius,trackPaint)
        }
    }

    private fun drawTrackLine(canvas: Canvas, trackPaint: Paint) {
        if (trackLine != null && trackLine!!.canDraw()) {
            trackPaint.style = Paint.Style.STROKE
            trackPaint.strokeWidth = trackLine!!.strokeWidth
            trackPaint.color = trackLine!!.color
            val startRect = grid.convertToSmallRect(trackLine!!.startPoint!!)
            val startX = startRect!!.exactCenterX()
            val startY = startRect.exactCenterY()
            canvas.drawLine(startX, startY, trackLine!!.trackX, trackLine!!.trackY, trackPaint)
            trackPaint.style = Paint.Style.FILL
            canvas.drawCircle(trackLine!!.trackX, trackLine!!.trackY,
                    trackLine!!.strokeWidth / 2, trackPaint)
        }
    }

    private fun drawBackStack(canvas: Canvas, trackPaint: Paint) {
        trackPaint.style = Paint.Style.STROKE
        for (line in backTracking) {
            val startRect = grid.convertToSmallRect(line.startPoint!!)
            val endRect = grid.convertToSmallRect(line.endPoint!!)
            val startX = startRect!!.exactCenterX()
            val startY = startRect.exactCenterY()
            val endX = endRect!!.exactCenterX()
            val endY = endRect.exactCenterY()
            trackPaint.color = line.color
            trackPaint.strokeWidth = line.strokeWidth
            canvas.drawLine(startX, startY, endX, endY, trackPaint)
        }
    }

    inner class TrackLine {

        val color: Int = resources.getColor(R.color.trackLine)
        val strokeWidth: Float = resources.getDimension(R.dimen.geometrical_line_width)

        var startPoint: Point? = null
        var trackX: Float = -1.0f
        var trackY: Float = -1.0f
        var endPoint: Point? = null

        fun canDraw(): Boolean {
            return (startPoint != null && endPoint != null)
                    || (startPoint != null && (trackX > 0 && trackY > 0))
        }
    }
}