package com.llx278.oneline.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.Element
import android.support.v8.renderscript.RenderScript
import android.support.v8.renderscript.ScriptIntrinsicBlur

import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.llx278.oneline.R
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class StreamView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * 所有用来作为浮动背景的图片
     */
    private val bitmaps: Array<Bitmap> = BitmapProvider.provideBitmaps()
    private val blurBitmaps : Array<Bitmap>

    private val paint: Paint = Paint()

    private val properties: Array<Property>

    private var timer: Timer? = null

    /**
     * 每隔多少ms更新一次坐标值
     */
    private val basePeriod: Long = 60

    /**
     * 更新一次坐标值的最小值
     */
    private var minUpdateValue: Int = 1

    private val script : RenderScript
    private val inAllocations : Array<Allocation>
    private val outAllocations : Array<Allocation>
    private val blurScript : ScriptIntrinsicBlur

    private val blurFinish : AtomicBoolean = AtomicBoolean(false)
    private val sizeChanged : AtomicBoolean = AtomicBoolean(false)

    private var animate : Boolean = false

    init {

        val maxNum = resources.getInteger(R.integer.max_num_of_float_view)
        val random = Random()
        properties = Array(maxNum) {
            Property(speed = random.nextInt(6) + random.nextFloat(),
                    index = random.nextInt(bitmaps.size))
        }

        blurBitmaps = Array(bitmaps.size) {
            val width = bitmaps[it].width
            val height = bitmaps[it].height
            Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        }

        // 创建模糊背景的脚本
        script = RenderScript.create(context)
        inAllocations = Array(bitmaps.size) {
            Allocation.createFromBitmap(script,bitmaps[it])
        }

        outAllocations = Array(bitmaps.size) {
            Allocation.createFromBitmap(script,blurBitmaps[it])
        }

        blurScript = ScriptIntrinsicBlur.create(script, Element.U8_4(script))
        blurScript.setRadius(16f)
        // 执行模糊操作
        val task = BlurTask()
        task.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 重新计算每个bitmap在x和y的距离，x = 0-w,y = 0-y
        val random = Random()
        for (property in properties) {
            property.x = random.nextInt(w)
            property.y = random.nextInt(h)
        }
        sizeChanged.set(true)
    }

    fun startAnimation(animate : Boolean) {
        this.animate = animate
        if (animate) {
            val timeTask = MyTimeTask()
            timer = Timer()
            timer!!.scheduleAtFixedRate(timeTask, 0, basePeriod)
        } else {
            timer?.cancel()
            timer = null
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        for (property in properties) {
            val bitmap = blurBitmaps[property.index]
            val x = property.x.toFloat()
            val y = property.y.toFloat()
            canvas!!.drawBitmap(bitmap, x, y, paint)
        }
    }


    private inner class MyTimeTask : TimerTask() {
        override fun run() {

            if (blurFinish.get() && sizeChanged.get()) {
                // 根据每个property里面的速度的系数，就可以计算出每bitmap的实际移动距离
                for (property in properties) {
                    // 只改变x值
                    property.x += (minUpdateValue * property.speed).toInt() + 1
                    if (property.x > width) {
                        property.x = -bitmaps[property.index].width
                    }
                }
                postInvalidate()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        timer?.cancel()
    }

    /**
     * 封装了每一张bitmap的位置，速度等相应的属性
     */
    private inner class Property(var x: Int = -1, var y: Int = -1, val speed: Float = 1.0f,
                                 val scale: Float = 1.0f, val index: Int)

    /**
     * 对获得的bitmap进行模糊处理
     */
    private inner class BlurTask : Thread() {

        override fun run() {
            for (i in inAllocations.indices) {
                val inAllocation = inAllocations[i]
                blurScript.setInput(inAllocation)
                val outAllocation = outAllocations[i]
                blurScript.forEach(outAllocation)
                outAllocation.copyTo(blurBitmaps[i])
            }
            blurFinish.set(true)
            postInvalidate()
        }
    }
}