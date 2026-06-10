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
    val keyFactors: String = ""
)

object ChartAnalyzer {

    fun analyze(bitmap: Bitmap): AnalysisResult {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

        var greenPixels = 0L
        var redPixels = 0L
        var skinPixels = 0L
        var totalPixels = 0L
        var backgroundPixels = 0L
        var topGreen = 0L
        var topRed = 0L
        var bottomGreen = 0L
        var bottomRed = 0L

        val height = scaledBitmap.height
        val width = scaledBitmap.width

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = scaledBitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val alpha = Color.alpha(pixel)

                if (alpha < 50) continue
                totalPixels++

                val brightness = (r + g + b) / 3
                if (brightness > 220) { backgroundPixels++; continue }

                val isSkin = r > 100 && g > 50 && b > 30 &&
                        r > g && r > b && (r - g) > 10 && (r - g) < 120 && b < 180
                if (isSkin) { skinPixels++; continue }

                val maxChannel = maxOf(r, g, b)
                val minChannel = minOf(r, g, b)
                val saturation = if (maxChannel > 0) (maxChannel - minChannel).toFloat() / maxChannel else 0f

                if (saturation > 0.2f) {
                    val isGreen = g > r && g > b && g > 60
                    val isRed = r > g && r > b && r > 60
                    if (isGreen) {
                        greenPixels++
                        if (y < height / 2) topGreen++ else bottomGreen++
                    } else if (isRed) {
                        redPixels++
                        if (y < height / 2) topRed++ else bottomRed++
                    }
                }
            }
        }

        scaledBitmap.recycle()

        val analyzablePixels = totalPixels - backgroundPixels
        if (analyzablePixels <= 0) {
            return AnalysisResult(isValidChart = false,
                notChartReason = "Image खूप light/blank आहे. Chart screenshot upload करा.")
        }

        val skinRatio = skinPixels.toFloat() / analyzablePixels
        if (skinRatio > 0.15f) {
            return AnalysisResult(isValidChart = false,
                notChartReason = "हे Chart नाही!\n\nSelfie किंवा photo दिसत आहे.\n\nकृपया Zerodha, Upstox, TradingView chart चा screenshot upload करा.")
        }

        val coloredPixels = greenPixels + redPixels
        val colorRatio = coloredPixels.toFloat() / analyzablePixels

        if (colorRatio < 0.03f) {
            return AnalysisResult(isValidChart = false,
                notChartReason = "Chart patterns सापडले नाहीत!\nGreen/Red candles असलेला Candlestick chart upload करा.")
        }

        // BUY % आणि SELL % — जे जास्त तेच signal
        val greenPercent = if (coloredPixels > 0) greenPixels.toFloat() / coloredPixels * 100f else 50f
        val redPercent = if (coloredPixels > 0) redPixels.toFloat() / coloredPixels * 100f else 50f
        val diff = Math.abs(greenPercent - redPercent)

        val signal = if (greenPercent >= redPercent) "BUY" else "SELL"
        val winningPercent = if (signal == "BUY") greenPercent else redPercent
        val confidence = winningPercent.toInt().coerceIn(50, 95)

        // 1. TREND
        val trend = when {
            topGreen > topRed && bottomGreen > bottomRed -> "UPTREND"
            topRed > topGreen && bottomRed > bottomGreen -> "DOWNTREND"
            topGreen > topRed && bottomRed > bottomGreen -> "REVERSAL"
            else -> "SIDEWAYS"
        }
        val trendDisplay = when (trend) {
            "UPTREND" -> "UPTREND ↑ (Bullish)"
            "DOWNTREND" -> "DOWNTREND ↓ (Bearish)"
            "REVERSAL" -> "REVERSAL ↕ (Trend बदलतोय)"
            else -> "SIDEWAYS ↔ (No clear direction)"
        }

        // 2. RSI
        val rsiValue = when {
            greenPercent > 75 -> (70 + ((greenPercent - 75) / 25 * 20)).toInt().coerceIn(70, 90)
            greenPercent < 25 -> (30 - ((25 - greenPercent) / 25 * 20)).toInt().coerceIn(10, 30)
            else -> (30 + (greenPercent / 100 * 40)).toInt().coerceIn(30, 70)
        }
        val rsiInfo = when {
            rsiValue >= 70 -> "RSI $rsiValue — Overbought: खूप खरेदी झाली, किंमत खाली येण्याची शक्यता"
            rsiValue <= 30 -> "RSI $rsiValue — Oversold: खूप विक्री झाली, किंमत वर जाण्याची शक्यता"
            rsiValue >= 60 -> "RSI $rsiValue — Bullish zone, uptrend मध्ये आहे"
            rsiValue <= 40 -> "RSI $rsiValue — Bearish zone, downtrend मध्ये आहे"
            else -> "RSI $rsiValue — Neutral, clear signal नाही"
        }

        // 3. VOLUME
        val volumeInfo = when {
            colorRatio > 0.15f -> "High Volume — Signal strong आहे"
            colorRatio > 0.08f -> "Medium Volume — Signal moderate आहे"
            else -> "Low Volume — Signal weak आहे, confirm करा"
        }

        // 4. CHART PATTERN
        val chartPattern = when {
            topGreen > topRed * 1.5 && bottomRed > bottomGreen * 1.5 -> "Evening Star pattern (Bearish reversal)"
            topRed > topGreen * 1.5 && bottomGreen > bottomRed * 1.5 -> "Morning Star pattern (Bullish reversal)"
            greenPercent > 70 && colorRatio > 0.12f -> "Three White Soldiers (Strong Bullish)"
            redPercent > 70 && colorRatio > 0.12f -> "Three Black Crows (Strong Bearish)"
            diff < 8f -> "Doji / Indecision candles (Market uncertain)"
            signal == "BUY" && diff > 20 -> "Bullish Engulfing pattern"
            signal == "SELL" && diff > 20 -> "Bearish Engulfing pattern"
            else -> "No specific pattern — general ${if (signal == "BUY") "bullish" else "bearish"} candles"
        }

        // 5. RISK/REWARD
        val rrRatio = when {
            diff >= 25 -> "Risk:Reward = 1:3 (Excellent)"
            diff >= 15 -> "Risk:Reward = 1:2 (Good)"
            diff >= 8 -> "Risk:Reward = 1:1.5 (Moderate)"
            else -> "Risk:Reward = 1:1 (Poor — trade टाळा)"
        }

        // 6. SUPPORT/RESISTANCE + ENTRY/SL/TARGET
        val supportResistance = buildString {
            if (signal == "BUY") {
                appendLine("Support zone मजबूत आहे")
                appendLine("Entry Zone: आत्ताची किंमत / next green candle close")
                appendLine("Stop Loss: मागील swing low च्या खाली")
                appendLine("Target: मागील high / next resistance level")
            } else {
                appendLine("Resistance zone मजबूत आहे")
                appendLine("Entry Zone: आत्ताची किंमत / next red candle close")
                appendLine("Stop Loss: मागील swing high च्या वर")
                appendLine("Target: मागील low / next support level")
            }
        }

        // Risk level
        val riskLevel: String
        val riskColor: String
        when {
            diff < 8f -> { riskLevel = "HIGH RISK — Signal खूप weak आहे"; riskColor = "#F44336" }
            diff < 20f -> { riskLevel = "MEDIUM RISK — Confirm झाल्यावर trade करा"; riskColor = "#FF9800" }
            else -> { riskLevel = "LOW RISK — Signal strong आहे"; riskColor = "#4CAF50" }
        }

        // Wait message
        val waitMessage = if (diff < 5f) {
            "BUY आणि SELL जवळजवळ सारखे आहेत (${String.format("%.1f", greenPercent)}% vs ${String.format("%.1f", redPercent)}%)\n2-3 candles confirm होण्याची वाट पाहा."
        } else ""

        val sentiment = when {
            signal == "BUY" && diff > 20 -> "Strongly Bullish"
            signal == "BUY" -> "Mildly Bullish"
            signal == "SELL" && diff > 20 -> "Strongly Bearish"
            else -> "Mildly Bearish"
        }

        val investorInfo = if (signal == "BUY")
            "Bullish momentum: ${String.format("%.1f", greenPercent)}% | Bearish pressure: ${String.format("%.1f", redPercent)}%"
        else
            "Bearish momentum: ${String.format("%.1f", redPercent)}% | Bullish pressure: ${String.format("%.1f", greenPercent)}%"

        val keyFactors = buildString {
            appendLine("1. Trend: $trendDisplay")
            appendLine("2. RSI: $rsiInfo")
            appendLine("3. Volume: $volumeInfo")
            appendLine("4. Pattern: $chartPattern")
            appendLine("5. $rrRatio")
        }

        val summary = buildString {
            appendLine("BUY Chance: ${String.format("%.1f", greenPercent)}%  |  SELL Chance: ${String.format("%.1f", redPercent)}%")
            appendLine("Signal: $signal | Confidence: $confidence%")
            appendLine()
            if (waitMessage.isNotEmpty()) appendLine(waitMessage)
            else if (diff >= 20) appendLine("Signal strong — Stop-Loss सह trade करा.")
            else appendLine("Signal moderate — confirm होण्याची वाट पाहा.")
            appendLine()
            append("हे technical analysis आहे. Investment before expert consult करा.")
        }

        return AnalysisResult(
            isValidChart = true,
            signal = signal,
            confidence = confidence,
            greenPercent = greenPercent,
            redPercent = redPercent,
            trend = trendDisplay,
            volumeInfo = volumeInfo,
            sentiment = sentiment,
            supportResistance = supportResistance,
            summary = summary,
            investorInfo = investorInfo,
            rsiValue = rsiValue,
            rsiInfo = rsiInfo,
            riskLevel = riskLevel,
            riskColor = riskColor,
            waitMessage = waitMessage,
            keyFactors = keyFactors
        )
    }
}
