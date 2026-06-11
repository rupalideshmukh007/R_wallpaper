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
    val displayBuyPercent: Float = 0f,
    val displaySellPercent: Float = 0f,
    val hasPatterns: Boolean = false,
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

        val zoneW   = (scanX2 - scanX1).coerceAtLeast(1)
        val zoneH   = (scanY2 - scanY1).coerceAtLeast(1)
        val halfW   = scanX1 + zoneW / 2
        val halfH   = scanY1 + zoneH / 2
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
                if ((r + g + b) / 3 > 215) { backgroundPixels++; continue }
                val isSkin = r > 100 && g > 50 && b > 30 && r > g && r > b &&
                             (r - g) in 10..120 && b < 180
                if (isSkin) { skinPixels++; continue }
                val maxC = maxOf(r, g, b)
                val minC = minOf(r, g, b)
                val sat  = if (maxC > 0) (maxC - minC).toFloat() / maxC else 0f
                if (sat < 0.28f) continue
                val isTealGreen  = g > 55 && b > 55 && g > r * 1.4f && b > r * 1.4f
                val isPureGreen  = g > 100 && g > r * 2.8f && g > b * 2.8f
                val isGreenCandle = isTealGreen || isPureGreen
                val isRedCandle   = r > 55 && r > g * 1.5f && r > b * 1.5f
                val isBlue        = b > r * 1.3f && b > g * 1.3f
                if (isBlue && !isGreenCandle) continue
                if (!isGreenCandle && !isRedCandle) continue

                if (isGreenCandle) {
                    greenPixels++
                    if (y < halfH) topGreen++ else bottomGreen++
                    when { x < thirdW1 -> leftGreen++; x < thirdW2 -> midGreen++; else -> rightGreen++ }
                    if      (y < halfH && x < halfW) tlGreen++
                    else if (y < halfH)              trGreen++
                    else if (x < halfW)              blGreen++
                    else                             brGreen++
                } else {
                    redPixels++
                    if (y < halfH) topRed++ else bottomRed++
                    when { x < thirdW1 -> leftRed++; x < thirdW2 -> midRed++; else -> rightRed++ }
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

        // ── Pixel % (raw) ─────────────────────────────────────────────
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
        val detectedPatterns = PatternEngine.detect(pixelData)
        val hasPatterns      = detectedPatterns.isNotEmpty()

        // ════════════════════════════════════════════════════════════════
        // CHANGE 3: Pattern BUY/SELL % — Pattern 90% + Pixel 10%
        // ════════════════════════════════════════════════════════════════
        val totalPatConf = detectedPatterns.sumOf { it.confidence.toLong() }.coerceAtLeast(1L)
        val bullConf     = detectedPatterns.filter { it.patternType == "Bullish" }.sumOf { it.confidence.toLong() }
        val bearConf     = detectedPatterns.filter { it.patternType == "Bearish" }.sumOf { it.confidence.toLong() }

        val patternBuyPct  = if (hasPatterns) bullConf.toFloat() / totalPatConf * 100f else 50f
        val patternSellPct = if (hasPatterns) bearConf.toFloat() / totalPatConf * 100f else 50f

        // Display % — Pattern 90% + Pixel 10% जेव्हा patterns आहेत
        // Pattern नाही → फक्त Pixel %
        val displayBuyPct  = if (hasPatterns) patternBuyPct * 0.9f + gp * 0.1f else gp
        val displaySellPct = if (hasPatterns) patternSellPct * 0.9f + rp * 0.1f else rp

        // ── Signal = Display % वरून ───────────────────────────────────
        val signal = if (displayBuyPct >= displaySellPct) "BUY" else "SELL"

        // ════════════════════════════════════════════════════════════════
        // CHANGE 2: Signal Strength (Confidence) = Pattern ONLY
        // ════════════════════════════════════════════════════════════════
        val patternConf: Float = if (hasPatterns) {
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
        } else {
            // Pattern नाही → Pixel ratio वरून fallback confidence
            val winPct = if (signal == "BUY") displayBuyPct else displaySellPct
            ((winPct - 50f) / 50f * 100f).coerceIn(0f, 100f)
        }

        val confidence = patternConf.toInt().coerceIn(1, 100)

        // ── Trend ─────────────────────────────────────────────────────
        val trendBull = topGreen > topRed   && bottomGreen > bottomRed
        val trendBear = topRed   > topGreen && bottomRed   > bottomGreen
        val reversal  = topGreen > topRed   && bottomRed   > bottomGreen
        val sideways  = !trendBull && !trendBear && !reversal
        val isDoji    = diff < 6f

        // ── Reasons (for display only) ────────────────────────────────
        val reasons = mutableListOf<String>()
        if (trendBull) reasons.add("Uptrend")
        if (trendBear) reasons.add("Downtrend")
        if (reversal)  reasons.add("Reversal")
        if (isDoji)    reasons.add("Doji — Market uncertain")
        val highVolume = colorRatio > 0.10f
        val lowVolume  = colorRatio < 0.03f
        if (highVolume && displayBuyPct > displaySellPct)  reasons.add("High Volume — Bullish")
        if (highVolume && displaySellPct > displayBuyPct)  reasons.add("High Volume — Bearish")
        if (lowVolume)  reasons.add("Low Volume — Weak signal")
        if (bottomGreen > bottomRed * 1.3f) reasons.add("Strong Support")
        if (topRed > topGreen * 1.3f)       reasons.add("Strong Resistance")

        // ── RSI ───────────────────────────────────────────────────────
        val rsiValue = when {
            gp > 75 -> (70 + ((gp - 75) / 25 * 20)).toInt().coerceIn(70, 90)
            gp < 25 -> (30 - ((25 - gp) / 25 * 20)).toInt().coerceIn(10, 30)
            else    -> (30 + (gp / 100 * 40)).toInt().coerceIn(30, 70)
        }
        when {
            rsiValue >= 75 -> reasons.add("RSI $rsiValue — Overbought")
            rsiValue <= 25 -> reasons.add("RSI $rsiValue — Oversold")
        }

        // ── Display labels ────────────────────────────────────────────
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
            confidence <= 40 -> { riskLevel = "🔴 HIGH RISK — Weak Signal ($confidenceLabel)";       riskColor = "#F44336" }
            confidence <= 65 -> { riskLevel = "🟡 MEDIUM RISK — Moderate Signal ($confidenceLabel)"; riskColor = "#FF9800" }
            confidence <= 84 -> { riskLevel = "🟢 LOW RISK — Strong Signal ($confidenceLabel)";      riskColor = "#4CAF50" }
            else             -> { riskLevel = "⭐ VERY LOW RISK — Very Strong ($confidenceLabel)";   riskColor = "#2196F3" }
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
            isValidChart       = true,
            signal             = signal,
            confidence         = confidence,
            greenPercent       = gp,
            redPercent         = rp,
            displayBuyPercent  = displayBuyPct,
            displaySellPercent = displaySellPct,
            hasPatterns        = hasPatterns,
            trend              = trend,
            volumeInfo         = volumeInfo,
            sentiment          = if (signal == "BUY") "Bullish" else "Bearish",
            supportResistance  = entrySlTarget,
            summary            = summary,
            investorInfo       = rrRatio,
            rsiValue           = rsiValue,
            rsiInfo            = rsiInfo,
            riskLevel          = riskLevel,
            riskColor          = riskColor,
            waitMessage        = waitMessage,
            keyFactors         = signalReason,
            signalReason       = signalReason,
            detectedPatterns   = detectedPatterns,
            patternDisplay     = patternDisplay
        )
    }
}
