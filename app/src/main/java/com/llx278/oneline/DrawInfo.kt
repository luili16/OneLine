package com.llx278.oneline

data class PointInfo(
        /**
         * 点的坐标
         */
        val point: Point,
        /**
         * 点的颜色
         */
        val color: Int,
        /**
         * 点的半径
         */
        val radius: Float,
        /**
         * 描述信息
         */
        val des: String?)

data class LineInfo(
        /**
         * 起始位置
         */
        val startPoint: Point,
        /**
         * 结束位置
         */
        val endPoint: Point,
        /**
         * 颜色
         */
        val color: Int,
        /**
         * 宽度
         */
        val strokeWidth : Float)

data class LinesInfo(val pointInfos : ArrayList<PointInfo>, val color : Int, val strokeWidth : Float)

data class DrawInfo(val pointInfo: ArrayList<PointInfo>, val lineInfo: ArrayList<LineInfo>)