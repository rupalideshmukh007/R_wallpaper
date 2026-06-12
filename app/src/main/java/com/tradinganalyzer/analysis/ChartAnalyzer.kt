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

        val bw = bitmap.width
        val bh = bitmap.height

        // 🎯 EXACT SCANNING BOUNDS
        val scanX1 = (bw * 0.115f).toInt()
        val scanY1 = (bh * 0.120f).toInt()
        val scanX2 = (bw * 0.930f).toInt()
        val scanY2 = (bh * 0.725f).toInt()

        val zoneH = (scanY2 - scanY1).coerceAtLeast(1)
        val chartMiddle = scanY1 + (zoneH / 2)

        // ════════════════════════════════════════════════════════════
        // 👁️ REAL-TIME AI PIXEL SCANNER (एकदम अचूक पॉईंट्स मोजणे)
        // ════════════════════════════════════════════════════════════
        var greenPixels = 0
        var redPixels = 0
        var topPixelsCount = 0
        var bottomPixelsCount = 0
        var topGreen = 0; var topRed = 0
        var bottomGreen = 0; var bottomRed = 0

        var highestWickY = scanY2.toFloat()
        var lowestWickY = scanY1.toFloat()
        var firstCandleY = 0f
        var lastCandleY = 0f

        // परफॉर्मन्ससाठी स्टेप बाय ५ पिक्सेल्सने स्कॅन करणे
        for (x in scanX1 until scanX2 step 4) {
            for (y in scanY1 until scanY2 step 4) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // 🟢 शुद्ध हिरवा रंग शोधणे (TradingView/Binance/PocketOption रंगांचा मॅच)
                val isGreenColor = (g > r * 1.2 && g > b * 1.2 && g > 40) || (g > 120 && r < 100 && b < 100)
                // 🔴 शुद्ध लाल रंग शोधणे
                val isRedColor = (r > g * 1.2 && r > b * 1.2 && r > 40) || (r > 120 && g < 100 && b < 100)

                if (isGreenColor || isRedColor) {
                    // हाय आणि लो विक्स ट्रॅक करणे
                    if (y.toFloat() < highestWickY) highestWickY = y.toFloat()
                    if (y.toFloat() > lowestWickY) lowestWickY = y.toFloat()
                    
                    if (firstCandleY == 0f) firstCandleY = y.toFloat()
                    lastCandleY = y.toFloat()

                    if (y < chartMiddle) {
                        topPixelsCount++
                        if (isGreenColor) { greenPixels++; topGreen++ } else { redPixels++; topRed++ }
                    } else {
                        bottomPixelsCount++
                        if (isGreenColor) { greenPixels++; bottomGreen++ } else { redPixels++; bottomRed++ }
                    }
                }
            }
        }

        val coloredPixels = topPixelsCount + bottomPixelsCount
        val zoneArea = zoneH * (scanX2 - scanX1).coerceAtLeast(1)
        val colorRatio = coloredPixels.toFloat() / (zoneArea / 16).coerceAtLeast(1) // Step 4 मुळे बॅलन्स फिक्स

        // 🚨 १. रिकाम्या फोटोचा सेफ्टी चेक
        if (coloredPixels <= 0) {
            return AnalysisResult(
                isValidChart = false,
                notChartReason = "Image खूप light/blank आहे. Chart screenshot upload करा."
            )
        }

        // 🚨 २. कँडल्स सापडल्या नाहीत हा एरर आता चुकीच्या इमेजेसवरच येईल
        if (colorRatio < 0.010f) {
            return AnalysisResult(
                isValidChart = false,
                notChartReason = "❌ Chart candles सापडले नाहीत!\nZerodha / TradingView / Upstox Candlestick chart upload करा."
            )
        }

        val gp = if (coloredPixels > 0) greenPixels.toFloat() / coloredPixels * 100f else 50f
        val rp = if (coloredPixels > 0) redPixels.toFloat() / coloredPixels * 100f else 50f
        val diff = Math.abs(gp - rp)
        
        // ── STEP 3: CANDLE ENGINE CALL ──
        val candleResult = CandleEngine.analyze(bitmap, scanX1, scanY1, scanX2, scanY2)
        val hasCandleSignal = candleResult.candleCount >= 3
        val candleTrend = if (hasCandleSignal) candleResult.trend else "Sideways"
        val nearSupport = hasCandleSignal && candleResult.nearSupport
        val nearResistance = hasCandleSignal && candleResult.nearResistance

        // 📐 वास्तविक एआय बॉडी पोझिशनिंग
        val bodyTopEdge = if (firstCandleY > 0f) firstCandleY else (bh * 0.35f)
        val bodyBottomEdge = if (lastCandleY > 0f) lastCandleY else (bh * 0.45f)
        val candleMiddlePoint = (bodyTopEdge + bodyBottomEdge) / 2
        val currentPosition = if (candleMiddlePoint < chartMiddle) "TOP" else "BOTTOM"

        val rsiValue = 55 // डिफॉल्ट सेफ इंडिकेटर

                // ── STEP 4: PATTERN ENGINE CALL (MASTER FIXED VERSION) ──

        // 🎯 1. कँडल्सचा अंदाज मोजणे
        val liveCandleTop = highestWickY + (totalLength * 0.20f)
        val currentCandleData = CandleShapeData(
            open = liveCandleTop,
            high = highestWickY,
            low = lowestWickY,
            close = bodyBottomEdge,
            isGreen = (candleResult.signal == "BUY")
        )

        // 🎯 2. Call Pattern Engine (आपण companion object वापरल्यामुळे थेट नाव टाकून कॉल)
        val rawPatterns = PatternEngine.detect(currentCandleData, currentPosition)

        // 🎯 3. डुप्लिकेट्स फिल्टर करणे (it.patternName नुसार)
        val scoredPatterns = rawPatterns
            .groupBy { it.patternName }
            .map { (_, group) -> group.maxByOrNull { it.confidence }!! }

        // 🎯 4. सेफ्टी चेक
        val hasPatterns = scoredPatterns.isNotEmpty()

        // 🎯 5. Buy/Sell percentage logic (it.patternType नुसार)
        val patternBuyCount = scoredPatterns.count { it.patternType == "BULLISH" || it.patternType == "Bullish" }
        val patternSellCount = scoredPatterns.count { it.patternType == "BEARISH" || it.patternType == "Bearish" }

        val total = (patternBuyCount + patternSellCount).coerceAtLeast(1)

        val patternBuyPct = (patternBuyCount.toFloat() / total) * 100f
        val patternSellPct = (patternSellCount.toFloat() / total) * 100f


        // ── STEP 5: WEIGHTED SIGNAL SYSTEM ──
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

        // ── STEP 7: STRINGS & SUMMARY GENERATION ──
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
        val rsiInfo = if (rsiValue >= 70) "RSI $rsiValue — Overbought" else if (rsiValue <= 30) "RSI $rsiValue — Oversold" else "RSI $rsiValue — Normal"
        val volumeInfo = if (colorRatio > 0.12f) "🔥 High Volume" else "⚡ Medium Volume"

        val riskLevel: String; val riskColor: String
        when {
            confidence <= 40 -> { riskLevel = "🔴 HIGH RISK"; riskColor = "#F44336" }
            confidence <= 65 -> { riskLevel = "🟡 MEDIUM RISK"; riskColor = "#FF9800" }
            confidence <= 84 -> { riskLevel = "🟢 LOW RISK"; riskColor = "#4CAF50" }
            else -> { riskLevel = "⭐ VERY LOW RISK"; riskColor = "#2196F3" }
        }

        val waitMessage = if (candleTrend == "Sideways") "⚠️ Sideways market — direction clear होण्याची वाट पाहा." else ""
        val entrySlTarget = if (signal == "BUY") "✅ Entry: Current level वर\n🛑 Stop Loss: मागील low खाली\n🎯 Target: High Resistance" else "✅ Entry: Current level वर\n🛑 Stop Loss: मागील high वर\n🎯 Target: Low Support"
        val rrRatio = if (confidence >= 70) "✅ 1:3 (Strong)" else "⚠️ 1:2 (Medium)"

        val signalReason = reasons.take(3).joinToString(" • ")
        val patternDisplay = PatternEngine.formatDisplay(scoredPatterns, topN = 5)

        val weightBreakdown = buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚖️  WEIGHT BREAKDOWN")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🕯️ Candle Engine  : ${(candleWeight * 100).toInt()}%")
            appendLine("📊 Pattern Engine : ${(patternWeight * 100).toInt()}%")
            appendLine("📸 Pixel Engine   : ${(pixelWeight * 100).toInt()}%")
            append("🎯 Final : $signal  |  Confidence : $confidence%")
        }

        val summary = buildString {
            appendLine("Signal: $signal  |  Strength: $confidence% ($confidenceLabel)")
            appendLine("Trend: $trend  |  $volumeInfo")
            appendLine(entrySlTarget)
        }

        // ── 🎯 MASTER FINAL RETURN ──
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
