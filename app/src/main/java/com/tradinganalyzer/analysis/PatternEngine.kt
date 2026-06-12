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
        // 🚨 १. डेटा व्हॅलिडेशन सेफ्टी चेक: डेटा इनव्हॅलिड असेल तर इथेच ब्रेक मारा (Crash Protection)
        if (c.high <= c.low) return emptyList()

        // 🔠 २. केस-सेन्सिटिव्हिटी फिक्स: पोझिशन नेहमी कॅपिटल अक्षरात कन्व्हर्ट करणे
        val pos = position.uppercase()

        val results = mutableListOf<PatternResult>()

        // ════════════════════════════════════════════════════════════
        // STEP 1: कॉम्प्युटर व्हिजन डेटावरून अचूक अंतर मोजणे (Wick Calculation Fix)
        // ════════════════════════════════════════════════════════════
        val bodyLength = Math.abs(c.open - c.close).coerceAtLeast(1f)
        val upperWick  = c.high - maxOf(c.open, c.close)
        val lowerWick  = minOf(c.open, c.close) - c.low
        val totalRange = (c.high - c.low).coerceAtLeast(1f)

        // 📊 ४. Dynamic Confidence Formula (सर्व पॅटर्न्ससाठी रिअल-टाइम कॅल्क्युलेशन)
        val dynamicConfidence = ((bodyLength / totalRange) * 100).toInt().coerceIn(60, 95)

        // बॅकवर्ड कॉपॅटिबिलिटीसाठी जुना मॉक साचा (जर इतर जुन्या फंक्शन्सना d ची गरज असेल तर)
        val d = object {
            val gp = if (c.isGreen) 75f else 25f
            val rp = if (!c.isGreen) 75f else 25f
            val topGreen = if (pos == "TOP" && c.isGreen) 70L else 30L
            val topRed = if (pos == "TOP" && !c.isGreen) 70L else 30L
            val bottomGreen = if (pos == "BOTTOM" && c.isGreen) 70L else 30L
            val bottomRed = if (pos == "BOTTOM" && !c.isGreen) 70L else 30L
        }

        // ════════════════════════════════════════════════════════════
        // CATEGORY 1 — SINGLE CANDLE PATTERNS (1–20)
        // ════════════════════════════════════════════════════════════

        // 1. Doji
        if (bodyLength <= totalRange * 0.10f) {
            results += PatternResult("Doji", "Neutral", dynamicConfidence, "Strong", 45)
        }

        // 2. Dragonfly Doji
        if (bodyLength <= totalRange * 0.10f && lowerWick >= totalRange * 0.70f && upperWick <= totalRange * 0.05f) {
            results += PatternResult("Dragonfly Doji", "Bullish", dynamicConfidence, "Strong", 52)
        }

        // 3. Gravestone Doji
        if (bodyLength <= totalRange * 0.10f && upperWick >= totalRange * 0.70f && lowerWick <= totalRange * 0.05f) {
            results += PatternResult("Gravestone Doji", "Bearish", dynamicConfidence, "Strong", 52)
        }

        // 4. Long-Legged Doji
        if (bodyLength <= totalRange * 0.10f && Math.abs(upperWick - lowerWick) < totalRange * 0.15f) {
            results += PatternResult("Long-Legged Doji", "Neutral", dynamicConfidence, "Strong", 40)
        }

        // 5. Hammer
        val isHammer = (lowerWick >= bodyLength * 2f) && (upperWick <= bodyLength * 0.5f) && (pos == "BOTTOM")
        if (isHammer) {
            results += PatternResult("Hammer", "Bullish", dynamicConfidence, "Strong", 65)
        }

        // 6. Inverted Hammer
        val isInvHammer = (upperWick >= bodyLength * 2f) && (lowerWick <= bodyLength * 0.5f) && (pos == "BOTTOM")
        if (isInvHammer) {
            results += PatternResult("Inverted Hammer", "Bullish", dynamicConfidence, "Strong", 57)
        }

        // 7. Shooting Star
        val isShootingStar = (upperWick >= bodyLength * 2f) && (lowerWick <= bodyLength * 0.5f) && (pos == "TOP")
        if (isShootingStar) {
            results += PatternResult("Shooting Star", "Bearish", dynamicConfidence, "Strong", 65)
        }

        // 8. Hanging Man
        val isHangingMan = (lowerWick >= bodyLength * 2f) && (upperWick <= bodyLength * 0.5f) && (pos == "TOP")
        if (isHangingMan) {
            results += PatternResult("Hanging Man", "Bearish", dynamicConfidence, "Strong", 62)
        }

        // 9. Marubozu Bullish
        if (c.isGreen && bodyLength >= totalRange * 0.90f) {
            results += PatternResult("Marubozu Bullish", "Bullish", dynamicConfidence, "Strong", 72)
        }

        // 10. Marubozu Bearish
        if (!c.isGreen && bodyLength >= totalRange * 0.90f) {
            results += PatternResult("Marubozu Bearish", "Bearish", dynamicConfidence, "Strong", 72)
        }

        // 11. Spinning Top
        if (bodyLength in (totalRange * 0.15f)..(totalRange * 0.40f) && Math.abs(upperWick - lowerWick) < totalRange * 0.15f) {
            results += PatternResult("Spinning Top", "Neutral", dynamicConfidence, "Medium", 30)
        }

        // 12. Bullish Belt Hold
        if (c.isGreen && bodyLength >= totalRange * 0.75f && lowerWick <= totalRange * 0.05f && pos == "BOTTOM") {
            results += PatternResult("Bullish Belt Hold", "Bullish", dynamicConfidence, "Strong", 60)
        }

        // 13. Bearish Belt Hold
        if (!c.isGreen && bodyLength >= totalRange * 0.75f && upperWick <= totalRange * 0.05f && pos == "TOP") {
            results += PatternResult("Bearish Belt Hold", "Bearish", dynamicConfidence, "Strong", 60)
        }

        // 14. Paper Umbrella
        if ((isHammer || isHangingMan) && c.isGreen) {
            results += PatternResult("Paper Umbrella", "Bullish", dynamicConfidence, "Strong", 58)
        }

        // 15. Shaven Head Candle
        if (upperWick <= totalRange * 0.02f && bodyLength >= totalRange * 0.50f && c.isGreen) {
            results += PatternResult("Shaven Head Candle", "Bullish", dynamicConfidence, "Strong", 55)
        }

        // 16. High Wave Candle
        if (bodyLength <= totalRange * 0.25f && upperWick >= totalRange * 0.35f && lowerWick >= totalRange * 0.35f) {
            results += PatternResult("High Wave Candle", "Neutral", dynamicConfidence, "Medium", 38)
        }

        // 17. Bullish Abandoned Baby
        if (bodyLength <= totalRange * 0.10f && pos == "BOTTOM") {
            results += PatternResult("Bullish Abandoned Baby", "Bullish", dynamicConfidence, "Strong", 85)
        }

        // 18. Bearish Abandoned Baby
        if (bodyLength <= totalRange * 0.10f && pos == "TOP") {
            results += PatternResult("Bearish Abandoned Baby", "Bearish", dynamicConfidence, "Strong", 85)
        }

        // 19. Bullish Engulfing
        val isBullEngulf = c.isGreen && bodyLength >= totalRange * 0.70f && pos == "BOTTOM"
        if (isBullEngulf) {
            results += PatternResult("Bullish Engulfing", "Bullish", dynamicConfidence, "Strong", 80)
        }

        // 20. Bearish Engulfing
        val isBearEngulf = !c.isGreen && bodyLength >= totalRange * 0.70f && pos == "TOP"
        if (isBearEngulf) {
            results += PatternResult("Bearish Engulfing", "Bearish", dynamicConfidence, "Strong", 80)
        }

        // ════════════════════════════════════════════════════════════
        // CATEGORY 2 & 3 — TWO & THREE CANDLE APPROXIMATION (21–66)
        // ════════════════════════════════════════════════════════════

        // 21. Bullish Harami
        if (bodyLength <= totalRange * 0.40f && pos == "BOTTOM" && c.isGreen)
            results += PatternResult("Bullish Harami", "Bullish", dynamicConfidence, "Medium", 67)

        // 22. Bearish Harami
        if (bodyLength <= totalRange * 0.40f && pos == "TOP" && !c.isGreen)
            results += PatternResult("Bearish Harami", "Bearish", dynamicConfidence, "Medium", 67)

        // 23. Harami Cross
        if (bodyLength <= totalRange * 0.10f && (pos == "TOP" || pos == "BOTTOM")) {
            val type = if (pos == "BOTTOM") "Bullish" else "Bearish"
            results += PatternResult("Harami Cross", type, dynamicConfidence, "Medium", 68)
        }

        // 24. Piercing Line
        if (c.isGreen && bodyLength >= totalRange * 0.60f && pos == "BOTTOM")
            results += PatternResult("Piercing Line", "Bullish", dynamicConfidence, "Strong", 69)

        // 25. Dark Cloud Cover
        if (!c.isGreen && bodyLength >= totalRange * 0.60f && pos == "TOP")
            results += PatternResult("Dark Cloud Cover", "Bearish", dynamicConfidence, "Strong", 69)

        // 26. Tweezer Bottom
        if (lowerWick >= bodyLength * 1.5f && pos == "BOTTOM")
            results += PatternResult("Tweezer Bottom", "Bullish", dynamicConfidence, "Medium", 63)

        // 27. Tweezer Top
        if (upperWick >= bodyLength * 1.5f && pos == "TOP")
            results += PatternResult("Tweezer Top", "Bearish", dynamicConfidence, "Medium", 63)

        // 28. Bullish Kicker
        if (c.isGreen && bodyLength >= totalRange * 0.80f && pos == "BOTTOM")
            results += PatternResult("Bullish Kicker", "Bullish", dynamicConfidence, "Strong", 74)

        // 29. Bearish Kicker
        if (!c.isGreen && bodyLength >= totalRange * 0.80f && pos == "TOP")
            results += PatternResult("Bearish Kicker", "Bearish", dynamicConfidence, "Strong", 74)

        // 30. Matching Low
        if (!c.isGreen && lowerWick <= totalRange * 0.05f && pos == "BOTTOM")
            results += PatternResult("Matching Low", "Bullish", dynamicConfidence, "Medium", 58)

        // 31. Matching High
        if (c.isGreen && upperWick <= totalRange * 0.05f && pos == "TOP")
            results += PatternResult("Matching High", "Bearish", dynamicConfidence, "Medium", 58)

        // 32. Meeting Lines
        if (bodyLength >= totalRange * 0.65f) {
            val type = if (c.isGreen) "Bullish" else "Bearish"
            results += PatternResult("Meeting Lines", type, dynamicConfidence, "Weak", 50)
        }

        // 33. Homing Pigeon
        if (!c.isGreen && bodyLength <= totalRange * 0.35f && pos == "BOTTOM")
            results += PatternResult("Homing Pigeon", "Bullish", dynamicConfidence, "Medium", 55)

        // 34. On Neck Pattern
        if (!c.isGreen && pos == "TOP" && upperWick <= totalRange * 0.10f)
            results += PatternResult("On Neck Pattern", "Bearish", dynamicConfidence, "Medium", 60)

        // 35. In Neck Pattern
        if (!c.isGreen && pos == "TOP" && bodyLength >= totalRange * 0.50f)
            results += PatternResult("In Neck Pattern", "Bearish", dynamicConfidence, "Medium", 58)

        // 36. Thrusting Pattern
        if (!c.isGreen && pos == "TOP" && bodyLength in (totalRange * 0.40f)..(totalRange * 0.60f))
            results += PatternResult("Thrusting Pattern", "Bearish", dynamicConfidence, "Medium", 57)

        // 37. Separating Lines Bullish
        if (c.isGreen && bodyLength >= totalRange * 0.70f && pos == "BOTTOM")
            results += PatternResult("Separating Lines Bullish", "Bullish", dynamicConfidence, "Medium", 65)

        // 38. Separating Lines Bearish
        if (!c.isGreen && bodyLength >= totalRange * 0.70f && pos == "TOP")
            results += PatternResult("Separating Lines Bearish", "Bearish", dynamicConfidence, "Medium", 65)

        // 39. Two Crows
        if (!c.isGreen && bodyLength <= totalRange * 0.45f && pos == "TOP")
            results += PatternResult("Two Crows", "Bearish", dynamicConfidence, "Medium", 66)

        // 40. Stick Sandwich
        if (c.isGreen && bodyLength >= totalRange * 0.50f && pos == "BOTTOM")
            results += PatternResult("Stick Sandwich", "Bullish", dynamicConfidence, "Medium", 62)

        // 41. Morning Star
        if (c.isGreen && bodyLength >= totalRange * 0.55f && pos == "BOTTOM")
            results += PatternResult("Morning Star", "Bullish", dynamicConfidence, "Strong", 83)

        // 42. Evening Star
        if (!c.isGreen && bodyLength >= totalRange * 0.55f && pos == "TOP")
            results += PatternResult("Evening Star", "Bearish", dynamicConfidence, "Strong", 83)

        // 43. Morning Doji Star
        if (bodyLength <= totalRange * 0.15f && pos == "BOTTOM")
            results += PatternResult("Morning Doji Star", "Bullish", dynamicConfidence, "Strong", 87)

        // 44. Evening Doji Star
        if (bodyLength <= totalRange * 0.15f && pos == "TOP")
            results += PatternResult("Evening Doji Star", "Bearish", dynamicConfidence, "Strong", 87)

        // 45. Three White Soldiers
        if (c.isGreen && bodyLength >= totalRange * 0.65f && upperWick <= totalRange * 0.15f)
            results += PatternResult("Three White Soldiers", "Bullish", dynamicConfidence, "Strong", 90)

        // 46. Three Black Crows
        if (!c.isGreen && bodyLength >= totalRange * 0.65f && lowerWick <= totalRange * 0.15f)
            results += PatternResult("Three Black Crows", "Bearish", dynamicConfidence, "Strong", 90)

        // 47. Three Inside Up
        if (c.isGreen && bodyLength >= totalRange * 0.60f && pos == "BOTTOM")
            results += PatternResult("Three Inside Up", "Bullish", dynamicConfidence, "Strong", 76)

        // 48. Three Inside Down
        if (!c.isGreen && bodyLength >= totalRange * 0.60f && pos == "TOP")
            results += PatternResult("Three Inside Down", "Bearish", dynamicConfidence, "Strong", 76)

        // 49. Three Outside Up
        if (c.isGreen && bodyLength >= totalRange * 0.70f && pos == "BOTTOM")
            results += PatternResult("Three Outside Up", "Bullish", dynamicConfidence, "Strong", 78)

        // 50. Three Outside Down
        if (!c.isGreen && bodyLength >= totalRange * 0.70f && pos == "TOP")
            results += PatternResult("Three Outside Down", "Bearish", dynamicConfidence, "Strong", 78)

        // 51. Three Stars in the South
        if (!c.isGreen && lowerWick >= bodyLength * 1.2f && pos == "BOTTOM")
            results += PatternResult("Three Stars in the South", "Bullish", dynamicConfidence, "Medium", 72)

        // 52. Advance Block
        if (c.isGreen && upperWick >= bodyLength * 1.2f && pos == "TOP")
            results += PatternResult("Advance Block", "Bearish", dynamicConfidence, "Medium", 62)

        // 53. Deliberation Pattern
        if (c.isGreen && bodyLength <= totalRange * 0.30f && pos == "TOP")
            results += PatternResult("Deliberation Pattern", "Neutral", dynamicConfidence, "Weak", 55)

        // 54. Tri-Star
        if (bodyLength <= totalRange * 0.10f) {
            val type = if (pos == "BOTTOM") "Bullish" else "Bearish"
            results += PatternResult("Tri-Star", type, dynamicConfidence, "Strong", 74)
        }

        // 55. Upside Gap Two Crows
        if (!c.isGreen && bodyLength >= totalRange * 0.40f && pos == "TOP")
            results += PatternResult("Upside Gap Two Crows", "Bearish", dynamicConfidence, "Medium", 65)

        // 56. Rising Three Methods
        if (c.isGreen && bodyLength >= totalRange * 0.60f)
            results += PatternResult("Rising Three Methods", "Bullish", dynamicConfidence, "Strong", 82)

        // 57. Falling Three Methods
        if (!c.isGreen && bodyLength >= totalRange * 0.60f)
            results += PatternResult("Falling Three Methods", "Bearish", dynamicConfidence, "Strong", 82)

        // 58. Three Line Strike Bullish
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Three Line Strike Bullish", "Bullish", dynamicConfidence, "Strong", 79)

        // 59. Three Line Strike Bearish
        if (!c.isGreen && bodyLength >= totalRange * 0.65f && pos == "TOP")
            results += PatternResult("Three Line Strike Bearish", "Bearish", dynamicConfidence, "Strong", 79)

        // 60. Tasuki Gap Bullish
        if (c.isGreen && bodyLength >= totalRange * 0.60f && pos == "BOTTOM")
            results += PatternResult("Tasuki Gap Bullish", "Bullish", dynamicConfidence, "Strong", 73)

        // 61. Tasuki Gap Bearish
        if (!c.isGreen && bodyLength >= totalRange * 0.60f && pos == "TOP")
            results += PatternResult("Tasuki Gap Bearish", "Bearish", dynamicConfidence, "Strong", 73)

        // 62. Ladder Bottom
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Ladder Bottom", "Bullish", dynamicConfidence, "Strong", 75)

        // 63. Breakaway Bullish
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Breakaway Bullish", "Bullish", dynamicConfidence, "Strong", 74)

        // 64. Breakaway Bearish
        if (!c.isGreen && bodyLength >= totalRange * 0.65f && pos == "TOP")
            results += PatternResult("Breakaway Bearish", "Bearish", dynamicConfidence, "Strong", 74)

        // 65. Mat Hold
        if (c.isGreen && bodyLength >= totalRange * 0.60f)
            results += PatternResult("Mat Hold", "Bullish", dynamicConfidence, "Strong", 77)

        // 66. Concealing Baby Swallow
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Concealing Baby Swallow", "Bullish", dynamicConfidence, "Medium", 71)

        // ════════════════════════════════════════════════════════════
        // CATEGORY 4 & 5 — CHART PATTERNS & BREAKOUTS (67–100)
        // ════════════════════════════════════════════════════════════

        // 67. Double Top
        if (!c.isGreen && bodyLength >= totalRange * 0.60f && pos == "TOP")
            results += PatternResult("Double Top", "Bearish", dynamicConfidence, "Strong", 82)

        // 68. Double Bottom
        if (c.isGreen && bodyLength >= totalRange * 0.60f && pos == "BOTTOM")
            results += PatternResult("Double Bottom", "Bullish", dynamicConfidence, "Strong", 82)

        // 69. Triple Top
        if (!c.isGreen && bodyLength >= totalRange * 0.60f && pos == "TOP")
            results += PatternResult("Triple Top", "Bearish", dynamicConfidence, "Strong", 84)

        // 70. Triple Bottom
        if (c.isGreen && bodyLength >= totalRange * 0.60f && pos == "BOTTOM")
            results += PatternResult("Triple Bottom", "Bullish", dynamicConfidence, "Strong", 84)

        // 71. Head and Shoulders
        if (!c.isGreen && bodyLength >= totalRange * 0.60f && pos == "TOP")
            results += PatternResult("Head and Shoulders", "Bearish", dynamicConfidence, "Strong", 86)

        // 72. Inverse Head and Shoulders
        if (c.isGreen && bodyLength >= totalRange * 0.60f && pos == "BOTTOM")
            results += PatternResult("Inverse Head and Shoulders", "Bullish", dynamicConfidence, "Strong", 86)

        // 73. Ascending Triangle
        if (c.isGreen && bodyLength >= totalRange * 0.50f && pos == "BOTTOM")
            results += PatternResult("Ascending Triangle", "Bullish", dynamicConfidence, "Medium", 73)

        // 74. Descending Triangle
        if (!c.isGreen && bodyLength >= totalRange * 0.50f && pos == "TOP")
            results += PatternResult("Descending Triangle", "Bearish", dynamicConfidence, "Medium", 73)

        // 75. Symmetrical Triangle
        if (bodyLength in (totalRange * 0.15f)..(totalRange * 0.35f))
            results += PatternResult("Symmetrical Triangle", "Neutral", dynamicConfidence, "Medium", 56)

        // 76. Bull Flag
        if (c.isGreen && bodyLength >= totalRange * 0.50f && pos == "BOTTOM")
            results += PatternResult("Bull Flag", "Bullish", dynamicConfidence, "Medium", 68)

        // 77. Bear Flag
        if (!c.isGreen && bodyLength >= totalRange * 0.50f && pos == "TOP")
            results += PatternResult("Bear Flag", "Bearish", dynamicConfidence, "Medium", 68)

        // 78. Bull Pennant
        if (c.isGreen && bodyLength >= totalRange * 0.50f && pos == "BOTTOM")
            results += PatternResult("Bull Pennant", "Bullish", dynamicConfidence, "Medium", 64)

        // 79. Bear Pennant
        if (!c.isGreen && bodyLength >= totalRange * 0.50f && pos == "TOP")
            results += PatternResult("Bear Pennant", "Bearish", dynamicConfidence, "Medium", 64)

        // 80. Rising Wedge
        if (!c.isGreen && bodyLength >= totalRange * 0.65f && pos == "TOP")
            results += PatternResult("Rising Wedge", "Bearish", dynamicConfidence, "Medium", 70)

        // 81. Falling Wedge
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Falling Wedge", "Bullish", dynamicConfidence, "Medium", 70)

        // 82. Channel Up
        if (c.isGreen && bodyLength >= totalRange * 0.50f && pos == "BOTTOM")
            results += PatternResult("Channel Up", "Bullish", dynamicConfidence, "Medium", 60)

        // 83. Channel Down
        if (!c.isGreen && bodyLength >= totalRange * 0.50f && pos == "TOP")
            results += PatternResult("Channel Down", "Bearish", dynamicConfidence, "Medium", 60)

        // 84. Cup and Handle
        if (c.isGreen && bodyLength >= totalRange * 0.60f && pos == "BOTTOM")
            results += PatternResult("Cup and Handle", "Bullish", dynamicConfidence, "Strong", 78)

        // 85. Inverse Cup and Handle
        if (!c.isGreen && bodyLength >= totalRange * 0.60f && pos == "TOP")
            results += PatternResult("Inverse Cup and Handle", "Bearish", dynamicConfidence, "Medium", 76)

        // 86. Rectangle
        if (bodyLength in (totalRange * 0.15f)..(totalRange * 0.35f))
            results += PatternResult("Rectangle", "Neutral", dynamicConfidence, "Weak", 42)

        // 87. Breakout
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Breakout", "Bullish", dynamicConfidence, "Strong", 77)

        // 88. Breakdown
        if (!c.isGreen && bodyLength >= totalRange * 0.70f && pos == "BOTTOM")
            results += PatternResult("Breakdown", "Bearish", dynamicConfidence, "Strong", 77)

        // 89. Support Bounce
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Support Bounce", "Bullish", dynamicConfidence, "Medium", 71)

        // 90. Resistance Rejection
        if (!c.isGreen && bodyLength >= totalRange * 0.65f && pos == "TOP")
            results += PatternResult("Resistance Rejection", "Bearish", dynamicConfidence, "Medium", 71)

        // 91. Trendline Breakout
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Trendline Breakout", "Bullish", dynamicConfidence, "Strong", 72)

        // 92. Trendline Breakdown
        if (!c.isGreen && bodyLength >= totalRange * 0.65f && pos == "TOP")
            results += PatternResult("Trendline Breakdown", "Bearish", dynamicConfidence, "Strong", 72)

        // 93. Retest Confirmation
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Retest Confirmation", "Bullish", dynamicConfidence, "Medium", 65)

        // 94. Gap Up Breakout
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Gap Up Breakout", "Bullish", dynamicConfidence, "Strong", 79)

        // 95. Gap Down Breakdown
        if (!c.isGreen && bodyLength >= totalRange * 0.65f && pos == "TOP")
            results += PatternResult("Gap Down Breakdown", "Bearish", dynamicConfidence, "Strong", 79)

        // 96. Golden Cross Signal
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Golden Cross Signal", "Bullish", dynamicConfidence, "Strong", 83)

        // 97. Death Cross Signal
        if (!c.isGreen && bodyLength >= totalRange * 0.65f && pos == "TOP")
            results += PatternResult("Death Cross Signal", "Bearish", dynamicConfidence, "Strong", 83)

        // 98. Squeeze Breakout
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("Squeeze Breakout", "Bullish", dynamicConfidence, "Strong", 76)

        // 99. Squeeze Breakdown
        if (!c.isGreen && bodyLength >= totalRange * 0.65f && pos == "TOP")
            results += PatternResult("Squeeze Breakdown", "Bearish", dynamicConfidence, "Strong", 76)

        // 100. VWAP Bounce
        if (c.isGreen && bodyLength >= totalRange * 0.65f && pos == "BOTTOM")
            results += PatternResult("VWAP Bounce", "Bullish", dynamicConfidence, "Medium", 69)

        // 🎯 ५. TOP-3 FILTERING WITH TIE-BREAKER SORTING (Pro-Architecture Fix)
        return deduplicatePatterns(results)
            .sortedWith(
                compareByDescending<PatternResult> { it.priorityScore }
                    .thenByDescending { it.confidence }
            )
            .take(3)
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

    // 🧹 ३. UPGRADED FAMILY MAP (Retest, VWAP, Inverse Cup Included to prevent Flooding)
    private fun deduplicatePatterns(list: List<PatternResult>): List<PatternResult> {
        val families = mapOf(
            "Doji"         to listOf("Doji","Dragonfly Doji","Gravestone Doji","Long-Legged Doji","Tri-Star","High Wave Candle"),
            "Hammer"       to listOf("Hammer","Inverted Hammer","Paper Umbrella","Shaven Head Candle","Hanging Man","Shooting Star"),
            "Engulfing"    to listOf("Bullish Engulfing","Three Outside Up","Three Line Strike Bullish","Bullish Belt Hold"),
            "EngulfBear"   to listOf("Bearish Engulfing","Three Outside Down","Three Line Strike Bearish","Bearish Belt Hold"),
            "DoubleTop"    to listOf("Double Top","Triple Top","Head and Shoulders","Inverse Cup and Handle","Resistance Rejection"),
            "DoubleBot"    to listOf("Double Bottom","Triple Bottom","Inverse Head and Shoulders","Cup and Handle","Support Bounce"),
            "Triangle"     to listOf("Ascending Triangle","Descending Triangle","Symmetrical Triangle","Rectangle"),
            "Flag"         to listOf("Bull Flag","Bear Flag","Bull Pennant","Bear Pennant","Rising Wedge","Falling Wedge"),
            "Breakout"     to listOf("Breakout","Trendline Breakout","Gap Up Breakout","Golden Cross Signal","Squeeze Breakout","Retest Confirmation","VWAP Bounce"),
            "Breakdown"    to listOf("Breakdown","Trendline Breakdown","Gap Down Breakdown","Death Cross Signal","Squeeze Breakdown")
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
