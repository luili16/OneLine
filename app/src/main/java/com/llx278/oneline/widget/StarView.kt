package com.llx278.oneline.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class StarView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val viewPort: Rect = Rect()

    private val paint: Paint = Paint()

    private val rectPaint: Paint = Paint()

    /**
     * 正五角星路线,这会在onSizeChange的时候进行初始化
     */
    private lateinit var paths : Array<PointF>
    private lateinit var paths1 : FloatArray

    init {
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL

        rectPaint.color = Color.BLACK
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = 4f
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

        viewPort.set(left, top, right, bottom)

        calculatePosition()
    }

    /**
     * 计算正五角星的顶点和交点的坐标
     */
    private fun calculatePosition() {

        // 外接圆的半径
        val r = viewPort.width() / 2.toFloat()
        // 圆内接正五边形的边长
        val l = (2 * Math.sin(Math.toRadians(36.0)) * r).toFloat()
        // 正五边形的中心点到其中一条边的距离
        val s = (Math.cos(Math.toRadians(36.0)) * r).toFloat()

        // 五角星顶点的坐标
        val a = PointF(r, 0f)
        val b = PointF((r - l * Math.cos(Math.toRadians(36.0))).toFloat(), (l * Math.sin(Math.toRadians(36.0))).toFloat())
        val c = PointF(r - l / 2.toFloat(), r + s)
        val d = PointF(r + l / 2.toFloat(), r + s)
        val e = PointF((r + l * Math.cos(Math.toRadians(36.0))).toFloat(), (l * sin(Math.toRadians(36.0))).toFloat())

        // 正五角星定点到对边的距离
        val ds = l * Math.sin(Math.toRadians(36.0))
        // 正五角星一条边的长度
        val ds0 = ds / Math.cos(Math.toRadians(18.0))
        // 正五角星任意两点的连线的距离相等，所以只需要计算一条就可以了
        // 计算a c 两点的距离
        val l0 = Math.sqrt((Math.abs(c.x - a.x) * Math.abs(c.x - a.x) +
                Math.abs(c.y - a.y) * Math.abs(c.y - a.y)).toDouble())

        // 计算正五角星任意两点连线的交点的坐标，一共5个
        val f = PointF((r - ds * Math.tan(Math.toRadians(18.0))).toFloat(), ds.toFloat())
        val g = PointF((r + ds * Math.tan(Math.toRadians(18.0))).toFloat(), ds.toFloat())
        val h1 = PointF((r - (l0 - ds0) * Math.sin(Math.toRadians(18.0))).toFloat(),
                ((l0 - ds0) * (cos(Math.toRadians(18.0)))).toFloat())
        val i = PointF(r, (r * Math.tan(Math.toRadians(54.0))).toFloat())
        val j = PointF((r + (l0 - ds0) * Math.sin(Math.toRadians(18.0))).toFloat(),
                ((l0 - ds0) * Math.cos(Math.toRadians(18.0))).toFloat())
        // 一共10条线，每条线有两个点,每个点有x和y 共有40个元素
        paths = arrayOf(
                a,f,
                f,b,
                b,h1,
                h1,c,
                c,i,
                i,d,
                d,j,
                j,e,
                e,g,
                g,a
        )

        paths1 = floatArrayOf(
                a.x,a.y,f.x,f.y,
                f.x,f.y,b.x,b.y,
                b.x,b.y,h1.x,h1.y,
                h1.x,h1.y,c.x,c.y,
                c.x,c.y,i.x,i.y,
                i.x,i.y,d.x,d.y,
                d.x,d.y,j.x,j.y,
                j.x,j.y,e.x,e.y,
                e.x,e.y,g.x,g.y,
                g.x,g.y,a.x,a.y
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas!!.drawRect(viewPort, rectPaint)

        canvas.save()
        canvas.translate(viewPort.left.toFloat(), viewPort.top.toFloat())
        canvas.drawLines(paths1,rectPaint)


        
        /*canvas.drawCircle(a!!.x, a!!.y, 15f, paint)
        canvas.drawCircle(b!!.x, b!!.y, 15f, paint)
        canvas.drawCircle(c!!.x, c!!.y, 15f, paint)
        canvas.drawCircle(d!!.x, d!!.y, 15f, paint)
        canvas.drawCircle(e!!.x, e!!.y, 15f, paint)

        canvas.drawCircle(f!!.x, f!!.y, 15f, paint)
        canvas.drawCircle(g!!.x, g!!.y, 15f, paint)
        canvas.drawCircle(h1!!.x, h1!!.y, 15f, paint)
        canvas.drawCircle(i!!.x, i!!.y, 15f, paint)
        canvas.drawCircle(j!!.x, j!!.y, 15f, paint)*/

        canvas.restore()

        //canvas.drawLines(paths1,rectPaint)
    }
}