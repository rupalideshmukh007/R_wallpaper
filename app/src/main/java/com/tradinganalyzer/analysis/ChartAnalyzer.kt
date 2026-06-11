package com.tradinganalyzer.analysis

import android.graphics.Bitmap
import android.graphics.Color

data class AnalysisResult(
    val isValidChart: Boolean,
    val notChartReason: String = "",
    val signal: String = "",
    val confidence: Int = 0,
    val greenPercent: Float = 0f,
    val redPercent: Float = 0f,
    val trend: String = "",
    val volumeInfo: String = "",
    val sentiment: String = "",
    val supportResistance: String = "",
    val summary: String = "",
    val investorInfo: String = "",
    val rsiValue: Int = 50,
    val rsiInfo: String = "",
    val riskLevel: String = "",
    val riskColor: String = "",
    val waitMessage: String = "",
    val keyFactors: String = "",
    val signalReason: String = "",
    val detectedPatterns: List<PatternResult> = emptyList(),
    val patternDisplay: String = ""
)

object ChartAnalyzer {

    fun analyze(bitmap: Bitmap): AnalysisResult {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

        var greenPixels = 0L; var redPixels = 0L
        var skinPixels  = 0L; var totalPixels = 0L; var backgroundPixels = 0L
        var topGreen    = 0L; var topRed    = 0L
        var bottomGreen = 0L; var bottomRed = 0L
        var leftGreen   = 0L; var leftRed   = 0L
        var midGreen    = 0L; var midRed    = 0L
        var rightGreen  = 0L; var rightRed  = 0L
        var tlGreen = 0L; var tlRed = 0L
        var trGreen = 0L; var trRed = 0L
        var blGreen = 0L; var blRed = 0L
        var brGreen = 0L; var brRed = 0L

        val h = scaledBitmap.height
        val w = scaledBitmap.width

        for (x in 0 until w) {
            for (y in 0 until h) {
                val pixel = scaledBitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                if (Color.alpha(pixel) < 50) continue
                totalPixels++
                if ((r + g + b) / 3 > 220) { backgroundPixels++; continue }
                val isSkin = r > 100 && g > 50 && b > 30 && r > g && r > b &&
                             (r - g) in 10..120 && b < 180
                if (isSkin) { skinPixels++; continue }
                val maxC = maxOf(r, g, b)
                val sat  = if (maxC > 0) (maxC - minOf(r, g, b)).toFloat() / maxC else 0f
                if (sat > 0.2f) {
                    val isG = g > r && g > b && g > 60
                    val isR = r > g && r > b && r > 60
                    if (isG) {
                        greenPixels++
                        if (y < h / 2) topGreen++ else bottomGreen++
                        when {
                            x < w / 3     -> leftGreen++
                            x < w * 2 / 3 -> midGreen++
                            else          -> rightGreen++
                        }
                        if      (y < h/2 && x < w/2) tlGreen++
                        else if (y < h/2)             trGreen++
                        else if (x < w/2)             blGreen++
                        else                          brGreen++
                    } else if (isR) {
                        redPixels++
                        if (y < h / 2) topRed++ else bottomRed++
                        when {
                            x < w / 3     -> leftRed++
                            x < w * 2 / 3 -> midRed++
                            else          -> rightRed++
                        }
                        if      (y < h/2 && x < w/2) tlRed++
                        else if (y < h/2)             trRed++
                        else if (x < w/2)             blRed++
                        else                          brRed++
                    }
                }
            }
        }
        scaledBitmap.recycle()

        // ── Validation ────────────────────────────────────────────────
        val analyzable = totalPixels - backgroundPixels
        if (analyzable <= 0) return AnalysisResult(
            isValidChart = false,
            notChartReason = "Image खूप light/blank आहे. Chart screenshot upload करा."
        )
        if (skinPixels.toFloat() / analyzable > 0.15f) return AnalysisResult(
            isValidChart = false,
            notChartReason = "❌ हे Chart नाही!\n\nSelfie किंवा photo दिसत आहे.\nकृपया Zerodha, Upstox, TradingView chart चा screenshot upload करा."
        )
        val coloredPixels = greenPixels + redPixels
        val colorRatio    = coloredPixels.toFloat() / analyzable
        if (colorRatio < 0.03f) return AnalysisResult(
            isValidChart = false,
            notChartReason = "❌ Chart patterns सापडले नाहीत!\nGreen/Red candles असलेला Candlestick chart upload करा."
        )

        val gp   = if (coloredPixels > 0) greenPixels.toFloat() / coloredPixels * 100f else 50f
        val rp   = if (coloredPixels > 0) redPixels.toFloat()   / coloredPixels * 100f else 50f
        val diff = Math.abs(gp - rp)

        // ── Pattern Engine ────────────────────────────────────────────
        val pixelData = PixelData(
            greenPixels = greenPixels, redPixels = redPixels, gp = gp, rp = rp,
            topGreen = topGreen, topRed = topRed,
            bottomGreen = bottomGreen, bottomRed = bottomRed,
            leftGreen = leftGreen, leftRed = leftRed,
            midGreen = midGreen, midRed = midRed,
            rightGreen = rightGreen, rightRed = rightRed,
            tlGreen = tlGreen, tlRed = tlRed,
            trGreen = trGreen, trRed = trRed,
            blGreen = blGreen, blRed = blRed,
            brGreen = brGreen, brRed = brRed,
            colorRatio = colorRatio
        )
        val detectedPatterns                    = PatternEngine.detect(pixelData)
        val (patternBuyBonus, patternSellBonus) = PatternEngine.mergedBonus(detectedPatterns)

        // ── Pixel Scoring ─────────────────────────────────────────────
        var buyScore  = 0
        var sellScore = 0
        val reasons   = mutableListOf<String>()

        if (gp > rp) { buyScore  += diff.toInt().coerceIn(1, 30); reasons.add("Bullish candles जास्त") }
        else         { sellScore += diff.toInt().coerceIn(1, 30); reasons.add("Bearish candles जास्त") }

        val trendBull = topGreen > topRed  && bottomGreen > bottomRed
        val trendBear = topRed   > topGreen && bottomRed  > bottomGreen
        val reversal  = topGreen > topRed  && bottomRed   > bottomGreen
        val sideways  = !trendBull && !trendBear && !reversal
        if (trendBull) { buyScore  += 15; reasons.add("Uptrend") }
        if (trendBear) { sellScore += 15; reasons.add("Downtrend") }
        if (reversal)  { sellScore += 8;  reasons.add("Reversal — trend बदलतोय") }

        val isDoji         = diff < 6f
        val isHammer       = topRed > topGreen * 1.4 && bottomGreen > bottomRed * 1.4
        val isShootingStar = topGreen > bottomGreen * 1.5 && topRed < topGreen
        val isBullEngulf   = gp > 65f && diff > 20f
        val isBearEngulf   = rp > 65f && diff > 20f
        val isMorningStar  = topRed  > topGreen && midGreen > midRed && bottomGreen > bottomRed
        val isEveningStar  = topGreen > topRed  && midRed > midGreen && bottomRed > bottomGreen
        val isThreeWhite   = leftGreen > leftRed && midGreen > midRed && rightGreen > rightRed && gp > 65f
        val isThreeCrows   = leftRed > leftGreen && midRed > midGreen && rightRed > rightGreen && rp > 65f

        when {
            isDoji         -> reasons.add("Doji — Market uncertain")
            isThreeWhite   -> { buyScore  += 25; reasons.add("Three White Soldiers — Strong BUY") }
            isThreeCrows   -> { sellScore += 25; reasons.add("Three Black Crows — Strong SELL") }
            isMorningStar  -> { buyScore  += 20; reasons.add("Morning Star — Bullish reversal") }
            isEveningStar  -> { sellScore += 20; reasons.add("Evening Star — Bearish reversal") }
            isBullEngulf   -> { buyScore  += 18; reasons.add("Bullish Engulfing") }
            isBearEngulf   -> { sellScore += 18; reasons.add("Bearish Engulfing") }
            isHammer       -> { buyScore  += 12; reasons.add("Hammer — Bullish reversal signal") }
            isShootingStar -> { sellScore += 12; reasons.add("Shooting Star — Bearish reversal signal") }
        }

        val leftTotal  = (leftGreen + leftRed).coerceAtLeast(1)
        val rightTotal = (rightGreen + rightRed).coerceAtLeast(1)
        val lRatio     = leftGreen.toFloat() / leftTotal
        val rRatio     = rightGreen.toFloat() / rightTotal
        val isDoubleTop    = tlRed > tlGreen && trRed > trGreen && bottomGreen > bottomRed
        val isDoubleBottom = blGreen > blRed  && brGreen > brRed  && topRed > topGreen
        val isAscTri       = lRatio < 0.45f && rRatio > 0.55f
        val isDescTri      = lRatio > 0.55f && rRatio < 0.45f
        val isFlag         = Math.abs(lRatio - rRatio) < 0.1f && diff < 15f

        when {
            isDoubleTop    -> { sellScore += 15; reasons.add("Double Top — Bearish reversal") }
            isDoubleBottom -> { buyScore  += 15; reasons.add("Double Bottom — Bullish reversal") }
            isAscTri       -> { buyScore  += 10; reasons.add("Ascending Triangle — Bullish breakout शक्य") }
            isDescTri      -> { sellScore += 10; reasons.add("Descending Triangle — Bearish breakdown शक्य") }
            isFlag -> {
                if (gp >= rp) { buyScore  += 8; reasons.add("Bull Flag — Continuation") }
                else          { sellScore += 8; reasons.add("Bear Flag — Continuation") }
            }
        }

        val highVolume = colorRatio > 0.12f
        val lowVolume  = colorRatio < 0.05f
        if (highVolume) {
            if (gp >= rp) { buyScore  += 10; reasons.add("High Volume — Bullish signal strong") }
            else          { sellScore += 10; reasons.add("High Volume — Bearish signal strong") }
        }
        if (lowVolume) {
            if (gp >= rp) buyScore  = (buyScore  - 8).coerceAtLeast(0)
            else          sellScore = (sellScore - 8).coerceAtLeast(0)
            reasons.add("Low Volume — Signal weak")
        }

        val rsiValue = when {
            gp > 75 -> (70 + ((gp - 75) / 25 * 20)).toInt().coerceIn(70, 90)
            gp < 25 -> (30 - ((25 - gp) / 25 * 20)).toInt().coerceIn(10, 30)
            else    -> (30 + (gp / 100 * 40)).toInt().coerceIn(30, 70)
        }
        when {
            rsiValue >= 75 -> { sellScore += 12; reasons.add("RSI $rsiValue — Overbought") }
            rsiValue <= 25 -> { buyScore  += 12; reasons.add("RSI $rsiValue — Oversold") }
            rsiValue >= 65 -> buyScore  += 5
            rsiValue <= 35 -> sellScore += 5
        }

        if (bottomGreen > bottomRed * 1.3) { buyScore  += 8; reasons.add("Strong Support") }
        if (topRed      > topGreen  * 1.3) { sellScore += 8; reasons.add("Strong Resistance") }

        if (highVolume && diff > 20) {
            if (gp > rp) { buyScore  += 12; reasons.add("Breakout — High volume + Bullish move") }
            else         { sellScore += 12; reasons.add("Breakdown — High volume + Bearish move") }
        }

        buyScore  += patternBuyBonus
        sellScore += patternSellBonus

        // ════════════════════════════════════════════════════════════════
        // DYNAMIC CONFIDENCE — 5 weighted factors (94% hard cap नाही)
        // ════════════════════════════════════════════════════════════════
        val totalScore = (buyScore + sellScore).coerceAtLeast(1)
        val signal     = if (buyScore >= sellScore) "BUY" else "SELL"
        val winScore   = if (signal == "BUY") buyScore else sellScore

        // Factor 1 — Base Score (pixel ratio)  [weight 35%]
        val baseRatio = winScore.toFloat() / totalScore
        val baseConf  = ((baseRatio - 0.5f) / 0.5f * 100f).coerceIn(0f, 100f)

        // Factor 2 — Pattern Engine confidence [weight 25%]
        val patternConf: Float = if (detectedPatterns.isNotEmpty()) {
            val aligned = detectedPatterns.filter {
                (signal == "BUY"  && it.patternType == "Bullish") ||
                (signal == "SELL" && it.patternType == "Bearish")
            }
            val opposing = detectedPatterns.filter {
                (signal == "BUY"  && it.patternType == "Bearish") ||
                (signal == "SELL" && it.patternType == "Bullish")
            }
            val alignedAvg  = if (aligned.isNotEmpty())
                aligned.map { it.confidence }.average().toFloat() else 50f
            val opposingPen = if (opposing.isNotEmpty())
                opposing.map { it.confidence }.average().toFloat() * 0.3f else 0f
            (alignedAvg - opposingPen).coerceIn(0f, 100f)
        } else 50f

        // Factor 3 — Trend Strength            [weight 20%]
        val trendConf: Float = when {
            signal == "BUY"  && trendBull -> 92f
            signal == "SELL" && trendBear -> 92f
            signal == "BUY"  && trendBear -> 20f
            signal == "SELL" && trendBull -> 20f
            reversal                      -> 62f
            sideways                      -> 40f
            else                          -> 55f
        }

        // Factor 4 — Volume Strength           [weight 12%]
        val volumeConf: Float = when {
            colorRatio > 0.18f -> 95f
            colorRatio > 0.12f -> 80f
            colorRatio > 0.08f -> 62f
            colorRatio > 0.05f -> 48f
            else               -> 28f
        }

        // Factor 5 — RSI Impact                [weight 8%]
        val rsiConf: Float = when {
            signal == "BUY"  && rsiValue <= 25 -> 95f
            signal == "BUY"  && rsiValue <= 35 -> 78f
            signal == "SELL" && rsiValue >= 75 -> 95f
            signal == "SELL" && rsiValue >= 65 -> 78f
            signal == "BUY"  && rsiValue >= 70 -> 25f
            signal == "SELL" && rsiValue <= 30 -> 25f
            rsiValue in 40..60                 -> 55f
            else                               -> 50f
        }

        // Weighted final — range 40-99, no hard cap
        val confidence = (
            baseConf    * 0.35f +
            patternConf * 0.25f +
            trendConf   * 0.20f +
            volumeConf  * 0.12f +
            rsiConf     * 0.08f
        ).toInt().coerceIn(40, 99)

        val confidenceLabel = when {
            confidence >= 91 -> "Very Strong"
            confidence >= 76 -> "Strong"
            confidence >= 61 -> "Medium"
            else             -> "Weak"
        }

        // ── Display Info ──────────────────────────────────────────────
        val trend = when {
            trendBull -> "📈 Uptrend (Bullish)"
            trendBear -> "📉 Downtrend (Bearish)"
            reversal  -> "🔄 Reversal"
            else      -> "↔ Sideways"
        }
        val rsiInfo = when {
            rsiValue >= 70 -> "RSI $rsiValue — Overbought (खूप खरेदी झाली)"
            rsiValue <= 30 -> "RSI $rsiValue — Oversold (खूप विक्री झाली)"
            else           -> "RSI $rsiValue — Normal zone"
        }
        val volumeInfo = when {
            colorRatio > 0.15f -> "🔥 High Volume"
            colorRatio > 0.08f -> "⚡ Medium Volume"
            else               -> "⚠️ Low Volume"
        }

        val riskLevel: String
        val riskColor: String
        when {
            confidence <= 60 -> {
                riskLevel = "🔴 HIGH RISK — Weak Signal ($confidenceLabel)"
                riskColor = "#F44336"
            }
            confidence <= 75 -> {
                riskLevel = "🟡 MEDIUM RISK — Moderate Signal ($confidenceLabel)"
                riskColor = "#FF9800"
            }
            confidence <= 90 -> {
                riskLevel = "🟢 LOW RISK — Strong Signal ($confidenceLabel)"
                riskColor = "#4CAF50"
            }
            else -> {
                riskLevel = "⭐ VERY LOW RISK — Very Strong Signal ($confidenceLabel)"
                riskColor = "#2196F3"
            }
        }

        val waitMessage = if (isDoji || (sideways && diff < 8f))
            "⚠️ Market uncertain — 2-3 candles confirm होण्याची वाट पाहा." else ""

        val signalReason = reasons.take(3).joinToString(" • ")

        val entrySlTarget = if (signal == "BUY")
            "✅ Entry: Current level वर\n🛑 Stop Loss: मागील low खाली\n🎯 Target: मागील high / next resistance"
        else
            "✅ Entry: Current level वर\n🛑 Stop Loss: मागील high वर\n🎯 Target: मागील low / next support"

        val rrRatio = when {
            confidence >= 91 -> "⭐ 1:4+ (Very Strong)"
            confidence >= 76 -> "✅ 1:3 (Strong)"
            confidence >= 61 -> "⚠️ 1:2 (Medium)"
            else             -> "❌ 1:1 (Weak — trade टाळा)"
        }

        val patternDisplay = PatternEngine.formatDisplay(detectedPatterns, topN = 5)

        val summary = buildString {
            appendLine("Signal: $signal  |  Strength: $confidence% ($confidenceLabel)")
            appendLine("Trend: $trend  |  $volumeInfo")
            appendLine("RSI: $rsiInfo")
            if (signalReason.isNotEmpty()) appendLine("Reason: $signalReason")
            appendLine()
            append(entrySlTarget)
        }

        return AnalysisResult(
            isValidChart      = true,
            signal            = signal,
            confidence        = confidence,
            greenPercent      = gp,
            redPercent        = rp,
            trend             = trend,
            volumeInfo        = volumeInfo,
            sentiment         = if (signal == "BUY") "Bullish" else "Bearish",
            supportResistance = entrySlTarget,
            summary           = summary,
            investorInfo      = rrRatio,
            rsiValue          = rsiValue,
            rsiInfo           = rsiInfo,
            riskLevel         = riskLevel,
            riskColor         = riskColor,
            waitMessage       = waitMessage,
            keyFactors        = signalReason,
            signalReason      = signalReason,
            detectedPatterns  = detectedPatterns,
            patternDisplay    = patternDisplay
        )
    }
}
