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
        var radius: Float,
        /**
         * 描述信息
         */
        val des: String?)

data class LinesInfo(val pointInfos : ArrayList<PointInfo>, val color : Int, val strokeWidth : Float)
