package com.tradinganalyzer.analysis

data class PixelData(
    val greenPixels: Long,
    val redPixels: Long,
    val gp: Float,
    val rp: Float,
    val topGreen: Long,     val topRed: Long,
    val bottomGreen: Long,  val bottomRed: Long,
    val leftGreen: Long,    val leftRed: Long,
    val midGreen: Long,     val midRed: Long,
    val rightGreen: Long,   val rightRed: Long,
    val tlGreen: Long,      val tlRed: Long,
    val trGreen: Long,      val trRed: Long,
    val blGreen: Long,      val blRed: Long,
    val brGreen: Long,      val brRed: Long,
    val colorRatio: Float
)

data class PatternResult(
    val patternName: String,
    val patternType: String,
    val confidence: Int,
    val strength: String,
    val priorityScore: Int
)

object PatternEngine {

    fun detect(d: PixelData): List<PatternResult> {
        val results = mutableListOf<PatternResult>()
        val diff    = Math.abs(d.gp - d.rp)

        val leftTotal  = (d.leftGreen  + d.leftRed ).coerceAtLeast(1).toFloat()
        val midTotal   = (d.midGreen   + d.midRed  ).coerceAtLeast(1).toFloat()
        val rightTotal = (d.rightGreen + d.rightRed).coerceAtLeast(1).toFloat()
        val topTotal   = (d.topGreen   + d.topRed  ).coerceAtLeast(1).toFloat()
        val botTotal   = (d.bottomGreen+ d.bottomRed).coerceAtLeast(1).toFloat()

        val lGRatio = d.leftGreen   / leftTotal
        val mGRatio = d.midGreen    / midTotal
        val rGRatio = d.rightGreen  / rightTotal
        val tGRatio = d.topGreen    / topTotal
        val bGRatio = d.bottomGreen / botTotal

        // ── Doji
        if (diff < 5f) {
            val c = (95 - diff * 8).toInt().coerceIn(60, 90)
            results += PatternResult("Doji", "Neutral", c, str(c), 45)
        }
        // ── Dragonfly Doji
        if (diff < 8f && bGRatio > 0.55f && tGRatio < 0.45f)
            results += PatternResult("Dragonfly Doji", "Bullish", 67, "Medium", 52)
        // ── Gravestone Doji
        if (diff < 8f && tGRatio < 0.40f && bGRatio < 0.40f && d.topRed > d.topGreen)
            results += PatternResult("Gravestone Doji", "Bearish", 67, "Medium", 52)
        // ── Long-Legged Doji
        if (diff < 6f && d.colorRatio > 0.08f)
            results += PatternResult("Long-Legged Doji", "Neutral", 63, "Weak", 40)
        // ── Hammer
        val isHammer = d.topRed > d.topGreen * 1.4f && d.bottomGreen > d.bottomRed * 1.3f && diff < 22f
        if (isHammer) {
            val c = (62 + diff / 2).toInt().coerceIn(62, 83)
            results += PatternResult("Hammer", "Bullish", c, str(c), 65)
        }
        // ── Inverted Hammer
        val isInvHammer = d.topGreen > d.topRed * 1.4f && d.bottomRed > d.bottomGreen && diff < 20f
        if (isInvHammer) results += PatternResult("Inverted Hammer", "Bullish", 63, "Medium", 57)
        // ── Shooting Star
        val isShootingStar = d.topGreen > d.bottomGreen * 1.5f && d.rp >= d.gp - 12f
        if (isShootingStar) {
            val c = (62 + diff / 2).toInt().coerceIn(62, 83)
            results += PatternResult("Shooting Star", "Bearish", c, str(c), 65)
        }
        // ── Hanging Man
        val isHangingMan = d.topRed > d.topGreen * 1.35f && d.bottomRed > d.bottomGreen * 1.2f && d.rp > 50f
        if (isHangingMan) results += PatternResult("Hanging Man", "Bearish", 64, "Medium", 62)
        // ── Marubozu Bullish
        val isMarubozuBull = d.gp > 72f && diff > 30f && tGRatio > 0.50f && bGRatio > 0.50f
        if (isMarubozuBull) {
            val c = (66 + (d.gp - 70f) / 10f * 5f).toInt().coerceIn(66, 88)
            results += PatternResult("Marubozu Bullish", "Bullish", c, str(c), 72)
        }
        // ── Marubozu Bearish
        val isMarubozuBear = d.rp > 72f && diff > 30f && tGRatio < 0.45f && bGRatio < 0.45f
        if (isMarubozuBear) {
            val c = (66 + (d.rp - 70f) / 10f * 5f).toInt().coerceIn(66, 88)
            results += PatternResult("Marubozu Bearish", "Bearish", c, str(c), 72)
        }
        // ── Spinning Top
        if (diff < 12f && d.colorRatio > 0.06f && !results.any { it.patternName == "Doji" })
            results += PatternResult("Spinning Top", "Neutral", 58, "Weak", 30)
        // ── Bullish Belt Hold
        if (d.gp > 68f && lGRatio < 0.4f && rGRatio > 0.6f)
            results += PatternResult("Bullish Belt Hold", "Bullish", 66, "Medium", 60)
        // ── Bearish Belt Hold
        if (d.rp > 68f && lGRatio > 0.6f && rGRatio < 0.4f)
            results += PatternResult("Bearish Belt Hold", "Bearish", 66, "Medium", 60)
        // ── Paper Umbrella
        if (isHammer && d.gp > d.rp)
            results += PatternResult("Paper Umbrella", "Bullish", 65, "Medium", 58)
        // ── Shaven Head Candle
        if (d.topGreen > d.topRed * 1.5f && d.gp > 55f && diff < 15f)
            results += PatternResult("Shaven Head Candle", "Bullish", 62, "Medium", 55)

        // ── Bullish Engulfing
        val isBullEngulf = d.gp > 65f && diff > 18f && lGRatio < 0.45f && rGRatio > 0.58f
        if (isBullEngulf) {
            val c = (66 + diff / 3f).toInt().coerceIn(66, 88)
            results += PatternResult("Bullish Engulfing", "Bullish", c, str(c), 80)
        }
        // ── Bearish Engulfing
        val isBearEngulf = d.rp > 65f && diff > 18f && lGRatio > 0.55f && rGRatio < 0.42f
        if (isBearEngulf) {
            val c = (66 + diff / 3f).toInt().coerceIn(66, 88)
            results += PatternResult("Bearish Engulfing", "Bearish", c, str(c), 80)
        }
        // ── Bullish Harami
        val lTotal = d.leftGreen + d.leftRed
        val mTotal = d.midGreen  + d.midRed
        val isBullHarami = lGRatio < 0.42f && mGRatio > 0.52f && mTotal < lTotal
        if (isBullHarami) results += PatternResult("Bullish Harami", "Bullish", 67, "Medium", 67)
        // ── Bearish Harami
        val isBearHarami = lGRatio > 0.58f && mGRatio < 0.48f && mTotal < lTotal
        if (isBearHarami) results += PatternResult("Bearish Harami", "Bearish", 67, "Medium", 67)
        // ── Harami Cross
        if ((isBullHarami || isBearHarami) && diff < 7f) {
            val type = if (d.gp > d.rp) "Bullish" else "Bearish"
            results += PatternResult("Harami Cross", type, 70, "Medium", 68)
        }
        // ── Piercing Line
        if (lGRatio < 0.4f && rGRatio > 0.58f && d.trGreen > d.trRed)
            results += PatternResult("Piercing Line", "Bullish", 71, "Strong", 69)
        // ── Dark Cloud Cover
        if (lGRatio > 0.6f && rGRatio < 0.42f && d.brRed > d.brGreen)
            results += PatternResult("Dark Cloud Cover", "Bearish", 71, "Strong", 69)
        // ── Tweezer Bottom
        if (bGRatio > 0.55f && d.blGreen > d.blRed && d.brGreen > d.brRed)
            results += PatternResult("Tweezer Bottom", "Bullish", 65, "Medium", 63)
        // ── Tweezer Top
        if (tGRatio < 0.44f && d.tlRed > d.tlGreen && d.trRed > d.trGreen)
            results += PatternResult("Tweezer Top", "Bearish", 65, "Medium", 63)
        // ── Bullish Kicker
        if (lGRatio < 0.35f && rGRatio > 0.65f && diff < 10f)
            results += PatternResult("Bullish Kicker", "Bullish", 76, "Strong", 74)
        // ── Bearish Kicker
        if (lGRatio > 0.65f && rGRatio < 0.35f && diff < 10f)
            results += PatternResult("Bearish Kicker", "Bearish", 76, "Strong", 74)
        // ── Matching Low
        if (d.blRed > d.blGreen && d.brGreen > d.brRed && bGRatio > 0.5f)
            results += PatternResult("Matching Low", "Bullish", 63, "Medium", 58)
        // ── Matching High
        if (d.tlGreen > d.tlRed && d.trRed > d.trGreen && tGRatio < 0.5f)
            results += PatternResult("Matching High", "Bearish", 63, "Medium", 58)
        // ── Meeting Lines
        if (diff < 8f && (lGRatio > 0.6f || lGRatio < 0.4f)) {
            val type = if (d.gp > d.rp) "Bullish" else "Bearish"
            results += PatternResult("Meeting Lines", type, 61, "Weak", 50)
        }
        // ── Homing Pigeon
        if (lGRatio < 0.42f && mGRatio > lGRatio && mGRatio < 0.52f)
            results += PatternResult("Homing Pigeon", "Bullish", 62, "Medium", 55)

        // ── Morning Star
        val isMorningStar = lGRatio < 0.45f && mGRatio > 0.48f && rGRatio > 0.55f
        if (isMorningStar) results += PatternResult("Morning Star", "Bullish", 77, "Strong", 83)
        // ── Evening Star
        val isEveningStar = lGRatio > 0.55f && mGRatio < 0.52f && rGRatio < 0.45f
        if (isEveningStar) results += PatternResult("Evening Star", "Bearish", 77, "Strong", 83)
        // ── Morning Doji Star
        if (isMorningStar && diff < 6f) results += PatternResult("Morning Doji Star", "Bullish", 82, "Strong", 87)
        // ── Evening Doji Star
        if (isEveningStar && diff < 6f) results += PatternResult("Evening Doji Star", "Bearish", 82, "Strong", 87)
        // ── Three White Soldiers
        val isThreeWhite = lGRatio > 0.52f && mGRatio > lGRatio && rGRatio > mGRatio && d.gp > 62f
        if (isThreeWhite) {
            val c = (72 + (d.gp - 62f) / 2f).toInt().coerceIn(72, 91)
            results += PatternResult("Three White Soldiers", "Bullish", c, str(c), 90)
        }
        // ── Three Black Crows
        val isThreeCrows = lGRatio < 0.48f && mGRatio < lGRatio && rGRatio < mGRatio && d.rp > 62f
        if (isThreeCrows) {
            val c = (72 + (d.rp - 62f) / 2f).toInt().coerceIn(72, 91)
            results += PatternResult("Three Black Crows", "Bearish", c, str(c), 90)
        }
        // ── Three Inside Up
        if (lGRatio < 0.43f && mGRatio > 0.52f && rGRatio > 0.58f)
            results += PatternResult("Three Inside Up", "Bullish", 73, "Strong", 76)
        // ── Three Inside Down
        if (lGRatio > 0.57f && mGRatio < 0.48f && rGRatio < 0.42f)
            results += PatternResult("Three Inside Down", "Bearish", 73, "Strong", 76)
        // ── Three Outside Up
        if (lGRatio < 0.4f && isBullEngulf && rGRatio > 0.55f)
            results += PatternResult("Three Outside Up", "Bullish", 75, "Strong", 78)
        // ── Three Outside Down
        if (lGRatio > 0.6f && isBearEngulf && rGRatio < 0.45f)
            results += PatternResult("Three Outside Down", "Bearish", 75, "Strong", 78)
        // ── Three Stars in the South
        if (isThreeCrows && d.bottomGreen > d.bottomRed)
            results += PatternResult("Three Stars in the South", "Bullish", 70, "Medium", 72)
        // ── Advance Block
        if (lGRatio > 0.55f && mGRatio > 0.5f && rGRatio > 0.45f && rGRatio < mGRatio)
            results += PatternResult("Advance Block", "Bearish", 65, "Medium", 62)
        // ── Deliberation Pattern
        if (isThreeWhite && diff < 10f && rGRatio < mGRatio)
            results += PatternResult("Deliberation Pattern", "Neutral", 63, "Weak", 55)
        // ── Tri-Star
        if (diff < 5f && lGRatio in 0.44f..0.56f && mGRatio in 0.44f..0.56f && rGRatio in 0.44f..0.56f) {
            val type = if (d.gp >= d.rp) "Bullish" else "Bearish"
            results += PatternResult("Tri-Star", type, 72, "Strong", 74)
        }
        // ── Upside Gap Two Crows
        if (d.trRed > d.trGreen && d.brRed > d.brGreen && tGRatio > 0.5f && lGRatio > 0.5f)
            results += PatternResult("Upside Gap Two Crows", "Bearish", 68, "Medium", 65)

        // ── Double Top
        if (d.tlRed > d.tlGreen && d.trRed > d.trGreen && bGRatio > 0.54f)
            results += PatternResult("Double Top", "Bearish", 71, "Strong", 82)
        // ── Double Bottom
        if (d.blGreen > d.blRed && d.brGreen > d.brRed && tGRatio < 0.46f)
            results += PatternResult("Double Bottom", "Bullish", 71, "Strong", 82)
        // ── Triple Top
        if (d.tlRed > d.tlGreen && d.trRed > d.trGreen && mGRatio < 0.45f && d.rp > d.gp)
            results += PatternResult("Triple Top", "Bearish", 73, "Strong", 84)
        // ── Triple Bottom
        if (d.blGreen > d.blRed && d.brGreen > d.brRed && mGRatio > 0.55f && d.gp > d.rp)
            results += PatternResult("Triple Bottom", "Bullish", 73, "Strong", 84)
        // ── Head and Shoulders
        if (tGRatio < 0.42f && d.tlRed > d.tlGreen && d.trRed > d.trGreen && bGRatio > 0.52f)
            results += PatternResult("Head and Shoulders", "Bearish", 76, "Strong", 86)
        // ── Inverse Head and Shoulders
        if (bGRatio > 0.58f && d.blGreen > d.blRed && d.brGreen > d.brRed && tGRatio < 0.46f)
            results += PatternResult("Inverse Head and Shoulders", "Bullish", 76, "Strong", 86)
        // ── Ascending Triangle
        if (lGRatio < 0.45f && rGRatio > 0.56f && d.topRed > d.topGreen * 1.1f)
            results += PatternResult("Ascending Triangle", "Bullish", 69, "Medium", 73)
        // ── Descending Triangle
        if (lGRatio > 0.55f && rGRatio < 0.44f && d.bottomRed > d.bottomGreen * 1.1f)
            results += PatternResult("Descending Triangle", "Bearish", 69, "Medium", 73)
        // ── Symmetrical Triangle
        if (Math.abs(lGRatio - 0.5f) < 0.1f && Math.abs(rGRatio - 0.5f) < 0.1f && diff < 12f)
            results += PatternResult("Symmetrical Triangle", "Neutral", 60, "Medium", 56)
        // ── Bull Flag
        if (lGRatio > 0.55f && Math.abs(lGRatio - rGRatio) < 0.1f && d.gp > d.rp)
            results += PatternResult("Bull Flag", "Bullish", 67, "Medium", 68)
        // ── Bear Flag
        if (lGRatio < 0.45f && Math.abs(lGRatio - rGRatio) < 0.1f && d.rp > d.gp)
            results += PatternResult("Bear Flag", "Bearish", 67, "Medium", 68)
        // ── Bull Pennant
        if (lGRatio > 0.55f && diff < 10f && d.colorRatio > 0.08f && d.gp > d.rp)
            results += PatternResult("Bull Pennant", "Bullish", 65, "Medium", 64)
        // ── Bear Pennant
        if (lGRatio < 0.45f && diff < 10f && d.colorRatio > 0.08f && d.rp > d.gp)
            results += PatternResult("Bear Pennant", "Bearish", 65, "Medium", 64)
        // ── Rising Wedge
        if (tGRatio > 0.55f && bGRatio > 0.5f && rGRatio < lGRatio)
            results += PatternResult("Rising Wedge", "Bearish", 67, "Medium", 70)
        // ── Falling Wedge
        if (tGRatio < 0.45f && bGRatio < 0.5f && rGRatio > lGRatio)
            results += PatternResult("Falling Wedge", "Bullish", 67, "Medium", 70)
        // ── Channel Up
        if (tGRatio > 0.52f && bGRatio > 0.52f && lGRatio < rGRatio)
            results += PatternResult("Channel Up", "Bullish", 64, "Medium", 60)
        // ── Channel Down
        if (tGRatio < 0.48f && bGRatio < 0.48f && lGRatio > rGRatio)
            results += PatternResult("Channel Down", "Bearish", 64, "Medium", 60)
        // ── Cup and Handle
        if (d.blGreen > d.blRed && d.brGreen > d.brRed && mGRatio < 0.48f && d.trGreen > d.trRed)
            results += PatternResult("Cup and Handle", "Bullish", 73, "Strong", 78)
        // ── Inverse Cup and Handle
        if (d.tlRed > d.tlGreen && d.trRed > d.trGreen && mGRatio > 0.52f && d.brRed > d.brGreen)
            results += PatternResult("Inverse Cup and Handle", "Bearish", 71, "Medium", 76)
        // ── Rectangle
        if (Math.abs(lGRatio - rGRatio) < 0.08f && diff < 15f)
            results += PatternResult("Rectangle", "Neutral", 58, "Weak", 42)

        // ── Breakout
        if (d.colorRatio > 0.12f && d.gp > d.rp && diff > 20f)
            results += PatternResult("Breakout", "Bullish", 74, "Strong", 77)
        // ── Breakdown
        if (d.colorRatio > 0.12f && d.rp > d.gp && diff > 20f)
            results += PatternResult("Breakdown", "Bearish", 74, "Strong", 77)
        // ── Support Bounce
        if (bGRatio > 0.58f && d.gp > d.rp)
            results += PatternResult("Support Bounce", "Bullish", 69, "Medium", 71)
        // ── Resistance Rejection
        if (tGRatio < 0.42f && d.rp > d.gp)
            results += PatternResult("Resistance Rejection", "Bearish", 69, "Medium", 71)
        // ── Trendline Breakout
        if (lGRatio < 0.48f && rGRatio > 0.58f && d.colorRatio > 0.09f)
            results += PatternResult("Trendline Breakout", "Bullish", 70, "Strong", 72)
        // ── Trendline Breakdown
        if (lGRatio > 0.52f && rGRatio < 0.42f && d.colorRatio > 0.09f)
            results += PatternResult("Trendline Breakdown", "Bearish", 70, "Strong", 72)
        // ── Retest Confirmation
        if (diff < 10f && d.colorRatio > 0.07f && (d.blGreen > d.blRed || d.brGreen > d.brRed))
            results += PatternResult("Retest Confirmation", "Bullish", 66, "Medium", 65)
        // ── Gap Up Breakout
        if (d.topGreen > d.topRed * 1.5f && d.colorRatio > 0.10f && d.gp > d.rp)
            results += PatternResult("Gap Up Breakout", "Bullish", 75, "Strong", 79)
        // ── Gap Down Breakdown
        if (d.topRed > d.topGreen * 1.5f && d.colorRatio > 0.10f && d.rp > d.gp)
            results += PatternResult("Gap Down Breakdown", "Bearish", 75, "Strong", 79)

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
            "Doji"          to listOf("Doji","Dragonfly Doji","Gravestone Doji","Long-Legged Doji","Tri-Star"),
            "Hammer"        to listOf("Hammer","Inverted Hammer","Paper Umbrella","Shaven Head Candle"),
            "Engulfing"     to listOf("Bullish Engulfing","Three Outside Up"),
            "EngulfingBear" to listOf("Bearish Engulfing","Three Outside Down"),
            "MorningStar"   to listOf("Morning Star","Morning Doji Star"),
            "EveningStar"   to listOf("Evening Star","Evening Doji Star"),
            "ThreeWhite"    to listOf("Three White Soldiers","Three Inside Up","Advance Block"),
            "ThreeCrows"    to listOf("Three Black Crows","Three Inside Down","Three Stars in the South"),
            "DoubleTop"     to listOf("Double Top","Triple Top","Head and Shoulders"),
            "DoubleBot"     to listOf("Double Bottom","Triple Bottom","Inverse Head and Shoulders"),
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
            val typeIcon = when (p.patternType) { "Bullish" -> "🟢"; "Bearish" -> "🔴"; else -> "🟡" }
            sb.appendLine("${i + 1}. $typeIcon ${p.patternName}")
            sb.appendLine("   Type: ${p.patternType}  |  Confidence: ${p.confidence}%")
            sb.appendLine("   Strength: ${p.strength}  |  Priority: ${p.priorityScore}")
            if (i < patterns.take(topN).size - 1) sb.appendLine("   ─────────────────────")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        return sb.toString()
    }
}
