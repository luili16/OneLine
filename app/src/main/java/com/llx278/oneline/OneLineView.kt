package com.llx278.oneline

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.llx278.oneline.Point.Companion.RECT_NUM
import java.util.*

class OneLineView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

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

    /**
     * kotlin的二维数组太TM扯淡了
     *
     * 每个小矩形代表一个点，共有 RECT_NUM * RECT_NUM个点
     */
    private val smallRects = Array(RECT_NUM * RECT_NUM) {
        Rect()
    }

    private var drawInfo: DrawInfo? = null

    //private val trackLine = TrackLine(Color.BLUE, 40f)

    private val backTracking = Stack<TrackLine>()

    init {
        pointPaint.color = Color.BLUE
    }

    fun setDrawInfo(drawInfo: DrawInfo) {
        this.drawInfo = drawInfo
        postInvalidate()
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
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event!!.x
        val y = event.y
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // 判断(x,y)是否落在了小矩形方框内
                val rect = getSmallRect(x, y)

                @Suppress("FoldInitializerAndIfToElvis")
                // (x,y)没有落在小矩形方框内
                if (rect == null) {
                    return super.onTouchEvent(event)
                }

                val point = PointF(rect.exactCenterX(), rect.exactCenterY())
                // 第一次点击，直接生成一个新的trackLine
                if (backTracking.isEmpty()) {
                    val trackLine = TrackLine(Color.BLUE, 30f)
                    trackLine.begin(point.x, point.y)
                    backTracking.push(trackLine)
                    invalidate()
                    return true
                }

                // 先前已经有一个trackLine，那就意味着新的trackLine需要先前的trackLine的终点作为起点
                val preTrackLine = backTracking.peek()
                val endX = preTrackLine.endX.toInt()
                val endY = preTrackLine.endY.toInt()
                // 此时的(x,y)没有落在先前的trackLine的终点所在的小矩形里面
                if (!rect.contains(endX, endY)) {
                    return super.onTouchEvent(event)
                }

                // 在小矩形里面
                val trackLine = TrackLine(Color.BLUE, 30f)
                trackLine.begin(point.x, point.y)
                backTracking.push(trackLine)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!backTracking.isNotEmpty()) {
                    return super.onTouchEvent(event)
                }
                val trackLine = backTracking.peek()
                trackLine.track(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (backTracking.isEmpty()) {
                    return super.onTouchEvent(event)
                }

                if (canSaveToBackTracking(x, y)) {

                }

                val trackLine = backTracking.peek()

                trackLine.end(x, y)
                invalidate()
                return true
            }
            else -> return super.onTouchEvent(event)
        }
    }

    /**
     * 判断手指抬起的坐标是否在某个连着线的点上
     */
    private fun canSaveToBackTracking(x: Float, y: Float): Boolean {

        val point = convertTo(x, y) ?: return false
        // 找到当前绘制线条的起始点
        val trackLine = backTracking.peek()
        val startPoint = convertTo(trackLine.startX,trackLine.startY)
        // 找到这个起始点可能指向的终点
        for (line in drawInfo!!.lineInfo) {
            if (line.startPoint == startPoint) {
                // 找到了一个终点与当前绘制的线的终点相同
                if (line.endPoint == point) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * 将屏幕上的坐标转换为圆点的位置，如果存在就返回指定的Point，不存在就返回null
     */
    private fun convertTo(x: Float, y: Float): Point? {
        val index = smallRects.indices
        for (i in index) {
            val rect = smallRects[i]
            if (rect.contains(x.toInt(), y.toInt())) {
                return com.llx278.oneline.Point(i / RECT_NUM, i % RECT_NUM)
            }
        }
        return null
    }

    private fun getSmallRect(x: Float, y: Float): Rect? {
        if (drawInfo == null) {
            return null
        }

        for (info in drawInfo!!.pointInfo) {
            val rect = smallRects[info.point.x * RECT_NUM + info.point.y]
            if (rect.contains(x.toInt(), y.toInt())) {
                return rect
            }
        }
        return null
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawLines(canvas!!, linePaint)
        drawTrackLines(canvas, trackPaint)
        drawPoints(canvas, pointPaint)
    }

    private fun drawTrackLines(canvas: Canvas, trackPaint: Paint) {
        if (backTracking.isNotEmpty()) {
            for (trackLine in backTracking) {
                trackLine.drawLine(canvas, trackPaint)
            }
        }
    }

    private fun drawPoints(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.FILL
        if (drawInfo == null) {
            return
        }

        val pointInfos = drawInfo!!.pointInfo
        for (info in pointInfos) {
            paint.color = info.color
            val x = info.point.x
            val y = info.point.y
            val rect = smallRects[x * RECT_NUM + y]
            canvas.drawCircle(rect.exactCenterX(), rect.exactCenterY(), info.radius, paint)
        }
    }

    private fun drawLines(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.STROKE
        val lineInfos = drawInfo!!.lineInfo

        for (info in lineInfos) {
            paint.strokeWidth = info.strokeWidth
            paint.color = info.color
            val startRect = smallRects[info.startPoint.x * RECT_NUM + info.startPoint.y]
            val startX = startRect.exactCenterX()
            val startY = startRect.exactCenterY()
            val endRect = smallRects[info.endPoint.x * RECT_NUM + info.endPoint.y]
            val endX = endRect.exactCenterX()
            val endY = endRect.exactCenterY()
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
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
                val smallLeft = clippedRect.left + j * div
                val smallTop = clippedRect.top + i * div
                val smallRight = smallLeft + div
                val smallBottom = smallTop + div
                smallRects[i * RECT_NUM + j].set(smallLeft, smallTop, smallRight, smallBottom)
            }
        }
    }

    inner class TrackLine(private val color: Int, private val strokeWidth: Float) {

        var startX: Float = -1.0f
        var startY: Float = -1.0f
        private var trackX: Float = -1.0f
        private var trackY: Float = -1.0f
        var endX: Float = -1.0f
        var endY: Float = -1.0f

        private var status: Int = -1

        fun begin(x: Float, y: Float) {
            this.startX = x
            this.startY = y
            this.status = 1
        }

        fun end(x: Float, y: Float) {
            this.endX = x
            this.endY = y
            status = 3
        }

        fun track(x: Float, y: Float) {
            this.trackX = x
            this.trackY = y
            status = 2
        }

        fun drawLine(canvas: Canvas, paint: Paint) {
            if (status <= 1) {
                return
            }
            paint.color = this.color
            paint.strokeWidth = this.strokeWidth
            canvas.drawLine(startX, startY, trackX, trackY, paint)
            canvas.drawCircle(trackX, trackY, this.strokeWidth / 2, paint)
        }
    }
}