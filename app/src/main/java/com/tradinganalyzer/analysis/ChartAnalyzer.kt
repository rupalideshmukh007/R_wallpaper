package com.tradinganalyzer.analysis

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

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
    val currentPosition: String = "",
    val rightSidePixelBox: String = ""
)

object ChartAnalyzer {

    // 🎯 ग्लोबल मेमरी लिस्ट
    private var closePricesList = mutableListOf<Float>()

    fun analyze(bitmap: Bitmap): AnalysisResult {

        val bw = bitmap.width
        val bh = bitmap.height

        val scanX1 = (bw * 0.115f).toInt()
        val scanY1 = (bh * 0.120f).toInt()
        val scanX2 = (bw * 0.930f).toInt()
        val scanY2 = (bh * 0.725f).toInt()

        val zoneH = (scanY2 - scanY1).coerceAtLeast(1)
        val chartMiddle = scanY1 + (zoneH / 2)

        var topWickY = scanY2.toFloat()
        var bottomWickY = scanY1.toFloat()
        var bodyTopY = scanY2.toFloat()
        var bodyBottomY = scanY1.toFloat()
        
        var greenEdgesDetected = 0
        var redEdgesDetected = 0

        // EDGE DETECTION MAPPING
        for (x in scanX1 until scanX2 step 4) {
            for (y in scanY1 until scanY2 step 4) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val isGreenEdge = (g > r * 1.2 && g > b * 1.2 && g > 50)
                val isRedEdge = (r > g * 1.2 && r > b * 1.2 && r > 50)

                if (isGreenEdge || isRedEdge) {
                    val currentY = y.toFloat()
                    if (currentY < topWickY) topWickY = currentY
                    if (currentY > bottomWickY) bottomWickY = currentY

                    if (isGreenEdge) {
                        greenEdgesDetected++
                        if (currentY < bodyTopY) bodyTopY = currentY
                        if (currentY > bodyBottomY) bodyBottomY = currentY
                    } else {
                        redEdgesDetected++
                        if (currentY < bodyTopY) bodyTopY = currentY
                        if (currentY > bodyBottomY) bodyBottomY = currentY
                    }
                }
            }
        }

        val totalEdges = greenEdgesDetected + redEdgesDetected
        if (totalEdges <= 0) {
            return AnalysisResult(isValidChart = false, notChartReason = "❌ Chart कडा अस्पष्ट आहेत.")
        }

        val isGreenCandle = greenEdgesDetected > redEdgesDetected
        val openPrice = if (isGreenCandle) bodyBottomY else bodyTopY
        val closePrice = if (isGreenCandle) bodyTopY else bodyBottomY

        val extractedCandle = OhlcCandle(open = openPrice, high = topWickY, low = bottomWickY, close = closePrice)
        val candleCenter = (topWickY + bottomWickY) / 2f
        val currentPosition = if (candleCenter < chartMiddle) "TOP" else "BOTTOM"

        // ── STEP 3: CANDLE ENGINE BACKEND CALL ──
        val candleResult = CandleEngine.analyze(bitmap, scanX1, scanY1, scanX2, scanY2)
        val hasCandleSignal = candleResult.candleCount >= 3

        // मेमरी लिस्ट मॅनेजमेंट
        closePricesList.add(closePrice)
        if (closePricesList.size > 200) closePricesList.removeAt(0)

        // 🎯 फिक्स: स्टॅटिक companion object वरून आरएसआय मिळवणे
        val rsiValue = PatternEngine.rsi(closePricesList).toInt().coerceIn(10, 90)

        // 🎯 फिक्स: फक्त २ पॅरामीटर्स पाठवून PatternEngine चा परफेक्ट कॉल करणे!
        val rawPatterns = PatternEngine().analyze(extractedCandle, closePricesList)
        
        val scoredPatterns = rawPatterns
            .groupBy { it.patternName }
            .map { it.value.maxByOrNull { p -> p.confidence }!! }

        val hasPatterns = scoredPatterns.isNotEmpty()

        val bullishPatterns = scoredPatterns.filter { it.patternType == "BULLISH" }
        val bearishPatterns = scoredPatterns.filter { it.patternType == "BEARISH" }

        val signal: String
        val confidence: Int
        val triggerReason: String

