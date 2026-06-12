package com.tradinganalyzer.analysis

data class PixelData(
    val greenPixels: Long,
    val redPixels: Long,
    val gp: Float,
    val rp: Float,
    val topGreen: Long,    val topRed: Long,
    val bottomGreen: Long, val bottomRed: Long,
    val leftGreen: Long,   val leftRed: Long,
    val midGreen: Long,    val midRed: Long,
    val rightGreen: Long,  val rightRed: Long,
    val tlGreen: Long,     val tlRed: Long,
    val trGreen: Long,     val trRed: Long,
    val blGreen: Long,     val blRed: Long,
    val brGreen: Long,     val brRed: Long,
    val colorRatio: Float
)
