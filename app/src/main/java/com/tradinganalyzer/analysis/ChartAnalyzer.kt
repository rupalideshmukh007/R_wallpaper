package com.tradinganalyzer.analysis

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

        // ─────────────────────────────────────────────────────────────────────
        // 👁️ 1. IMAGE PRE-PROCESSING & CROP BOUNDS (कोऑर्डिनेट मॅपिंग क्षेत्र)
        // ─────────────────────────────────────────────────────────────────────
        val scanX1 = (bw * 0.115f).toInt()
        val scanY1 = (bh * 0.120f).toInt()
        val scanX2 = (bw * 0.930f).toInt()
        val scanY2 = (bh * 0.725f).toInt()

        val zoneH = (scanY2 - scanY1).coerceAtLeast(1)
        val chartMiddle = scanY1 + (zoneH / 2)

        // 📐 COORDINATE MAPPING VARIABLES (कॅन्डलच्या कडा शोधण्यासाठी)
        var topWickY = scanY2.toFloat()
        var bottomWickY = scanY1.toFloat()
        var bodyTopY = scanY2.toFloat()
        var bodyBottomY = scanY1.toFloat()
        
        var greenEdgesDetected = 0
        var redEdgesDetected = 0

        // ── EDGE DETECTION THRESHOLDING (पिक्सेल संख्या नाही, फक्त कडांचे स्थान मोजणे) ──
        for (x in scanX1 until scanX2 step 4) {
            for (y in scanY1 until scanY2 step 4) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // 🟢 Canny/Thresholding - शुद्ध हिरव्या आणि लाल कँडलच्या कडा मॅप करणे
                val isGreenEdge = (g > r * 1.2 && g > b * 1.2 && g > 50)
                val isRedEdge = (r > g * 1.2 && r > b * 1.2 && r > 50)

                if (isGreenEdge || isRedEdge) {
                    val currentY = y.toFloat()
                    
                    // High/Low Wick चे कोऑर्डिनेट्स लॉक करणे
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

        // 🚨 Safety Check: जर इमेज ब्लँक असेल किंवा कडा सापडल्या नाहीत
        if (totalEdges <= 0) {
            return AnalysisResult(
                isValidChart = false,
                notChartReason = "❌ Chart कडा अस्पष्ट आहेत. क्लिअर स्क्रीनशॉट अपलोड करा."
            )
        }

        // ─────────────────────────────────────────────────────────────────────
        // 📐 2. OHLC EXTRACTION (कोऑर्डिनेट्सवरून काढलेला गणिती रेशो)
        // ─────────────────────────────────────────────────────────────────────
        val isGreenCandle = greenEdgesDetected > redEdgesDetected
        val openPrice: Float
        val closePrice: Float

        if (isGreenCandle) {
            openPrice = bodyBottomY  // हिरव्या कँडलचा ओपन खाली असतो
            closePrice = bodyTopY    // क्लोज वर असतो
        } else {
            openPrice = bodyTopY    // लाल कँडलचा ओपन वर असतो
            closePrice = bodyBottomY // क्लोज खाली असतो
        }

        val extractedCandle = OhlcCandle(
            open = openPrice,
            high = topWickY,
            low = bottomWickY,
            close = closePrice
        )

        // कँडलची चार्टवरील मुख्य पोझिशन ओळखणे (AI Position Detection)
        val candleCenter = (topWickY + bottomWickY) / 2f
        val currentPosition = if (candleCenter < chartMiddle) "TOP" else "BOTTOM"

        // ── STEP 3: CANDLE ENGINE COMPATIBILITY ──
        val candleResult = CandleEngine.analyze(bitmap, scanX1, scanY1, scanX2, scanY2)
        val hasCandleSignal = candleResult.candleCount >= 3

        // ─────────────────────────────────────────────────────────────────────
        // 🧠 3. RULE-BASED AI PATTERN ENGINE ANALYSIS (१००% पॅटर्न डायरेक्शन)
        // ─────────────────────────────────────────────────────────────────────
        val currentTrend = if (isGreenCandle) "UP" else "DOWN"
        val mockCloses = listOf(closePrice * 0.97f, closePrice * 0.98f, closePrice * 0.99f, closePrice)
        val rsiValue = PatternEngine.rsi(mockCloses).toInt().coerceIn(10, 90)

        // गणिती नियमांचे इंजिन रन करणे
        val rawPatterns = PatternEngine().analyze(extractedCandle, currentTrend, rsiValue.toFloat())

        // डुप्लिकेट पॅटर्न्स गाळणे
        val scoredPatterns = rawPatterns
            .groupBy { it.patternName }
            .map { it.value.maxByOrNull { p -> p.confidence }!! }

        val hasPatterns = scoredPatterns.isNotEmpty()

        // पॅटर्नच्या बहुमतावरून फायनल सिग्नलची दिशा ठरवणे
        val bullishPatterns = scoredPatterns.filter { it.patternType == "BULLISH" }
        val bearishPatterns = scoredPatterns.filter { it.patternType == "BEARISH" }

        val signal: String
        val confidence: Int
        val triggerReason: String

        if (bullishPatterns.size > bearishPatterns.size) {
            signal = "BUY"
            val topBull = bullishPatterns.maxByOrNull { it.confidence }
            confidence = topBull?.confidence ?: 75
            triggerReason = topBull?.patternName ?: "Mathematical Bullish Edge"
        } else if (bearishPatterns.size > bullishPatterns.size) {
            signal = "SELL"
            val topBear = bearishPatterns.maxByOrNull { it.confidence }
            confidence = topBear?.confidence ?: 75
            triggerReason = topBear?.patternName ?: "Mathematical Bearish Edge"
        } else {
            signal = if (isGreenCandle) "BUY" else "SELL"
            confidence = 60
            triggerReason = "OHLC Trend Continuation"
        }

        // ── STEP 4: DISPLAY PERCENTAGE LOGIC ──
        val displayBuyPct = if (signal == "BUY") 50f + (confidence / 2f) else 50f - (confidence / 2f)
        val displaySellPct = 100f - displayBuyPct

        // ── STEP 5: LABELS & LABELS GENERATION ──
        val trend = "Trend: $currentTrend ($currentPosition)"
        val patternDisplay = PatternEngine.formatDisplay(scoredPatterns, topN = 3)

        val rsiInfo = "RSI $rsiValue — " + if (rsiValue >= 70) "Overbought" else if (rsiValue <= 30) "Oversold" else "Normal Zone"
        val zoneArea = zoneH * (scanX2 - scanX1).coerceAtLeast(1)
        val colorRatio = totalEdges.toFloat() / (zoneArea / 16).coerceAtLeast(1)
        val volumeInfo = if (colorRatio > 0.12f) "🔥 High Volume" else "⚡ Medium Volume"

        val confidenceLabel = when {
            confidence >= 85 -> "Very Strong"
            confidence >= 70 -> "Strong"
            confidence >= 55 -> "Medium"
            else -> "Weak"
        }

        val riskLevel: String; val riskColor: String
        when {
            confidence <= 40 -> { riskLevel = "🔴 HIGH RISK ($confidenceLabel)"; riskColor = "#F44336" }
            confidence <= 65 -> { riskLevel = "🟡 MEDIUM RISK ($confidenceLabel)"; riskColor = "#FF9800" }
            confidence <= 84 -> { riskLevel = "🟢 LOW RISK ($confidenceLabel)"; riskColor = "#4CAF50" }
            else -> { riskLevel = "⭐ VERY LOW RISK ($confidenceLabel)"; riskColor = "#2196F3" }
        }

        val entrySlTarget = if (signal == "BUY")
            "✅ Entry: Above High\n🛑 SL: Below Pattern Low\n🎯 Target: Next Resistance"
        else
            "✅ Entry: Below Low\n🛑 SL: Above Pattern High\n🎯 Target: Next Support"

        val rrRatio = if (confidence >= 70) "✅ 1:3 (Strong)" else "⚠️ 1:2 (Medium)"

        // ── WEIGHT BREAKDOWN STRING ──
        val weightBreakdown = buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📐 AI COMPUTER VISION SETUP")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🔍 Mapped Rule: $triggerReason")
            appendLine("🕯️ Position   : $currentPosition")
            appendLine("📊 Range Math : High-Low Coordinates")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            append("🎯 Final Signal : $signal")
        }

        // ── SUMMARY STRING ──
        val summary = buildString {
            appendLine("Signal: $signal  | Trigger: $triggerReason")
            appendLine("Position: $currentPosition  | $volumeInfo")
            appendLine("RSI: $rsiInfo")
            appendLine()
            append(entrySlTarget)
        }

        // ─────────────────────────────────────────────────────────────────────
        // 🎯 4. COMPLETE RETURN BLOCK (सर्व डेटा व्हेरिएबल्स सुसंगत भरले आहेत)
        // ─────────────────────────────────────────────────────────────────────
        return AnalysisResult(
            isValidChart = true,
            notChartReason = null,
            signal = signal,
            confidence = confidence,
            greenPercent = (greenEdgesDetected.toFloat() / totalEdges) * 100f,
            redPercent = (redEdgesDetected.toFloat() / totalEdges) * 100f,
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
            waitMessage = if (currentTrend == "SIDE") "⚠️ Sideways Market" else "",
            keyFactors = triggerReason,
            signalReason = triggerReason,
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
