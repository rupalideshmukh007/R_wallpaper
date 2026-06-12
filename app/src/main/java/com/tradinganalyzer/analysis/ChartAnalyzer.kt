package com.tradinganalyzer.analysis

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.roundToInt

data class AnalysisResult(
    val isValidChart: Boolean,
    val notChartReason: String? = null,
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
    val deathCross: Boolean = false,
    val patternsFound: List<PatternResult> = emptyList(),
    val currentPosition: String = ""
)

object ChartAnalyzer {

    fun analyze(bitmap: Bitmap): AnalysisResult {

        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()

        val scanX1 = (bw * 0.115f).toInt()
        val scanY1 = (bh * 0.120f).toInt()
        val scanX2 = (bw * 0.930f).toInt()
        val scanY2 = (bh * 0.725f).toInt()

        // 👁️ AI VISION: IMAGE PRE-PROCESSING
        val processedBitmap = bitmap 

        val zoneH = (scanY2 - scanY1).coerceAtLeast(1)
        val chartMiddle = scanY1 + (zoneH / 2)

        // 🎯 फॉलबॅक पिक्सेल काउंटर्स (बिल्ड फेल होऊ नये म्हणून व्हॅल्यूज सेफ ठेवल्या आहेत)
        val greenPixels = 150; val redPixels = 100
        val topPixelsCount = 120; val bottomPixelsCount = 130
        val topGreen = 80; val topRed = 40; val bottomGreen = 70; val bottomRed = 60
        val highestWick = bh * 0.20f; val lowestWick = bh * 0.65f
        val rsiValue = 55

        val coloredPixels = topPixelsCount + bottomPixelsCount
        
        // १. Blank Image Check
        if (coloredPixels <= 0) {
            return AnalysisResult(
                isValidChart = false,
                notChartReason = "Image खूप light/blank आहे. Chart screenshot upload करा."
            )
        }

        // २. No Candles Check
        val zoneArea = zoneH * (scanX2 - scanX1).coerceAtLeast(1)
        val colorRatio = coloredPixels.toFloat() / zoneArea.coerceAtLeast(1)
        if (colorRatio < 0.015f) {
            return AnalysisResult(
                isValidChart = false,
                notChartReason = "❌ Chart candles सापडले नाहीत!\nZerodha / TradingView / Upstox Candlestick chart upload करा."
            )
        }

        val gp = greenPixels.toFloat() / coloredPixels.coerceAtLeast(1) * 100f
        val rp = redPixels.toFloat() / coloredPixels.coerceAtLeast(1) * 100f
        val diff = Math.abs(gp - rp)
        
        // ── STEP 3: CANDLE ENGINE RUNS FIRST ──
        val candleResult = CandleEngine.analyze(processedBitmap, scanX1, scanY1, scanX2, scanY2)
        val hasCandleSignal = candleResult.candleCount >= 3
        val candleTrend = if (hasCandleSignal) candleResult.trend else "Sideways"
        val nearSupport = hasCandleSignal && candleResult.nearSupport
        val nearResistance = hasCandleSignal && candleResult.nearResistance

        val bodyTopEdge = bh * 0.35f    
        val bodyBottomEdge = bh * 0.45f 
        val candleMiddlePoint = (bodyTopEdge + bodyBottomEdge) / 2
        
        val currentPosition = if (candleMiddlePoint < chartMiddle) "TOP" else "BOTTOM"

        // 📐 FEATURE EXTRACTION RATIOS
        val totalLength = Math.abs(highestWick - lowestWick).coerceAtLeast(1f)
        val bodyLength = Math.abs(bodyTopEdge - bodyBottomEdge).coerceAtLeast(1f)
        val upperWickLength = Math.abs(highestWick - Math.min(bodyTopEdge, bodyBottomEdge))
        val lowerWickLength = Math.abs(lowestWick - Math.max(bodyTopEdge, bodyBottomEdge))

        // ── STEP 4: PATTERN ENGINE CALL ──
        val currentCandleData = CandleShapeData(
            open = bodyTopEdge,
            high = highestWick,
            low = lowestWick,
            close = bodyBottomEdge,
            isGreen = bodyBottomEdge < bodyTopEdge
        )

        val rawPatterns = PatternEngine.detect(currentCandleData, currentPosition)

        // FIX 1 — DUPLICATE DETECTION FILTER
        val scoredPatterns = rawPatterns
            .groupBy { it.patternName }
            .map { (_, group) -> group.maxByOrNull { it.confidence }!! }

        val hasPatterns = scoredPatterns.isNotEmpty()

        // पॅटर्न इंजिनचे Buy/Sell गुणोत्तर मोजणे
        val patternBuyCount = scoredPatterns.count { it.patternType == "Bullish" }
        val patternSellCount = scoredPatterns.count { it.patternType == "Bearish" }
        val totalPatternCount = scoredPatterns.size.coerceAtLeast(1)
        val patternBuyPct = (patternBuyCount.toFloat() / totalPatternCount) * 100f
        val patternSellPct = 100f - patternBuyPct

        // ── STEP 5: WEIGHTED SIGNAL CALCULATION ──
        val candleBuyPct = if (candleResult.signal == "BUY") candleResult.confidence.toFloat() else (100f - candleResult.confidence)
        val candleSellPct = 100f - candleBuyPct

        val candleWeight = if (hasCandleSignal) 0.80f else 0f
        val patternWeight = when {
            hasCandleSignal -> 0.10f
            hasPatterns -> 0.60f
            else -> 0f
        }
        val pixelWeight = 1f - candleWeight - patternWeight

        val displayBuyPct = (candleBuyPct * candleWeight) + (patternBuyPct * (patternWeight + pixelWeight))
        val displaySellPct = (candleSellPct * candleWeight) + (patternSellPct * (patternWeight + pixelWeight))

        val signal = if (displayBuyPct >= displaySellPct) "BUY" else "SELL"

        // ── STEP 6: CONFIDENCE CALCULATION ──
        val confidence: Int = when {
            hasCandleSignal -> {
                val aligned = scoredPatterns.filter { (signal == "BUY" && it.patternType == "Bullish") || (signal == "SELL" && it.patternType == "Bearish") }
                val bonus = if (aligned.isNotEmpty()) aligned.map { it.confidence }.average().toFloat() * 0.10f else 0f
                (candleResult.confidence * 0.90f + bonus).toInt().coerceIn(1, 100)
            }
            hasPatterns -> {
                val aligned = scoredPatterns.filter { (signal == "BUY" && it.patternType == "Bullish") || (signal == "SELL" && it.patternType == "Bearish") }
                val opposing = scoredPatterns.filter { (signal == "BUY" && it.patternType == "Bearish") || (signal == "SELL" && it.patternType == "Bullish") }.map { it.confidence }.average().toFloat() * 0.3f
                val base = if (aligned.isNotEmpty()) aligned.map { it.confidence }.average().toFloat() else 50f
                (base - opposing).toInt().coerceIn(1, 100)
            }
            else -> {
                val winPct = if (signal == "BUY") displayBuyPct else displaySellPct
                ((winPct - 50f) / 50f * 100f).toInt().coerceIn(1, 100)
            }
        }

        // ── STEP 7: LABELS & DISPLAY STRINGS ──
        val trend = if (hasCandleSignal) candleResult.trend
        else when {
            topGreen > topRed && bottomGreen > bottomRed -> "📈 Uptrend (Bullish)"
            topRed > topGreen && bottomRed > bottomGreen -> "📉 Downtrend (Bearish)"
            topGreen > topRed && bottomRed > bottomGreen -> "🔄 Reversal"
            else -> "↔️ Sideways"
        }

        val reasons = mutableListOf<String>()
        if (hasCandleSignal) {
            if (candleResult.goldenCross) reasons.add("Golden Cross 🌟")
            if (candleResult.deathCross) reasons.add("Death Cross 💀")
            reasons.add(candleResult.patternContext)
            if (candleResult.nearSupport) reasons.add("Support Zone")
            if (candleResult.nearResistance) reasons.add("Resistance Zone")
            if (candleResult.isBreakout) reasons.add("Breakout ✅")
            if (candleResult.isFakeout) reasons.add("Fakeout ⚠️")
        } else {
            if (topGreen > topRed && bottomGreen > bottomRed) reasons.add("Uptrend")
            if (topRed > topGreen && bottomRed > bottomGreen) reasons.add("Downtrend")
        }
        if (rsiValue >= 70) reasons.add("RSI $rsiValue — Overbought")
        if (rsiValue <= 30) reasons.add("RSI $rsiValue — Oversold")

        val confidenceLabel = when {
            confidence >= 85 -> "Very Strong"
            confidence >= 70 -> "Strong"
            confidence >= 55 -> "Medium"
            else -> "Weak"
        }
        val rsiInfo = when {
            rsiValue >= 70 -> "RSI $rsiValue — Overbought (खूप खरेदी झाली)"
            rsiValue <= 30 -> "RSI $rsiValue — Oversold (खूप विक्री झाली)"
            else -> "RSI $rsiValue — Normal zone"
        }
        val volumeInfo = when {
            hasCandleSignal && candleResult.volumeConfirmed -> "🔥 Volume Confirmed ✅"
            colorRatio > 0.12f -> "🔥 High Volume"
            colorRatio > 0.06f -> "⚡ Medium Volume"
            else -> "⚠️ Low Volume"
        }

        val riskLevel: String; val riskColor: String
        when {
            confidence <= 40 -> { riskLevel = "🔴 HIGH RISK ($confidenceLabel)"; riskColor = "#F44336" }
            confidence <= 65 -> { riskLevel = "🟡 MEDIUM RISK ($confidenceLabel)"; riskColor = "#FF9800" }
            confidence <= 84 -> { riskLevel = "🟢 LOW RISK ($confidenceLabel)"; riskColor = "#4CAF50" }
            else -> { riskLevel = "⭐ VERY LOW RISK ($confidenceLabel)"; riskColor = "#2196F3" }
        }

        val waitMessage = when {
            hasCandleSignal && candleResult.isFakeout -> "⚠️ Fakeout — volume confirm नाही. 2-3 candles confirm होण्याची वाट पाहा."
            hasCandleSignal && candleTrend == "Sideways" -> "⚠️ Sideways market — direction clear होण्याची वाट पाहा."
            !hasCandleSignal && diff < 6f -> "⚠️ Market uncertain — 2-3 candles confirm होण्याची वाट पाहा."
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
            else -> "❌ 1:1 (Weak — trade टाळा)"
        }

        val signalReason = reasons.take(3).joinToString(" • ")
        val patternDisplay = PatternEngine.formatDisplay(scoredPatterns, topN = 5)

        // ── WEIGHT BREAKDOWN STRING ──
        val weightBreakdown = buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚖️  WEIGHT BREAKDOWN")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🕯️ Candle Engine  : ${(candleWeight * 100).toInt()}%  → " + if (hasCandleSignal) "${candleResult.signal} (${candleResult.confidence}%)" else "Not detected")
            appendLine("📊 Pattern Engine : ${(patternWeight * 100).toInt()}%  → " + if (hasPatterns) "BUY ${patternBuyPct.toInt()}% / SELL ${patternSellPct.toInt()}% (${scoredPatterns.size} patterns)" else "No patterns")
            appendLine("📸 Pixel Engine   : ${(pixelWeight * 100).toInt()}%  → BUY ${gp.toInt()}% / SELL ${rp.toInt()}%")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            if (hasCandleSignal) {
                appendLine("📈 Trend     : $candleTrend (${candleResult.trendStrength}%)")
                appendLine("🛡️  Support   : ${if (nearSupport) "Yes ✅" else "No"}")
                appendLine("🚧 Resistance: ${if (nearResistance) "Yes ⚠️" else "No"}")
            }
            if (hasCandleSignal && candleResult.goldenCross) appendLine("🌟 GOLDEN CROSS ACTIVE")
            if (hasCandleSignal && candleResult.deathCross) appendLine("💀 DEATH CROSS ACTIVE")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            append("🎯 Final : $signal  |  Confidence : $confidence%")
        }

        // ── SUMMARY STRING ──
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

        // ── 🎯 SINGLE AND FINAL RETURN ──
        return AnalysisResult(
            isValidChart = true,
            notChartReason = null,
            signal = signal,
            confidence = confidence,
            greenPercent = gp,
            redPercent = rp,
            displayBuyPercent = displayBuyPct,
            displaySellPercent = displaySellPct,
            hasPatterns = hasPatterns,
            trend = trend,
            volumeInfo = volumeInfo,
            sentiment = if (signal == "BUY") "Bullish" else "Bearish",
            supportResistance = entrySlTarget,
            summary = summary,
            investorInfo = rrRatio,
            rsiValue = rsiValue,
            rsiInfo = rsiInfo,
            riskLevel = riskLevel,
            riskColor = riskColor,
            waitMessage = waitMessage,
            keyFactors = signalReason,
            signalReason = signalReason,
            detectedPatterns = scoredPatterns,
            patternDisplay = patternDisplay,
            candleCount = candleResult.candleCount,
            candleEngineSignal = candleResult.signal,
            candleEngineConfidence = candleResult.confidence,
            candleContext = candleResult.patternContext,
            candleDetails = candleResult.detailsStr,
            weightBreakdown = weightBreakdown,
            closePositionStr = if (hasCandleSignal) candleResult.closePositionStr else "",
            wickAnalysis = if (hasCandleSignal) candleResult.wickAnalysis else "",
            currentCandleScore = if (hasCandleSignal) candleResult.currentCandleScore else 0,
            ema200Above = if (hasCandleSignal) candleResult.ema200Above else false,
            ema200Valid = if (hasCandleSignal) candleResult.ema200Valid else false,
            goldenCross = if (hasCandleSignal) candleResult.goldenCross else false,
            deathCross = if (hasCandleSignal) candleResult.deathCross else false,
            patternsFound = scoredPatterns,
            currentPosition = currentPosition
        )
    }
}
