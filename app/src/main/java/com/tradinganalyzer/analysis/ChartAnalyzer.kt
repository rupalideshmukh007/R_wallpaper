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

        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()

        // ════════════════════════════════════════════════════════════════
        // ★ ZONE-BASED SCAN — फक्त chart area scan करा (crop नाही)
        //   Top-Left  [115, 120] → Bottom-Right [930, 725] (0-1000 scale)
        //   Buttons, axis, title — सगळं बाहेर राहतं
        // ════════════════════════════════════════════════════════════════
        val scanX1 = (bw * 0.115f).toInt()
        val scanY1 = (bh * 0.120f).toInt()
        val scanX2 = (bw * 0.930f).toInt()
        val scanY2 = (bh * 0.725f).toInt()

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

        val zoneW = (scanX2 - scanX1).coerceAtLeast(1)
        val zoneH = (scanY2 - scanY1).coerceAtLeast(1)
        val halfW = scanX1 + zoneW / 2
        val halfH = scanY1 + zoneH / 2
        val thirdW1 = scanX1 + zoneW / 3
        val thirdW2 = scanX1 + zoneW * 2 / 3

        for (x in scanX1 until scanX2) {
            for (y in scanY1 until scanY2) {
                if (x >= bitmap.width || y >= bitmap.height) continue

                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                if (Color.alpha(pixel) < 50) continue
                totalPixels++

                // ── White/light background skip ────────────────────────
                if ((r + g + b) / 3 > 215) { backgroundPixels++; continue }

                // ── Selfie/skin check ──────────────────────────────────
                val isSkin = r > 100 && g > 50 && b > 30 && r > g && r > b &&
                             (r - g) in 10..120 && b < 180
                if (isSkin) { skinPixels++; continue }

                // ── Saturation check — grey/dark/grid skip ─────────────
                val maxC = maxOf(r, g, b)
                val minC = minOf(r, g, b)
                val sat  = if (maxC > 0) (maxC - minC).toFloat() / maxC else 0f
                if (sat < 0.28f) continue

                // ════════════════════════════════════════════════════════
                // ★ CANDLE COLOR DETECTION
                //
                // 🟢 GREEN Candle — TEAL (Zerodha/TradingView/Upstox):
                //    #26A69A → r=38,  g=166, b=154  (b >> r)
                //    #089981 → r=8,   g=153, b=129  (b >> r)
                //    BrightG → r=0,   g=200, b=83   (g >> r,b)
                //
                // 🔴 RED Candle:
                //    #EF5350 → r=239, g=83,  b=80
                //    #F23645 → r=242, g=54,  b=69
                //
                // ❌ BUY Button Green (#4CAF50 → r=76, g=175, b=80):
                //    b=80, r=76 → b/r=1.05 → SKIP (b > r*1.4 fail)
                // ════════════════════════════════════════════════════════

                // 🟢 Teal green candle: Green + Blue दोन्ही Red पेक्षा जास्त
                val isTealGreen = g > 55 && b > 55 &&
                                  g > r * 1.4f &&
                                  b > r * 1.4f

                // 🟢 Pure bright green candle (काही apps)
                val isPureGreen = g > 100 &&
                                  g > r * 2.8f &&
                                  g > b * 2.8f

                val isGreenCandle = isTealGreen || isPureGreen

                // 🔴 Red candle: Red clearly dominates
                val isRedCandle = r > 55 &&
                                  r > g * 1.5f &&
                                  r > b * 1.5f

                // Blue indicator lines skip (RSI, MA, Bollinger)
                val isBlue = b > r * 1.3f && b > g * 1.3f
                if (isBlue && !isGreenCandle) continue

                // फक्त candle pixels count — बाकी skip
                if (!isGreenCandle && !isRedCandle) continue

                // ── Zone tracking ──────────────────────────────────────
                if (isGreenCandle) {
                    greenPixels++
                    if (y < halfH) topGreen++ else bottomGreen++
                    when {
                        x < thirdW1 -> leftGreen++
                        x < thirdW2 -> midGreen++
                        else        -> rightGreen++
                    }
                    if      (y < halfH && x < halfW) tlGreen++
                    else if (y < halfH)              trGreen++
                    else if (x < halfW)              blGreen++
                    else                             brGreen++
                } else {
                    redPixels++
                    if (y < halfH) topRed++ else bottomRed++
                    when {
                        x < thirdW1 -> leftRed++
                        x < thirdW2 -> midRed++
                        else        -> rightRed++
                    }
                    if      (y < halfH && x < halfW) tlRed++
                    else if (y < halfH)              trRed++
                    else if (x < halfW)              blRed++
                    else                             brRed++
                }
            }
        }

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
        val colorRatio    = coloredPixels.toFloat() / analyzable.coerceAtLeast(1)
        if (colorRatio < 0.015f) return AnalysisResult(
            isValidChart = false,
            notChartReason = "❌ Chart candles सापडले नाहीत!\nZerodha / TradingView / Upstox Candlestick chart upload करा."
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

        val trendBull = topGreen > topRed   && bottomGreen > bottomRed
        val trendBear = topRed   > topGreen && bottomRed   > bottomGreen
        val reversal  = topGreen > topRed   && bottomRed   > bottomGreen
        val sideways  = !trendBull && !trendBear && !reversal
        if (trendBull) { buyScore  += 15; reasons.add("Uptrend") }
        if (trendBear) { sellScore += 15; reasons.add("Downtrend") }
        if (reversal)  { sellScore += 8;  reasons.add("Reversal — trend बदलतोय") }

        val isDoji         = diff < 6f
        val isHammer       = topRed > topGreen * 1.4f && bottomGreen > bottomRed * 1.3f && diff < 22f
        val isShootingStar = topGreen > bottomGreen * 1.5f && rp >= gp - 12f
        val isBullEngulf   = gp > 65f && diff > 18f
        val isBearEngulf   = rp > 65f && diff > 18f
        val isMorningStar  = topRed   > topGreen && midGreen > midRed   && bottomGreen > bottomRed
        val isEveningStar  = topGreen > topRed   && midRed   > midGreen && bottomRed   > bottomGreen
        val isThreeWhite   = leftGreen > leftRed  && midGreen > midRed  && rightGreen > rightRed && gp > 62f
        val isThreeCrows   = leftRed   > leftGreen && midRed  > midGreen && rightRed  > rightGreen && rp > 62f

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
        val lRatio     = leftGreen.toFloat()  / leftTotal
        val rRatio     = rightGreen.toFloat() / rightTotal
        val isDoubleTop    = tlRed > tlGreen && trRed > trGreen && bottomGreen > bottomRed
        val isDoubleBottom = blGreen > blRed  && brGreen > brRed && topRed > topGreen
        val isAscTri       = lRatio < 0.45f && rRatio > 0.56f
        val isDescTri      = lRatio > 0.55f && rRatio < 0.44f
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

        val highVolume = colorRatio > 0.10f
        val lowVolume  = colorRatio < 0.03f
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

        if (bottomGreen > bottomRed * 1.3f) { buyScore  += 8;  reasons.add("Strong Support") }
        if (topRed      > topGreen  * 1.3f) { sellScore += 8;  reasons.add("Strong Resistance") }

        if (highVolume && diff > 20f) {
            if (gp > rp) { buyScore  += 12; reasons.add("Breakout — High volume + Bullish move") }
            else         { sellScore += 12; reasons.add("Breakdown — High volume + Bearish move") }
        }

        buyScore  += patternBuyBonus
        sellScore += patternSellBonus

        // ════════════════════════════════════════════════════════════════
        // DYNAMIC CONFIDENCE — 5 weighted factors — coerceIn(1, 100)
        // ════════════════════════════════════════════════════════════════
        val totalScore = (buyScore + sellScore).coerceAtLeast(1)
        val signal     = if (buyScore >= sellScore) "BUY" else "SELL"
        val winScore   = if (signal == "BUY") buyScore else sellScore

        val baseRatio = winScore.toFloat() / totalScore
        val baseConf  = ((baseRatio - 0.5f) / 0.5f * 100f).coerceIn(0f, 100f)

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

        val trendConf: Float = when {
            signal == "BUY"  && trendBull -> 92f
            signal == "SELL" && trendBear -> 92f
            signal == "BUY"  && trendBear -> 20f
            signal == "SELL" && trendBull -> 20f
            reversal                      -> 62f
            sideways                      -> 40f
            else                          -> 55f
        }

        val volumeConf: Float = when {
            colorRatio > 0.15f -> 95f
            colorRatio > 0.10f -> 80f
            colorRatio > 0.06f -> 62f
            colorRatio > 0.03f -> 48f
            else               -> 28f
        }

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

        val confidence = (
            baseConf    * 0.35f +
            patternConf * 0.25f +
            trendConf   * 0.20f +
            volumeConf  * 0.12f +
            rsiConf     * 0.08f
        ).toInt().coerceIn(1, 100)

        val confidenceLabel = when {
            confidence >= 85 -> "Very Strong"
            confidence >= 70 -> "Strong"
            confidence >= 55 -> "Medium"
            else             -> "Weak"
        }

        val trend = when {
            trendBull -> "📈 Uptrend (Bullish)"
            trendBear -> "📉 Downtrend (Bearish)"
            reversal  -> "🔄 Reversal"
            else      -> "↔️ Sideways"
        }
        val rsiInfo = when {
            rsiValue >= 70 -> "RSI $rsiValue — Overbought (खूप खरेदी झाली)"
            rsiValue <= 30 -> "RSI $rsiValue — Oversold (खूप विक्री झाली)"
            else           -> "RSI $rsiValue — Normal zone"
        }
        val volumeInfo = when {
            colorRatio > 0.12f -> "🔥 High Volume"
            colorRatio > 0.06f -> "⚡ Medium Volume"
            else               -> "⚠️ Low Volume"
        }

        val riskLevel: String
        val riskColor: String
        when {
            confidence <= 40 -> {
                riskLevel = "🔴 HIGH RISK — Weak Signal ($confidenceLabel)"
                riskColor = "#F44336"
            }
            confidence <= 65 -> {
                riskLevel = "🟡 MEDIUM RISK — Moderate Signal ($confidenceLabel)"
                riskColor = "#FF9800"
            }
            confidence <= 84 -> {
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
            confidence >= 85 -> "⭐ 1:4+ (Very Strong)"
            confidence >= 70 -> "✅ 1:3 (Strong)"
            confidence >= 55 -> "⚠️ 1:2 (Medium)"
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
