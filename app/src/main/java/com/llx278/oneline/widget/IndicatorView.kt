package com.llx278.oneline.widget

import android.content.Context
import android.database.DataSetObserver
import android.graphics.*
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.llx278.oneline.R
import java.lang.ref.WeakReference

@ViewPager.DecorView
class IndicatorView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val processColor: Int
    private val pointColor: Int
    private var itemCount: Int = -1
    private var pointPositions: ArrayList<PointF> = ArrayList()
    private val bgPaint: Paint = Paint()
    private var bgRadius: Float = 0f
    private val circlePaint: Paint = Paint()
    private val movingPaint: Paint = Paint()
    private val movingPoint: PointF = PointF()

    private val viewPort: Rect = Rect()
    private val bgRect: RectF = RectF()

    /**
     * 两个移动点之间的距离
     */
    private var distance: Float = 0f

    private lateinit var pagerRef: WeakReference<ViewPager>
    private lateinit var adapterRef: WeakReference<PagerAdapter>
    private lateinit var pageListener: PageListener

    init {
        val a = context.theme.obtainStyledAttributes(attrs!!,
                R.styleable.IndicatorView, 0, 0)
        try {
            processColor = a.getColor(R.styleable.IndicatorView_process_color, Color.BLACK)
            pointColor = a.getColor(R.styleable.IndicatorView_point_color, Color.BLUE)
        } finally {
            a.recycle()
        }

        //processColor = Color.GRAY
        //pointColor = Color.BLUE
        bgPaint.color = processColor
        bgPaint.style = Paint.Style.FILL

        circlePaint.color = processColor
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeWidth = 2f

        movingPaint.color = pointColor
        movingPaint.style = Paint.Style.FILL

    }

    fun setViewPager(pager: ViewPager) {
        pagerRef = WeakReference(pager)
        val adapter: PagerAdapter? = pagerRef.get()?.adapter
        itemCount = adapter?.count!!
        pageListener = PageListener()
        pagerRef.get()?.addOnPageChangeListener(pageListener)
        updateAdapter(null, adapter)
        Log.d("main","setViewPager : w = $width h = $height")
        calculatePosition(width,height)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        /*if (parent !is ViewPager) {
            throw IllegalStateException("only ViewPager can be the parent of IndicatorView")
        }*/
    }

    private fun calculatePosition(w: Int, h: Int) {

        if (w ==0 || h == 0) {
            return
        }

        if (itemCount < 0) {
            Log.e("OneLine", "IndicatorView itemCount = $itemCount < 0")
            return
        }

        if (h * itemCount >= w) {
            Log.e("OneLine", "IndicatorView h * itemCount = ${h * itemCount} > w = $w!")
        }

        val innerSpace = (w - h * itemCount).toFloat() / (itemCount - 1).toFloat()
        val r = h.toFloat() / 2f
        bgRadius = r
        distance = innerSpace + h
        var inc: Float = 0f
        for (i in 0 until itemCount) {
            val point = PointF()
            inc = r + (i * innerSpace) + 2 * i * r
            point.x = inc
            point.y = r
            pointPositions.add(point)
        }

        movingPoint.set(pointPositions[0])

        bgRect.set(r, h / 4f, w.toFloat() - r, h.toFloat() * 3 / 4)
    }

    private fun updateAdapter(oldAdapter: PagerAdapter?, newAdapter: PagerAdapter?) {
        if (oldAdapter != null) {
            oldAdapter.unregisterDataSetObserver(pageListener)
        }

        if (newAdapter != null) {
            newAdapter.registerDataSetObserver(pageListener)
            adapterRef = WeakReference(newAdapter)
        }

        if (pagerRef.get() != null) {
            // 需要重新设计指示器的位置
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (this::pagerRef.isInitialized) {
            pagerRef.get()?.removeOnPageChangeListener(pageListener)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        viewPort.set(left, top, right, bottom)
        Log.d("main","onLayout!")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d("main","onSizeChanged!!")
        calculatePosition(w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (itemCount < 0) {
            Log.e("OneLine", "IndicatorView itemCount = $itemCount < 0")
            return
        }

        // 画轮廓
        //canvas!!.drawRect(viewPort, circlePaint)

        // 画背景
        canvas!!.drawRect(bgRect, bgPaint)
        for (point in pointPositions) {
            canvas.drawCircle(point.x, point.y, point.y, bgPaint)
        }

        // 画移动的点
        canvas.drawCircle(movingPoint.x, movingPoint.y, bgRadius, movingPaint)
    }

    private inner class PageListener : ViewPager.OnPageChangeListener, DataSetObserver() {

        // OnPageChangeListener

        override fun onPageScrollStateChanged(state: Int) {
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            // 当滚动ViewPager页面开始滚动的时候，需要重新计算移动点的位置,因为滚动是横向的，因此
            // y值并不会发生改变
            //Log.d("main","onPageScrolled position = $position positionOffset : $positionOffset positionOffsetPixels : $positionOffsetPixels")

            val offset = positionOffset * distance
            val beginPoint = pointPositions[position]
            movingPoint.set(beginPoint)
            movingPoint.x += offset
            invalidate()
        }

        override fun onPageSelected(position: Int) {
        }

        override fun onChanged() {
            super.onChanged()
        }
    }
}