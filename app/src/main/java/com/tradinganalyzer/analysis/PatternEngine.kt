package com.tradinganalyzer.analysis

// १ मिनिट फोकस: कॉम्प्युटर व्हिजनचा नवीन डेटा क्लास जो आपण ChartAnalyzer मध्ये वापरला
data class CandleShapeData(
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val isGreen: Boolean
)

data class PatternResult(
    val patternName: String,
    val patternType: String,
    val confidence: Int,
    val strength: String,
    val priorityScore: Int
)

object PatternEngine {

    // 🟢 बदल: आता आपण ChartAnalyzer कडून आलेला नवीन कँडल डेटा आणि पोझिशन स्वीकारतो
    fun detect(c: CandleShapeData, position: String): List<PatternResult> {
        val results = mutableListOf<PatternResult>()

        // =====================================================================
// 📐 AI VISION: RULE-BASED PATTERN DETECTION (1–20)
// =====================================================================

val bodyLength = Math.abs(c.open - c.close).coerceAtLeast(1f)
val upperWick  = c.high - maxOf(c.open, c.close)
val lowerWick  = minOf(c.open, c.close) - c.low
val totalRange = (c.high - c.low).coerceAtLeast(1f)

// 1. Doji
if (bodyLength <= totalRange * 0.10f) {
    results += PatternResult("Doji", "Neutral", 85, "Strong", 45)
}

// 2. Dragonfly Doji
if (
    bodyLength <= totalRange * 0.10f &&
    lowerWick >= totalRange * 0.70f &&
    upperWick <= totalRange * 0.05f
) {
    results += PatternResult("Dragonfly Doji", "Bullish", 88, "Strong", 52)
}

// 3. Gravestone Doji
if (
    bodyLength <= totalRange * 0.10f &&
    upperWick >= totalRange * 0.70f &&
    lowerWick <= totalRange * 0.05f
) {
    results += PatternResult("Gravestone Doji", "Bearish", 88, "Strong", 52)
}

// 4. Long-Legged Doji
if (
    bodyLength <= totalRange * 0.10f &&
    Math.abs(upperWick - lowerWick) < totalRange * 0.15f
) {
    results += PatternResult("Long-Legged Doji", "Neutral", 82, "Strong", 40)
}

// 5. Hammer
val isHammer =
    (lowerWick >= bodyLength * 2f) &&
    (upperWick <= bodyLength * 0.5f) &&
    (position == "BOTTOM")

if (isHammer) {
    results += PatternResult("Hammer", "Bullish", 86, "Strong", 65)
}

// 6. Inverted Hammer
val isInvHammer =
    (upperWick >= bodyLength * 2f) &&
    (lowerWick <= bodyLength * 0.5f) &&
    (position == "BOTTOM")

if (isInvHammer) {
    results += PatternResult("Inverted Hammer", "Bullish", 86, "Strong", 57)
}

// 7. Shooting Star
val isShootingStar =
    (upperWick >= bodyLength * 2f) &&
    (lowerWick <= bodyLength * 0.5f) &&
    (position == "TOP")

if (isShootingStar) {
    results += PatternResult("Shooting Star", "Bearish", 86, "Strong", 65)
}

// 8. Hanging Man
val isHangingMan =
    (lowerWick >= bodyLength * 2f) &&
    (upperWick <= bodyLength * 0.5f) &&
    (position == "TOP")

if (isHangingMan) {
    results += PatternResult("Hanging Man", "Bearish", 86, "Strong", 62)
}

// 9. Marubozu Bullish
if (
    c.isGreen &&
    bodyLength >= totalRange * 0.90f
) {
    results += PatternResult("Marubozu Bullish", "Bullish", 90, "Strong", 72)
}

// 10. Marubozu Bearish
if (
    !c.isGreen &&
    bodyLength >= totalRange * 0.90f
) {
    results += PatternResult("Marubozu Bearish", "Bearish", 90, "Strong", 72)
}

// 11. Spinning Top
if (
    bodyLength in (totalRange * 0.15f)..(totalRange * 0.40f) &&
    Math.abs(upperWick - lowerWick) < totalRange * 0.15f
) {
    results += PatternResult("Spinning Top", "Neutral", 75, "Medium", 30)
}

// 12. Bullish Belt Hold
if (
    c.isGreen &&
    bodyLength >= totalRange * 0.75f &&
    lowerWick <= totalRange * 0.05f &&
    position == "BOTTOM"
) {
    results += PatternResult("Bullish Belt Hold", "Bullish", 88, "Strong", 60)
}

// 13. Bearish Belt Hold
if (
    !c.isGreen &&
    bodyLength >= totalRange * 0.75f &&
    upperWick <= totalRange * 0.05f &&
    position == "TOP"
) {
    results += PatternResult("Bearish Belt Hold", "Bearish", 88, "Strong", 60)
}

// 14. Paper Umbrella
if (
    (isHammer || isHangingMan) &&
    c.isGreen
) {
    results += PatternResult("Paper Umbrella", "Bullish", 84, "Strong", 58)
}

// 15. Shaven Head Candle
if (
    upperWick <= totalRange * 0.02f &&
    bodyLength >= totalRange * 0.50f &&
    c.isGreen
) {
    results += PatternResult("Shaven Head Candle", "Bullish", 82, "Strong", 55)
}

// 16. High Wave Candle
if (
    bodyLength <= totalRange * 0.25f &&
    upperWick >= totalRange * 0.35f &&
    lowerWick >= totalRange * 0.35f
) {
    results += PatternResult("High Wave Candle", "Neutral", 78, "Medium", 38)
}

// 17. Bullish Abandoned Baby
if (
    bodyLength <= totalRange * 0.10f &&
    position == "BOTTOM"
) {
    results += PatternResult("Bullish Abandoned Baby", "Bullish", 80, "Strong", 85)
}

// 18. Bearish Abandoned Baby
if (
    bodyLength <= totalRange * 0.10f &&
    position == "TOP"
) {
    results += PatternResult("Bearish Abandoned Baby", "Bearish", 80, "Strong", 85)
}

// 19. Bullish Engulfing
val isBullEngulf =
    c.isGreen &&
    bodyLength >= totalRange * 0.70f &&
    position == "BOTTOM"

if (isBullEngulf) {
    results += PatternResult("Bullish Engulfing", "Bullish", 88, "Strong", 80)
}

// 20. Bearish Engulfing
val isBearEngulf =
    !c.isGreen &&
    bodyLength >= totalRange * 0.70f &&
    position == "TOP"

if (isBearEngulf) {
    results += PatternResult("Bearish Engulfing", "Bearish", 88, "Strong", 80)
}
        // 21. Bullish Harami
        val lTotal = d.leftGreen + d.leftRed
        val mTotal = d.midGreen  + d.midRed
        val isBullHarami = lGRatio < 0.42f && mGRatio > 0.52f && mTotal < lTotal
        if (isBullHarami) results += PatternResult("Bullish Harami", "Bullish", 67, "Medium", 67)
        // 22. Bearish Harami
        val isBearHarami = lGRatio > 0.58f && mGRatio < 0.48f && mTotal < lTotal
        if (isBearHarami) results += PatternResult("Bearish Harami", "Bearish", 67, "Medium", 67)
        // 23. Harami Cross
        if ((isBullHarami || isBearHarami) && diff < 7f) {
            val type = if (d.gp > d.rp) "Bullish" else "Bearish"
            results += PatternResult("Harami Cross", type, 70, "Medium", 68)
        }
        // 24. Piercing Line
        if (lGRatio < 0.4f && rGRatio > 0.58f && d.trGreen > d.trRed)
            results += PatternResult("Piercing Line", "Bullish", 71, "Strong", 69)
        // 25. Dark Cloud Cover
        if (lGRatio > 0.6f && rGRatio < 0.42f && d.brRed > d.brGreen)
            results += PatternResult("Dark Cloud Cover", "Bearish", 71, "Strong", 69)
        // 26. Tweezer Bottom
        if (bGRatio > 0.55f && d.blGreen > d.blRed && d.brGreen > d.brRed)
            results += PatternResult("Tweezer Bottom", "Bullish", 65, "Medium", 63)
        // 27. Tweezer Top
        if (tGRatio < 0.44f && d.tlRed > d.tlGreen && d.trRed > d.trGreen)
            results += PatternResult("Tweezer Top", "Bearish", 65, "Medium", 63)
        // 28. Bullish Kicker
        if (lGRatio < 0.35f && rGRatio > 0.65f && diff < 10f)
            results += PatternResult("Bullish Kicker", "Bullish", 76, "Strong", 74)
        // 29. Bearish Kicker
        if (lGRatio > 0.65f && rGRatio < 0.35f && diff < 10f)
            results += PatternResult("Bearish Kicker", "Bearish", 76, "Strong", 74)
        // 30. Matching Low
        if (d.blRed > d.blGreen && d.brGreen > d.brRed && bGRatio > 0.5f)
            results += PatternResult("Matching Low", "Bullish", 63, "Medium", 58)
        // 31. Matching High
        if (d.tlGreen > d.tlRed && d.trRed > d.trGreen && tGRatio < 0.5f)
            results += PatternResult("Matching High", "Bearish", 63, "Medium", 58)
        // 32. Meeting Lines
        if (diff < 8f && (lGRatio > 0.6f || lGRatio < 0.4f)) {
            val type = if (d.gp > d.rp) "Bullish" else "Bearish"
            results += PatternResult("Meeting Lines", type, 61, "Weak", 50)
        }
        // 33. Homing Pigeon
        if (lGRatio < 0.42f && mGRatio > lGRatio && mGRatio < 0.52f)
            results += PatternResult("Homing Pigeon", "Bullish", 62, "Medium", 55)
        // 34. On Neck Pattern  ★NEW
        if (lGRatio < 0.40f && rGRatio > 0.50f && rGRatio < 0.56f && diff < 12f && bGRatio < 0.48f)
            results += PatternResult("On Neck Pattern", "Bearish", 64, "Medium", 60)
        // 35. In Neck Pattern  ★NEW
        if (lGRatio < 0.40f && rGRatio > 0.53f && rGRatio < 0.59f && d.bottomRed > d.bottomGreen)
            results += PatternResult("In Neck Pattern", "Bearish", 63, "Medium", 58)
        // 36. Thrusting Pattern  ★NEW
        if (lGRatio < 0.42f && rGRatio > 0.55f && tGRatio < 0.45f && diff > 10f)
            results += PatternResult("Thrusting Pattern", "Bearish", 62, "Medium", 57)
        // 37. Separating Lines Bullish  ★NEW
        if (lGRatio < 0.42f && rGRatio > 0.60f && diff > 15f)
            results += PatternResult("Separating Lines Bullish", "Bullish", 67, "Medium", 65)
        // 38. Separating Lines Bearish  ★NEW
        if (lGRatio > 0.58f && rGRatio < 0.40f && diff > 15f)
            results += PatternResult("Separating Lines Bearish", "Bearish", 67, "Medium", 65)
        // 39. Two Crows  ★NEW
        if (lGRatio > 0.55f && mGRatio < 0.48f && rGRatio < 0.45f && d.trRed > d.trGreen)
            results += PatternResult("Two Crows", "Bearish", 68, "Medium", 66)
        // 40. Stick Sandwich  ★NEW
        if (lGRatio < 0.45f && mGRatio > 0.55f && rGRatio < 0.45f && bGRatio > 0.5f)
            results += PatternResult("Stick Sandwich", "Bullish", 65, "Medium", 62)

        // ════════════════════════════════════════════════════════════
        // CATEGORY 3 — THREE CANDLE PATTERNS (41–61)
        // ════════════════════════════════════════════════════════════

        // 41. Morning Star
        val isMorningStar = lGRatio < 0.45f && mGRatio > 0.48f && rGRatio > 0.55f
        if (isMorningStar) results += PatternResult("Morning Star", "Bullish", 77, "Strong", 83)
        // 42. Evening Star
        val isEveningStar = lGRatio > 0.55f && mGRatio < 0.52f && rGRatio < 0.45f
        if (isEveningStar) results += PatternResult("Evening Star", "Bearish", 77, "Strong", 83)
        // 43. Morning Doji Star
        if (isMorningStar && diff < 6f)
            results += PatternResult("Morning Doji Star", "Bullish", 82, "Strong", 87)
        // 44. Evening Doji Star
        if (isEveningStar && diff < 6f)
            results += PatternResult("Evening Doji Star", "Bearish", 82, "Strong", 87)
        // 45. Three White Soldiers
        val isThreeWhite = lGRatio > 0.52f && mGRatio > lGRatio && rGRatio > mGRatio && d.gp > 62f
        if (isThreeWhite) {
            val c = (72 + (d.gp - 62f) / 2f).toInt().coerceIn(72, 91)
            results += PatternResult("Three White Soldiers", "Bullish", c, str(c), 90)
        }
        // 46. Three Black Crows
        val isThreeCrows = lGRatio < 0.48f && mGRatio < lGRatio && rGRatio < mGRatio && d.rp > 62f
        if (isThreeCrows) {
            val c = (72 + (d.rp - 62f) / 2f).toInt().coerceIn(72, 91)
            results += PatternResult("Three Black Crows", "Bearish", c, str(c), 90)
        }
        // 47. Three Inside Up
        if (lGRatio < 0.43f && mGRatio > 0.52f && rGRatio > 0.58f)
            results += PatternResult("Three Inside Up", "Bullish", 73, "Strong", 76)
        // 48. Three Inside Down
        if (lGRatio > 0.57f && mGRatio < 0.48f && rGRatio < 0.42f)
            results += PatternResult("Three Inside Down", "Bearish", 73, "Strong", 76)
        // 49. Three Outside Up
        if (lGRatio < 0.4f && isBullEngulf && rGRatio > 0.55f)
            results += PatternResult("Three Outside Up", "Bullish", 75, "Strong", 78)
        // 50. Three Outside Down
        if (lGRatio > 0.6f && isBearEngulf && rGRatio < 0.45f)
            results += PatternResult("Three Outside Down", "Bearish", 75, "Strong", 78)
        // 51. Three Stars in the South
        if (isThreeCrows && d.bottomGreen > d.bottomRed)
            results += PatternResult("Three Stars in the South", "Bullish", 70, "Medium", 72)
        // 52. Advance Block
        if (lGRatio > 0.55f && mGRatio > 0.5f && rGRatio > 0.45f && rGRatio < mGRatio)
            results += PatternResult("Advance Block", "Bearish", 65, "Medium", 62)
        // 53. Deliberation Pattern
        if (isThreeWhite && diff < 10f && rGRatio < mGRatio)
            results += PatternResult("Deliberation Pattern", "Neutral", 63, "Weak", 55)
        // 54. Tri-Star
        if (diff < 5f && lGRatio in 0.44f..0.56f && mGRatio in 0.44f..0.56f && rGRatio in 0.44f..0.56f) {
            val type = if (d.gp >= d.rp) "Bullish" else "Bearish"
            results += PatternResult("Tri-Star", type, 72, "Strong", 74)
        }
        // 55. Upside Gap Two Crows
        if (d.trRed > d.trGreen && d.brRed > d.brGreen && tGRatio > 0.5f && lGRatio > 0.5f)
            results += PatternResult("Upside Gap Two Crows", "Bearish", 68, "Medium", 65)
        // 56. Rising Three Methods  ★NEW
        if (lGRatio > 0.60f && mGRatio < 0.48f && rGRatio > 0.62f && d.gp > 60f)
            results += PatternResult("Rising Three Methods", "Bullish", 76, "Strong", 82)
        // 57. Falling Three Methods  ★NEW
        if (lGRatio < 0.40f && mGRatio > 0.52f && rGRatio < 0.38f && d.rp > 60f)
            results += PatternResult("Falling Three Methods", "Bearish", 76, "Strong", 82)
        // 58. Three Line Strike Bullish  ★NEW
        if (isThreeCrows && rGRatio > 0.65f && diff > 20f)
            results += PatternResult("Three Line Strike Bullish", "Bullish", 74, "Strong", 79)
        // 59. Three Line Strike Bearish  ★NEW
        if (isThreeWhite && rGRatio < 0.35f && diff > 20f)
            results += PatternResult("Three Line Strike Bearish", "Bearish", 74, "Strong", 79)
        // 60. Tasuki Gap Bullish  ★NEW
        if (lGRatio > 0.55f && d.trGreen > d.trRed && rGRatio > 0.50f && d.topGreen > d.topRed)
            results += PatternResult("Tasuki Gap Bullish", "Bullish", 70, "Strong", 73)
        // 61. Tasuki Gap Bearish  ★NEW
        if (lGRatio < 0.45f && d.blRed > d.blGreen && rGRatio < 0.50f && d.bottomRed > d.bottomGreen)
            results += PatternResult("Tasuki Gap Bearish", "Bearish", 70, "Strong", 73)
        // 62. Ladder Bottom  ★NEW
        if (isThreeCrows && d.brGreen > d.brRed && bGRatio > 0.52f)
            results += PatternResult("Ladder Bottom", "Bullish", 72, "Strong", 75)
        // 63. Breakaway Bullish  ★NEW
        if (d.blGreen > d.blRed && d.brGreen > d.brRed && lGRatio < 0.42f && diff > 18f)
            results += PatternResult("Breakaway Bullish", "Bullish", 71, "Strong", 74)
        // 64. Breakaway Bearish  ★NEW
        if (d.tlRed > d.tlGreen && d.trRed > d.trGreen && lGRatio > 0.58f && diff > 18f)
            results += PatternResult("Breakaway Bearish", "Bearish", 71, "Strong", 74)
        // 65. Mat Hold  ★NEW
        if (lGRatio > 0.58f && mGRatio > 0.45f && rGRatio > 0.60f && d.gp > 58f)
            results += PatternResult("Mat Hold", "Bullish", 73, "Strong", 77)
        // 66. Concealing Baby Swallow  ★NEW
        if (lGRatio < 0.42f && mGRatio < 0.45f && bGRatio > 0.55f && d.gp > d.rp)
            results += PatternResult("Concealing Baby Swallow", "Bullish", 70, "Medium", 71)

        // ════════════════════════════════════════════════════════════
        // CATEGORY 4 — CHART PATTERNS (67–86)
        // ════════════════════════════════════════════════════════════

        // 67. Double Top
        if (d.tlRed > d.tlGreen && d.trRed > d.trGreen && bGRatio > 0.54f)
            results += PatternResult("Double Top", "Bearish", 71, "Strong", 82)
        // 68. Double Bottom
        if (d.blGreen > d.blRed && d.brGreen > d.brRed && tGRatio < 0.46f)
            results += PatternResult("Double Bottom", "Bullish", 71, "Strong", 82)
        // 69. Triple Top
        if (d.tlRed > d.tlGreen && d.trRed > d.trGreen && mGRatio < 0.45f && d.rp > d.gp)
            results += PatternResult("Triple Top", "Bearish", 73, "Strong", 84)
        // 70. Triple Bottom
        if (d.blGreen > d.blRed && d.brGreen > d.brRed && mGRatio > 0.55f && d.gp > d.rp)
            results += PatternResult("Triple Bottom", "Bullish", 73, "Strong", 84)
        // 71. Head and Shoulders
        if (tGRatio < 0.42f && d.tlRed > d.tlGreen && d.trRed > d.trGreen && bGRatio > 0.52f)
            results += PatternResult("Head and Shoulders", "Bearish", 76, "Strong", 86)
        // 72. Inverse Head and Shoulders
        if (bGRatio > 0.58f && d.blGreen > d.blRed && d.brGreen > d.brRed && tGRatio < 0.46f)
            results += PatternResult("Inverse Head and Shoulders", "Bullish", 76, "Strong", 86)
        // 73. Ascending Triangle
        if (lGRatio < 0.45f && rGRatio > 0.56f && d.topRed > d.topGreen * 1.1f)
            results += PatternResult("Ascending Triangle", "Bullish", 69, "Medium", 73)
        // 74. Descending Triangle
        if (lGRatio > 0.55f && rGRatio < 0.44f && d.bottomRed > d.bottomGreen * 1.1f)
            results += PatternResult("Descending Triangle", "Bearish", 69, "Medium", 73)
        // 75. Symmetrical Triangle
        if (Math.abs(lGRatio - 0.5f) < 0.1f && Math.abs(rGRatio - 0.5f) < 0.1f && diff < 12f)
            results += PatternResult("Symmetrical Triangle", "Neutral", 60, "Medium", 56)
        // 76. Bull Flag
        if (lGRatio > 0.55f && Math.abs(lGRatio - rGRatio) < 0.1f && d.gp > d.rp)
            results += PatternResult("Bull Flag", "Bullish", 67, "Medium", 68)
        // 77. Bear Flag
        if (lGRatio < 0.45f && Math.abs(lGRatio - rGRatio) < 0.1f && d.rp > d.gp)
            results += PatternResult("Bear Flag", "Bearish", 67, "Medium", 68)
        // 78. Bull Pennant
        if (lGRatio > 0.55f && diff < 10f && d.colorRatio > 0.08f && d.gp > d.rp)
            results += PatternResult("Bull Pennant", "Bullish", 65, "Medium", 64)
        // 79. Bear Pennant
        if (lGRatio < 0.45f && diff < 10f && d.colorRatio > 0.08f && d.rp > d.gp)
            results += PatternResult("Bear Pennant", "Bearish", 65, "Medium", 64)
        // 80. Rising Wedge
        if (tGRatio > 0.55f && bGRatio > 0.5f && rGRatio < lGRatio)
            results += PatternResult("Rising Wedge", "Bearish", 67, "Medium", 70)
        // 81. Falling Wedge
        if (tGRatio < 0.45f && bGRatio < 0.5f && rGRatio > lGRatio)
            results += PatternResult("Falling Wedge", "Bullish", 67, "Medium", 70)
        // 82. Channel Up
        if (tGRatio > 0.52f && bGRatio > 0.52f && lGRatio < rGRatio)
            results += PatternResult("Channel Up", "Bullish", 64, "Medium", 60)
        // 83. Channel Down
        if (tGRatio < 0.48f && bGRatio < 0.48f && lGRatio > rGRatio)
            results += PatternResult("Channel Down", "Bearish", 64, "Medium", 60)
        // 84. Cup and Handle
        if (d.blGreen > d.blRed && d.brGreen > d.brRed && mGRatio < 0.48f && d.trGreen > d.trRed)
            results += PatternResult("Cup and Handle", "Bullish", 73, "Strong", 78)
        // 85. Inverse Cup and Handle
        if (d.tlRed > d.tlGreen && d.trRed > d.trGreen && mGRatio > 0.52f && d.brRed > d.brGreen)
            results += PatternResult("Inverse Cup and Handle", "Bearish", 71, "Medium", 76)
        // 86. Rectangle
        if (Math.abs(lGRatio - rGRatio) < 0.08f && diff < 15f)
            results += PatternResult("Rectangle", "Neutral", 58, "Weak", 42)
        // ════════════════════════════════════════════════════════════
        // CATEGORY 5 — BREAKOUT & MARKET PATTERNS (87–100)
        // ════════════════════════════════════════════════════════════
        // 87. Breakout
        if (d.colorRatio > 0.12f && d.gp > d.rp && diff > 20f)
            results += PatternResult("Breakout", "Bullish", 74, "Strong", 77)
        // 88. Breakdown
        if (d.colorRatio > 0.12f && d.rp > d.gp && diff > 20f)
            results += PatternResult("Breakdown", "Bearish", 74, "Strong", 77)
        // 89. Support Bounce
        if (bGRatio > 0.58f && d.gp > d.rp)
            results += PatternResult("Support Bounce", "Bullish", 69, "Medium", 71)
        // 90. Resistance Rejection
        if (tGRatio < 0.42f && d.rp > d.gp)
            results += PatternResult("Resistance Rejection", "Bearish", 69, "Medium", 71)
        // 91. Trendline Breakout
        if (lGRatio < 0.48f && rGRatio > 0.58f && d.colorRatio > 0.09f)
            results += PatternResult("Trendline Breakout", "Bullish", 70, "Strong", 72)
        // 92. Trendline Breakdown
        if (lGRatio > 0.52f && rGRatio < 0.42f && d.colorRatio > 0.09f)
            results += PatternResult("Trendline Breakdown", "Bearish", 70, "Strong", 72)
        // 93. Retest Confirmation
        if (diff < 10f && d.colorRatio > 0.07f && (d.blGreen > d.blRed || d.brGreen > d.brRed))
            results += PatternResult("Retest Confirmation", "Bullish", 66, "Medium", 65)
        // 94. Gap Up Breakout
        if (d.topGreen > d.topRed * 1.5f && d.colorRatio > 0.10f && d.gp > d.rp)
            results += PatternResult("Gap Up Breakout", "Bullish", 75, "Strong", 79)
        // 95. Gap Down Breakdown
        if (d.topRed > d.topGreen * 1.5f && d.colorRatio > 0.10f && d.rp > d.gp)
            results += PatternResult("Gap Down Breakdown", "Bearish", 75, "Strong", 79)
        // 96. Golden Cross Signal  ★NEW
        if (lGRatio < 0.45f && rGRatio > 0.58f && d.gp > 60f && d.colorRatio > 0.08f && bGRatio > 0.52f)
            results += PatternResult("Golden Cross Signal", "Bullish", 78, "Strong", 83)
        // 97. Death Cross Signal  ★NEW
        if (lGRatio > 0.55f && rGRatio < 0.42f && d.rp > 60f && d.colorRatio > 0.08f && tGRatio < 0.48f)
            results += PatternResult("Death Cross Signal", "Bearish", 78, "Strong", 83)
        // 98. Squeeze Breakout  ★NEW
        if (d.colorRatio < 0.07f && diff < 8f && d.gp > d.rp && bGRatio > 0.52f)
            results += PatternResult("Squeeze Breakout", "Bullish", 72, "Strong", 76)
        // 99. Squeeze Breakdown  ★NEW
        if (d.colorRatio < 0.07f && diff < 8f && d.rp > d.gp && tGRatio < 0.48f)
            results += PatternResult("Squeeze Breakdown", "Bearish", 72, "Strong", 76)
        // 100. VWAP Bounce  ★NEW
        if (bGRatio > 0.55f && mGRatio > 0.52f && d.gp > d.rp && diff < 15f && d.colorRatio > 0.06f)
            results += PatternResult("VWAP Bounce", "Bullish", 68, "Medium", 69)
        return deduplicatePatterns(results).sortedByDescending { it.priorityScore }.take(20)
    }
    fun mergedBonus(patterns: List<PatternResult>): Pair<Int, Int> {
        var buyBonus = 0; var sellBonus = 0
        for (p in patterns) {
            val weight = (p.priorityScore / 12).coerceIn(1, 8)
            when (p.patternType) {
                "Bullish" -> buyBonus  += (p.confidence * weight / 100.0f).toInt().coerceIn(1, 12)
                "Bearish" -> sellBonus += (p.confidence * weight / 100.0f).toInt().coerceIn(1, 12)
            }
        }
        return Pair(buyBonus.coerceIn(0, 40), sellBonus.coerceIn(0, 40))
    }
    private fun str(confidence: Int): String = when {
        confidence >= 75 -> "Strong"
        confidence >= 62 -> "Medium"
        else             -> "Weak"
    }
    private fun deduplicatePatterns(list: List<PatternResult>): List<PatternResult> {
        val families = mapOf(
            "Doji"         to listOf("Doji","Dragonfly Doji","Gravestone Doji",
                                     "Long-Legged Doji","Tri-Star","High Wave Candle"),
            "Hammer"       to listOf("Hammer","Inverted Hammer","Paper Umbrella",
                                     "Shaven Head Candle"),
            "AbandonedB"   to listOf("Bullish Abandoned Baby","Morning Doji Star"),
            "AbandonedBr"  to listOf("Bearish Abandoned Baby","Evening Doji Star"),
            "Engulfing"    to listOf("Bullish Engulfing","Three Outside Up",
                                     "Three Line Strike Bullish"),
            "EngulfBear"   to listOf("Bearish Engulfing","Three Outside Down",
                                     "Three Line Strike Bearish"),
            "MorningStar"  to listOf("Morning Star","Morning Doji Star"),
            "EveningStar"  to listOf("Evening Star","Evening Doji Star"),
            "ThreeWhite"   to listOf("Three White Soldiers","Three Inside Up",
                                     "Advance Block","Rising Three Methods","Mat Hold"),
            "ThreeCrows"   to listOf("Three Black Crows","Three Inside Down",
                                     "Three Stars in the South","Falling Three Methods"),
            "DoubleTop"    to listOf("Double Top","Triple Top","Head and Shoulders"),
            "DoubleBot"    to listOf("Double Bottom","Triple Bottom",
                                     "Inverse Head and Shoulders"),
            "GoldenCross"  to listOf("Golden Cross Signal","Trendline Breakout","Breakout"),
            "DeathCross"   to listOf("Death Cross Signal","Trendline Breakdown","Breakdown"),
            "Squeeze"      to listOf("Squeeze Breakout","Squeeze Breakdown"),
        )
        val keep = list.toMutableList()
        for ((_, members) in families) {
            val inFamily = keep.filter { it.patternName in members }
            if (inFamily.size > 1) {
                val best = inFamily.maxByOrNull { it.priorityScore }!!
                keep.removeAll { it.patternName in members && it.patternName != best.patternName }
            }
        }
        return keep
    }
    fun formatDisplay(patterns: List<PatternResult>, topN: Int = 5): String {
        if (patterns.isEmpty()) return "कोणताही pattern detect झाला नाही."
        val sb = StringBuilder()
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📊 DETECTED PATTERNS")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        patterns.take(topN).forEachIndexed { i, p ->
            val icon = when (p.patternType) { "Bullish" -> "🟢"; "Bearish" -> "🔴"; else -> "🟡" }
            sb.appendLine("${i + 1}. $icon ${p.patternName}")
            sb.appendLine("   Type: ${p.patternType}  |  Confidence: ${p.confidence}%")
            sb.appendLine("   Strength: ${p.strength}  |  Priority: ${p.priorityScore}")
            if (i < patterns.take(topN).size - 1) sb.appendLine("   ─────────────────────")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        return sb.toString()
    }
}
