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
    val patternDisplay: String = "",
    val candleCount: Int = 0,
    val candleEngineSignal: String = "",
    val candleEngineConfidence: Int = 0,
    val candleContext: String = "",
    val candleDetails: String = "",
    val weightBreakdown: String = "",
    val closePositionStr: String = "",
    val wickAnalysis: String = "",
    val currentCandleScore: Int = 0,
    val ema200Above: Boolean = false,
    val ema200Valid: Boolean = false,
    val goldenCross: Boolean = false,
    val deathCross: Boolean = false
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
        var topGreen = 0L; var topRed = 0L
        var bottomGreen = 0L; var bottomRed = 0L
        var leftGreen = 0L; var leftRed = 0L
        var midGreen = 0L; var midRed = 0L
        var rightGreen = 0L; var rightRed = 0L
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
                val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
                if (Color.alpha(pixel) < 50) continue
                totalPixels++
                if ((r + g + b) / 3 > 215) { backgroundPixels++; continue }
                val isSkin = r > 100 && g > 50 && b > 30 && r > g && r > b &&
                             (r - g) in 10..120 && b < 180
                if (isSkin) { skinPixels++; continue }
                val maxC = maxOf(r, g, b); val minC = minOf(r, g, b)
                val sat  = if (maxC > 0) (maxC - minC).toFloat() / maxC else 0f
                if (sat < 0.28f) continue
                val isTealGreen   = g > 55 && b > 55 && g > r * 1.4f && b > r * 1.4f
                val isPureGreen   = g > 100 && g > r * 2.8f && g > b * 2.8f
                val isGreenCandle = isTealGreen || isPureGreen
                val isRedCandle   = r > 55 && r > g * 1.5f && r > b * 1.5f
                val isBlue        = b > r * 1.3f && b > g * 1.3f
                if (isBlue && !isGreenCandle) continue
                if (!isGreenCandle && !isRedCandle) continue

                if (isGreenCandle) {
                    greenPixels++
                    if (y < halfH) topGreen++ else bottomGreen++
                    when { x < thirdW1 -> leftGreen++; x < thirdW2 -> midGreen++; else -> rightGreen++ }
                    if (y < halfH && x < halfW) tlGreen++ else if (y < halfH) trGreen++
                    else if (x < halfW) blGreen++ else brGreen++
                } else {
                    redPixels++
                    if (y < halfH) topRed++ else bottomRed++
                    when { x < thirdW1 -> leftRed++; x < thirdW2 -> midRed++; else -> rightRed++ }
                    if (y < halfH && x < halfW) tlRed++ else if (y < halfH) trRed++
                    else if (x < halfW) blRed++ else brRed++
                }
            }
        }

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

        val pixelData = PixelData(
            greenPixels = greenPixels, redPixels = redPixels, gp = gp, rp = rp,
            topGreen = topGreen, topRed = topRed,
            bottomGreen = bottomGreen, bottomRed = bottomRed,
            leftGreen = leftGreen, leftRed = leftRed,
            midGreen = midGreen, midRed = midRed,
            rightGreen = rightGreen, rightRed = rightRed,
            tlGreen = tlGreen, tlRed = tlRed, trGreen = trGreen, trRed = trRed,
            blGreen = blGreen, blRed = blRed, brGreen = brGreen, brRed = brRed,
            colorRatio = colorRatio
        )
        val detectedPatterns = PatternEngine.detect(pixelData)
        val hasPatterns      = detectedPatterns.isNotEmpty()

        val totalPatConf  = detectedPatterns.sumOf { it.confidence.toLong() }.coerceAtLeast(1L)
        val bullConf      = detectedPatterns.filter { it.patternType == "Bullish" }.sumOf { it.confidence.toLong() }
        val bearConf      = detectedPatterns.filter { it.patternType == "Bearish" }.sumOf { it.confidence.toLong() }
        val patternBuyPct  = if (hasPatterns) bullConf.toFloat() / totalPatConf * 100f else 50f
        val patternSellPct = if (hasPatterns) bearConf.toFloat() / totalPatConf * 100f else 50f

        val candleResult    = CandleEngine.analyze(bitmap, scanX1, scanY1, scanX2, scanY2)
        val hasCandleSignal = candleResult.candleCount >= 3

        val candleBuyPct  = if (candleResult.signal == "BUY")
            candleResult.confidence.toFloat() else (100f - candleResult.confidence)
        val candleSellPct = 100f - candleBuyPct

        val candleWeight  = if (hasCandleSignal) 0.80f else 0f
        val patternWeight = when {
            hasCandleSignal -> 0.10f
            hasPatterns     -> 0.60f
            else            -> 0f
        }
        val pixelWeight = 1f - candleWeight - patternWeight

        val displayBuyPct  = candleBuyPct  * candleWeight + patternBuyPct  * patternWeight + gp * pixelWeight
        val displaySellPct = candleSellPct * candleWeight + patternSellPct * patternWeight + rp * pixelWeight
        val signal = if (displayBuyPct >= displaySellPct) "BUY" else "SELL"

        val confidence: Int = when {
            hasCandleSignal -> {
                val aligned = detectedPatterns.filter {
                    (signal == "BUY"  && it.patternType == "Bullish") ||
                    (signal == "SELL" && it.patternType == "Bearish")
                }
                val patternBonus = if (aligned.isNotEmpty())
                    aligned.map { it.confidence }.average().toFloat() * 0.10f else 0f
                (candleResult.confidence * 0.90f + patternBonus).toInt().coerceIn(1, 100)
            }
            hasPatterns -> {
                val aligned = detectedPatterns.filter {
                    (signal == "BUY"  && it.patternType == "Bullish") ||
                    (signal == "SELL" && it.patternType == "Bearish")
                }
                val opposing = detectedPatterns.filter {
                    (signal == "BUY"  && it.patternType == "Bearish") ||
                    (signal == "SELL" && it.patternType == "Bullish")
                }.map { it.confidence }.average().toFloat() * 0.3f
                val base = if (aligned.isNotEmpty())
                    aligned.map { it.confidence }.average().toFloat() else 50f
                (base - opposing).toInt().coerceIn(1, 100)
            }
            else -> {
                val winPct = if (signal == "BUY") displayBuyPct else displaySellPct
                ((winPct - 50f) / 50f * 100f).toInt().coerceIn(1, 100)
            }
        }

        val trend = if (hasCandleSignal) candleResult.trend
        else when {
            topGreen > topRed   && bottomGreen > bottomRed   -> "📈 Uptrend (Bullish)"
            topRed   > topGreen && bottomRed   > bottomGreen -> "📉 Downtrend (Bearish)"
            topGreen > topRed   && bottomRed   > bottomGreen -> "🔄 Reversal"
            else                                              -> "↔️ Sideways"
        }

        val rsiValue = if (hasCandleSignal) candleResult.rsiEstimate
        else when {
            gp > 75 -> (70 + ((gp - 75) / 25 * 20)).toInt().coerceIn(70, 90)
            gp < 25 -> (30 - ((25 - gp) / 25 * 20)).toInt().coerceIn(10, 30)
            else    -> (30 + (gp / 100 * 40)).toInt().coerceIn(30, 70)
        }

        val reasons = mutableListOf<String>()
        if (hasCandleSignal) {
            if (candleResult.goldenCross)    reasons.add("Golden Cross 🌟")
            if (candleResult.deathCross)     reasons.add("Death Cross 💀")
            reasons.add(candleResult.patternContext)
            if (candleResult.nearSupport)    reasons.add("Support Zone")
            if (candleResult.nearResistance) reasons.add("Resistance Zone")
            if (candleResult.isBreakout)     reasons.add("Breakout ✅")
            if (candleResult.isFakeout)      reasons.add("Fakeout ⚠️")
        } else {
            if (topGreen > topRed   && bottomGreen > bottomRed)   reasons.add("Uptrend")
            if (topRed   > topGreen && bottomRed   > bottomGreen) reasons.add("Downtrend")
        }
        if (rsiValue >= 70) reasons.add("RSI $rsiValue — Overbought")
        if (rsiValue <= 30) reasons.add("RSI $rsiValue — Oversold")

        val confidenceLabel = when {
            confidence >= 85 -> "Very Strong"; confidence >= 70 -> "Strong"
            confidence >= 55 -> "Medium";      else             -> "Weak"
        }
        val rsiInfo = when {
            rsiValue >= 70 -> "RSI $rsiValue — Overbought (खूप खरेदी झाली)"
            rsiValue <= 30 -> "RSI $rsiValue — Oversold (खूप विक्री झाली)"
            else           -> "RSI $rsiValue — Normal zone"
        }
        val volumeInfo = when {
            hasCandleSignal && candleResult.volumeConfirmed -> "🔥 Volume Confirmed ✅"
            colorRatio > 0.12f -> "🔥 High Volume"
            colorRatio > 0.06f -> "⚡ Medium Volume"
            else               -> "⚠️ Low Volume"
        }

        val riskLevel: String; val riskColor: String
        when {
            confidence <= 40 -> { riskLevel = "🔴 HIGH RISK ($confidenceLabel)";     riskColor = "#F44336" }
            confidence <= 65 -> { riskLevel = "🟡 MEDIUM RISK ($confidenceLabel)";   riskColor = "#FF9800" }
            confidence <= 84 -> { riskLevel = "🟢 LOW RISK ($confidenceLabel)";      riskColor = "#4CAF50" }
            else             -> { riskLevel = "⭐ VERY LOW RISK ($confidenceLabel)"; riskColor = "#2196F3" }
        }

        val waitMessage = when {
            hasCandleSignal && candleResult.isFakeout ->
                "⚠️ Fakeout — volume confirm नाही. 2-3 candles confirm होण्याची वाट पाहा."
            hasCandleSignal && candleResult.trend == "Sideways" ->
                "⚠️ Sideways market — direction clear होण्याची वाट पाहा."
            !hasCandleSignal && diff < 6f ->
                "⚠️ Market uncertain — 2-3 candles confirm होण्याची वाट पाहा."
            else -> ""
        }

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

        val signalReason   = reasons.take(3).joinToString(" • ")
        val patternDisplay = PatternEngine.formatDisplay(detectedPatterns, topN = 5)

        val weightBreakdown = buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚖️ WEIGHT BREAKDOWN")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🕯️ Candle Engine : ${(candleWeight * 100).toInt()}%  → ${if (hasCandleSignal) "${candleResult.signal} (${candleResult.confidence}%)" else "Not detected"}")
            appendLine("📊 Pattern Engine: ${(patternWeight * 100).toInt()}%  → ${if (hasPatterns) "BUY ${patternBuyPct.toInt()}% / SELL ${patternSellPct.toInt()}%" else "No patterns"}")
            appendLine("📸 Pixel Engine  : ${(pixelWeight * 100).toInt()}%  → BUY ${gp.toInt()}% / SELL ${rp.toInt()}%")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            if (hasCandleSignal && candleResult.goldenCross) appendLine("🌟 GOLDEN CROSS ACTIVE")
            if (hasCandleSignal && candleResult.deathCross)  appendLine("💀 DEATH CROSS ACTIVE")
            append("🎯 Final: $signal  |  Confidence: $confidence%")
        }

        val summary = buildString {
            appendLine("Signal: $signal  |  Strength: $confidence% ($confidenceLabel)")
            appendLine("Trend: $trend  |  $volumeInfo")
            appendLine("RSI: $rsiInfo")
            if (hasCandleSignal) {
                appendLine("Candle: ${candleResult.currentCandleStr}")
                appendLine("Close: ${candleResult.closePositionStr}")
            }
            if (signalReason.isNotEmpty()) appendLine("Reason: $signalReason")
            appendLine()
            append(entrySlTarget)
        }

        return AnalysisResult(
            isValidChart = true,
            signal = signal, confidence = confidence,
            greenPercent = gp, redPercent = rp,
            displayBuyPercent = displayBuyPct, displaySellPercent = displaySellPct,
            hasPatterns = hasPatterns, trend = trend,
            volumeInfo = volumeInfo, sentiment = if (signal == "BUY") "Bullish" else "Bearish",
            supportResistance = entrySlTarget, summary = summary,
            investorInfo = rrRatio, rsiValue = rsiValue, rsiInfo = rsiInfo,
            riskLevel = riskLevel, riskColor = riskColor,
            waitMessage = waitMessage, keyFactors = signalReason, signalReason = signalReason,
            detectedPatterns = detectedPatterns, patternDisplay = patternDisplay,
            candleCount = candleResult.candleCount,
            candleEngineSignal = candleResult.signal,
            candleEngineConfidence = candleResult.confidence,
            candleContext = candleResult.patternContext,
            candleDetails = candleResult.detailsStr,
            weightBreakdown = weightBreakdown,
            closePositionStr   = if (hasCandleSignal) candleResult.closePositionStr  else "",
            wickAnalysis       = if (hasCandleSignal) candleResult.wickAnalysis       else "",
            currentCandleScore = if (hasCandleSignal) candleResult.currentCandleScore else 0,
            ema200Above  = if (hasCandleSignal) candleResult.ema200Above  else false,
            ema200Valid  = if (hasCandleSignal) candleResult.ema200Valid   else false,
            goldenCross  = if (hasCandleSignal) candleResult.goldenCross   else false,
            deathCross   = if (hasCandleSignal) candleResult.deathCross    else false
        )
    }
                       }
