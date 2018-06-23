package com.llx278.oneline.widget

/**
 * 定义了每个圆点的坐标
 */
class Point(val x: Int, val y: Int) {
    companion object {
        /**
         * 每行的矩形数量
         */
        const val RECT_NUM = 7
    }

    init {
        if (x < 0 || y < 0 || x >= RECT_NUM || y >= RECT_NUM) {
            throw IllegalArgumentException("startX and y must be greater than 0 and less than $RECT_NUM")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }

    override fun toString(): String {
        return "Point(x=$x, y=$y)"
    }
}

data class LinesInfo(val points : ArrayList<Point>)