package com.llx278.oneline

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import com.llx278.oneline.Point.Companion.RECT_NUM
import java.util.*
import kotlin.collections.ArrayList

class OneLineView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

    private val pointPaint: Paint = Paint()
    private val linePaint = Paint()
    private val trackPaint = Paint()

    private val verticalLines = FloatArray(RECT_NUM * 4)
    private val horizontalLines = FloatArray(RECT_NUM * 4)
    /**
     * 所有小矩形的可绘制的空间
     */
    private val viewPort = Rect()
    /**
     * 所有小矩形的容器,这个是在viewPort的基础上裁剪而来的,因为Viewport内部的空间可能无法整除RECT_NUM*RECT_NUM个
     * 小矩形,所以需要重新计算出一个新的矩形空间
     */
    private val clippedRect = Rect()

    private var linesInfo: LinesInfo? = null

    private var drawLines: Array<PointInfo>? = null

    private var tempDrawLines: FloatArray? = null

    private val backTracking = Stack<TrackLine>()

    private var trackLine: TrackLine? = null

    private val valueAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 80f)

    private var lastPoint: Point? = null
    private var lastColor: Int = -1
    private var radius: Float = -1f

    private var trackFinish = false

    /**
     * kotlin的二维数组太TM扯淡了
     *
     * 每个小矩形代表一个点，共有 RECT_NUM * RECT_NUM个点
     */
    private val smallRects = Array(RECT_NUM * RECT_NUM) {
        Rect()
    }

    init {
        valueAnimator.addUpdateListener(this)
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.repeatMode = ValueAnimator.REVERSE
        valueAnimator.repeatCount = ValueAnimator.INFINITE
    }

    fun setLineInfo(linesInfo: LinesInfo) {
        this.linesInfo = linesInfo
        requestLayout()
    }

    override fun onAnimationUpdate(animation: ValueAnimator?) {

        animation ?: return

        if (backTracking.isEmpty()) {
            lastPoint = null
            return
        }
        // 找到最近一条线的坐标
        val line = backTracking.peek()
        lastPoint = line.endPoint
        lastColor = linesInfo?.pointInfos?.get(0)?.color ?: return
        radius = animation.animatedValue as Float
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

        viewPort.left = left
        viewPort.top = top
        viewPort.right = right
        viewPort.bottom = bottom

        clipViewPortAndFillSmallRects(left, top, right, bottom)

        createLines()
    }

    fun pop() {
        if (backTracking.isNotEmpty()) {
            backTracking.pop()
            if (!valueAnimator.isStarted) {
                valueAnimator.start()
            }
            invalidate()
        }
    }

    private fun createLines() {
        val drawLineArray = ArrayList<PointInfo>()
        val tempDrawLineArray = ArrayList<Float>()
        val pointInfos = this.linesInfo!!.pointInfos
        for (i in pointInfos.indices) {
            if (i == pointInfos.size - 1) {
                break
            }

            val pointInfo = pointInfos[i]
            val nextPointInfo = pointInfos[i + 1]
            val rect = convertToSmallRect(pointInfo.point)
            val nextRect = convertToSmallRect(nextPointInfo.point)
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


        this.tempDrawLines = FloatArray(tempDrawLineArray.size) {
            tempDrawLineArray[it]
        }

        this.drawLines = Array(drawLineArray.size) {
            drawLineArray[it]
        }
    }

    fun isValidPoint(startPoint: Point): Boolean {
        var validPoint = false
        for (info in linesInfo!!.pointInfos) {
            if (info.point == startPoint) {
                validPoint = true
            }
        }

        return validPoint
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event!!.x
        val y = event.y
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                valueAnimator.cancel()

                // 将(x,y)转换为Point
                val startPoint = convertToPoint(x, y) ?: return true
                if (!isValidPoint(startPoint)) {
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

                if (trackLine == null) {
                    return true
                }

                // 规则:
                //  1. 移动过程中如果手指触碰到了一个点，并且这个两个点的连线是在路径上，那么就把这条线
                //     记录下来，并重新生成一个新的路线
                val movingPoint = convertToPoint(x, y)

                if (movingPoint == null) {
                    trackLine!!.trackX = x
                    trackLine!!.trackY = y
                    invalidate()
                    return true
                }

                if (!isValidPoint(movingPoint)) {
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
                val connectPoints = findConnectLines(trackLine!!.startPoint)
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
                    Log.d("main","绘制完成!!")
                    trackFinish = true
                    return true
                }
                trackLine = TrackLine()
                trackLine!!.startPoint = movingPoint
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!trackFinish) {
                    valueAnimator.start()
                }
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
        // 只需要保证linesInfo里面的线在backTracking里面有且仅出现一次就可以证明完成了一笔画
        loop@ for (i in drawLines!!.indices step 2) {
            if (i == drawLines!!.size - 2) {
                break
            }
            val startPointInfo = drawLines!![i]
            val endPointInfo = drawLines!![i + 1]
            var sameLine = false
            for (trackLine in backTracking) {
                sameLine = (trackLine.startPoint == startPointInfo.point && trackLine.endPoint == endPointInfo.point) ||
                        (trackLine.startPoint == endPointInfo.point && trackLine.endPoint == startPointInfo.point)
                // 找到了一个相同的线,继续找下一个线
                if (sameLine) {
                    continue@loop
                }
            }
            // 当前的这条线在for循环遍历完成以后都没有找到，那么就说明并没有完成所有的连线
            if (!sameLine) {
                return false
            }
        }
        return true
    }

    private fun findConnectLines(point: Point?): ArrayList<Point> {

        val points = ArrayList<Point>()

        for (i in drawLines!!.indices) {

            if (i == drawLines!!.size - 1) {
                break
            }

            val startPoint = drawLines!![i].point
            val endPoint = drawLines!![i + 1].point

            if (startPoint == point) {
                points.add(endPoint)
            } else if (endPoint == point) {
                points.add(startPoint)
            }
        }
        return points
    }

    /**
     * 将屏幕上的坐标转换为圆点的位置，如果存在就返回指定的Point，不存在就返回null
     */
    private fun convertToPoint(x: Float, y: Float): Point? {
        val index = smallRects.indices
        for (i in index) {
            val rect = smallRects[i]
            if (rect.contains(x.toInt(), y.toInt())) {
                return com.llx278.oneline.Point(i / RECT_NUM, i % RECT_NUM)
            }
        }
        return null
    }


    private fun convertToSmallRect(point: Point): Rect? {
        return smallRects[point.x * RECT_NUM + point.y]
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawLines(canvas!!, linePaint)
        drawBackStack(canvas, trackPaint)
        drawTrackLine(canvas, trackPaint)
        drawPoints(canvas, pointPaint)

        if (lastPoint != null) {
            val rect = convertToSmallRect(lastPoint!!)
            val x = rect?.exactCenterX() ?: return
            val y = rect.exactCenterY()
            trackPaint.style = Paint.Style.FILL
            trackPaint.color = lastColor
            canvas.drawCircle(x, y, radius, trackPaint)
        }
    }

    private fun drawTrackLine(canvas: Canvas, trackPaint: Paint) {
        if (trackLine != null && trackLine!!.canDraw()) {
            trackPaint.style = Paint.Style.STROKE
            trackPaint.strokeWidth = trackLine!!.strokeWidth
            trackPaint.color = trackLine!!.color
            val startRect = convertToSmallRect(trackLine!!.startPoint!!)
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
            val startRect = convertToSmallRect(line.startPoint!!)
            val endRect = convertToSmallRect(line.endPoint!!)
            val startX = startRect!!.exactCenterX()
            val startY = startRect.exactCenterY()
            val endX = endRect!!.exactCenterX()
            val endY = endRect.exactCenterY()
            trackPaint.color = line.color
            trackPaint.strokeWidth = line.strokeWidth
            canvas.drawLine(startX, startY, endX, endY, trackPaint)
        }
    }

    private fun drawPoints(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.FILL

        if (linesInfo == null) {
            return
        }

        val pointInfos = linesInfo!!.pointInfos
        for (info in pointInfos) {
            paint.color = info.color
            val rect = convertToSmallRect(info.point)
            canvas.drawCircle(rect!!.exactCenterX(), rect.exactCenterY(), info.radius, paint)
        }
    }

    private fun drawLines(canvas: Canvas, paint: Paint) {

        if (linesInfo == null) {
            return
        }

        paint.style = Paint.Style.STROKE
        paint.color = linesInfo!!.color
        paint.strokeWidth = linesInfo!!.strokeWidth

        canvas.drawLines(tempDrawLines!!, paint)
    }

    private fun clipViewPortAndFillSmallRects(left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val div = width.div(RECT_NUM)
        val residue = width - div * RECT_NUM
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
        clippedRect.right = clippedRect.left + div * RECT_NUM
        clippedRect.top = top + begin
        clippedRect.bottom = clippedRect.top + div * RECT_NUM

        for (i in 0 until RECT_NUM) {
            for (j in 0 until RECT_NUM) {
                val smallLeft = clippedRect.left + i * div
                val smallTop = clippedRect.top + j * div
                val smallRight = smallLeft + div
                val smallBottom = smallTop + div
                smallRects[i * RECT_NUM + j].set(smallLeft, smallTop, smallRight, smallBottom)
            }
        }
    }

    inner class TrackLine {

        val color: Int
        val strokeWidth: Float

        var startPoint: Point? = null
        var trackX: Float = -1.0f
        var trackY: Float = -1.0f
        var endPoint: Point? = null

        fun canDraw(): Boolean {
            return (startPoint != null && endPoint != null)
                    || (startPoint != null && (trackX > 0 && trackY > 0))
        }

        init {
            color = Color.BLUE
            strokeWidth = 40f
        }

    }
}