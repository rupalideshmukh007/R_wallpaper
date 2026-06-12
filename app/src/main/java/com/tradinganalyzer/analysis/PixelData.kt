package com.tradinganalyzer.analysis

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PixelData — ChartAnalyzer → PatternEngine ला pass होतो
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
data class PixelData(
    val greenPixels: Long,
    val redPixels: Long,
    val gp: Float,              // green % (0–100)
    val rp: Float,              // red   % (0–100)

    // Chart zones (top / bottom half)
    val topGreen: Long,    val topRed: Long,
    val bottomGreen: Long, val bottomRed: Long,

    // Chart thirds (left / mid / right)
    val leftGreen: Long,   val leftRed: Long,
    val midGreen: Long,    val midRed: Long,
    val rightGreen: Long,  val rightRed: Long,

    // 4 quadrants (top-left, top-right, bottom-left, bottom-right)
    val tlGreen: Long,  val tlRed: Long,
    val trGreen: Long,  val trRed: Long,
    val blGreen: Long,  val blRed: Long,
    val brGreen: Long,  val brRed: Long,

    val colorRatio: Float       // colored pixels / total pixels
)
