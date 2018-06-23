package com.llx278.oneline.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint


object BitmapProvider {

    // 150 * 150
    private val paths1 = floatArrayOf(30f, 30f, 120f, 30f, 120f, 30f, 120f, 120f, 120f, 120f, 30f, 120f, 30f, 120f, 30f, 30f)

    // 150 * 150
    private val paths2 = floatArrayOf(100f, 50f, 200f, 50f, 200f, 50f, 250f, 150f, 250f, 150f, 200f, 250f, 200f, 250f, 100f, 250f,100f,250f,50f,150f,50f,150f,100f,50f)

    // 150 * 150
    private val paths3 = floatArrayOf(150f,50f,250f,250f,250f,250f,50f,250f,50f,250f,150f,50f)

    init {

    }

    fun provideBitmaps(): Array<Bitmap> {

        // 创建3个bitmap
        val linePaint = Paint()
        linePaint.isAntiAlias = true
        linePaint.color = Color.parseColor("#3F51B5")
        linePaint.strokeWidth = 10f
        linePaint.style = Paint.Style.STROKE
        val circlePaint = Paint()
        circlePaint.isAntiAlias = true
        circlePaint.color = Color.parseColor("#228B22")
        circlePaint.style = Paint.Style.FILL

        val bitmap1 = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val canvas1 = Canvas(bitmap1)
        canvas1.drawLines(paths1, linePaint)
        for (i in paths1.indices step 2) {
            canvas1.drawCircle(paths1[i], paths1[i + 1], 15f, circlePaint)
        }

        val bitmap2 = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val canvas2 = Canvas(bitmap2)
        canvas2.drawLines(paths2, linePaint)
        for (i in paths2.indices step 2) {
            canvas2.drawCircle(paths2[i], paths2[i + 1], 15f, circlePaint)
        }

        val bitmap3 = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val canvas3 = Canvas(bitmap3)
        canvas3.drawLines(paths3, linePaint)
        for (i in paths3.indices step 2) {
            canvas3.drawCircle(paths3[i], paths3[i + 1], 15f, circlePaint)
        }

        return arrayOf(bitmap1, bitmap2, bitmap3)

    }
}