        if (bullishPatterns.size > bearishPatterns.size) {
            signal = "BUY"
            val topBull = bullishPatterns.maxByOrNull { it.confidence }
            confidence = topBull?.confidence ?: 75
            triggerReason = topBull?.patternName ?: "Bullish Dominance"
        } else if (bearishPatterns.size > bullishPatterns.size) {
            signal = "SELL"
            val topBear = bearishPatterns.maxByOrNull { it.confidence }
            confidence = topBear?.confidence ?: 75
            triggerReason = topBear?.patternName ?: "Bearish Dominance"
        } else {
            signal = if (isGreenCandle) "BUY" else "SELL"
            confidence = 60
            triggerReason = "OHLC Neutral Trend"
        }

        // ── 📊 RIGHT SIDE PIXEL CALCULATION BOX ──
        val rightSidePixelBox = buildString {
            appendLine("📐 [ PIXEL CALCULATION BOX ]")
            appendLine("• Green Pixels Matched: $greenEdgesDetected")
            appendLine("• Red Pixels Matched: $redEdgesDetected")
            appendLine("• Raw Volume Ratio: ${String.format("%.2f", totalEdges.toFloat() / bw)}")
            appendLine("• Core Pixel Bias: ${if (greenEdgesDetected > redEdgesDetected) "BUY BIAS" else "SELL BIAS"}")
            append("• Calculation Mode: Background Scanning")
        }

        val displayBuyPercent = if (signal == "BUY") 50f + (confidence / 2f) else 50f - (confidence / 2f)
        val displaySellPercent = 100f - displayBuyPercent

        val patternDisplay = PatternEngine.formatDisplay(scoredPatterns, topN = 5)
        val rsiInfo = "RSI $rsiValue — " + if (rsiValue >= 70) "Overbought" else if (rsiValue <= 30) "Oversold" else "Normal Zone"
        val volumeInfo = if (totalEdges.toFloat() / zoneArea > 0.12f) "🔥 High Volume" else "⚡ Medium Volume"

        val entrySlTarget = if (signal == "BUY")
            "✅ Entry: Above High\n🛑 SL: Below Pattern Low\n🎯 Target: Resistance Zone"
        else
            "✅ Entry: Below Low\n🛑 SL: Above Pattern High\n🎯 Target: Support Zone"

        val summary = buildString {
            appendLine("🎯 AI FINAL SIGNAL: $signal")
            appendLine("🔍 Triggered By: $triggerReason")
            appendLine("📈 Chart Location: $currentPosition")
            append(entrySlTarget)
        }

        return AnalysisResult(
            isValidChart = true,
            signal = signal,
            confidence = confidence,
            greenPercent = (greenEdgesDetected.toFloat() / totalEdges) * 100f,
            redPercent = (redEdgesDetected.toFloat() / totalEdges) * 100f,
            displayBuyPercent = displayBuyPercent,
            displaySellPercent = displaySellPercent,
            hasPatterns = hasPatterns,
            trend = "Trend: ${if(isGreenCandle)"UP" else "DOWN"} ($currentPosition)",
            volumeInfo = volumeInfo,
            sentiment = if (signal == "BUY") "Bullish" else "Bearish",
            supportResistance = entrySlTarget,
            summary = summary,
            investorInfo = if (confidence >= 70) "✅ 1:3 (Strong)" else "⚠️ 1:2 (Medium)",
            rsiValue = rsiValue,
            rsiInfo = rsiInfo,
            riskLevel = if (confidence >= 75) "🟢 LOW RISK" else "🟡 MEDIUM RISK",
            riskColor = if (confidence >= 75) "#4CAF50" else "#FF9800",
            keyFactors = triggerReason,
            signalReason = triggerReason,
            detectedPatterns = scoredPatterns,
            patternDisplay = patternDisplay,
            currentPosition = currentPosition,
            candleCount = candleResult.candleCount,
            candleEngineSignal = candleResult.signal,
            candleEngineConfidence = candleResult.confidence,
            candleContext = candleResult.patternContext,
            candleDetails = candleResult.detailsStr,
            weightBreakdown = "Trigger: $triggerReason | Position: $currentPosition",
            rightSidePixelBox = rightSidePixelBox
        )
    }
}
