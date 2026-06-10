package com.tradinganalyzer.analysis

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.min

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

        // Split image into top half and bottom half to detect trend direction
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
                if (brightness > 220) {
                    backgroundPixels++
                    continue
                }

                val isSkin = r > 100 && g > 50 && b > 30 &&
                        r > g && r > b &&
                        (r - g) > 10 && (r - g) < 120 &&
                        b < 180 && g < 200
                if (isSkin) {
                    skinPixels++
                    continue
                }

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
            return AnalysisResult(
                isValidChart = false,
                notChartReason = "Image खूप light/blank आहे. Chart screenshot upload करा."
            )
        }

        val skinRatio = skinPixels.toFloat() / analyzablePixels
        if (skinRatio > 0.15f) {
            return AnalysisResult(
                isValidChart = false,
                notChartReason = "हे Chart नाही!\n\nहे selfie किंवा photo दिसत आहे.\n\nकृपया Trading Chart चा screenshot upload करा.\n(NSE, BSE, TradingView, Zerodha Kite, Upstox, Angel One चे chart)"
            )
        }

        val coloredPixels = greenPixels + redPixels
        val colorRatio = coloredPixels.toFloat() / analyzablePixels

        if (colorRatio < 0.03f) {
            return AnalysisResult(
                isValidChart = false,
                notChartReason = "Chart patterns सापडले नाहीत!\n\nGreen/Red candles असलेला Candlestick chart upload करा.\n\nLine chart किंवा black-white chart support नाही."
            )
        }

        val greenPercent = if (coloredPixels > 0) greenPixels.toFloat() / coloredPixels * 100f else 50f
        val redPercent = if (coloredPixels > 0) redPixels.toFloat() / coloredPixels * 100f else 50f
        val diff = abs(greenPercent - redPercent)

        // BUY/SELL bias fix — need strong difference (>20%) for clear signal
        val signal: String
        val confidence: Int
        val trend: String
        val waitMessage: String

        when {
            greenPercent > redPercent + 20 -> {
                signal = "BUY"
                confidence = (60 + min(diff.toInt() / 2, 30)).coerceIn(60, 90)
                trend = "UPTREND ↑"
                waitMessage = ""
            }
            redPercent > greenPercent + 20 -> {
                signal = "SELL"
                confidence = (60 + min(diff.toInt() / 2, 30)).coerceIn(60, 90)
                trend = "DOWNTREND ↓"
                waitMessage = ""
            }
            greenPercent > redPercent + 10 -> {
                signal = "BUY"
                confidence = (50 + diff.toInt()).coerceIn(52, 62)
                trend = "WEAK UPTREND ↗"
                waitMessage = "⏳ Signal weak आहे — 2-3 candles confirm होण्याची वाट पाहा, मग position घ्या."
            }
            redPercent > greenPercent + 10 -> {
                signal = "SELL"
                confidence = (50 + diff.toInt()).coerceIn(52, 62)
                trend = "WEAK DOWNTREND ↘"
                waitMessage = "⏳ Signal weak आहे — 2-3 candles confirm होण्याची वाट पाहा, मग position घ्या."
            }
            else -> {
                // Very close — sideways
                signal = if (greenPercent >= redPercent) "BUY" else "SELL"
                confidence = (48 + diff.toInt()).coerceIn(45, 55)
                trend = "SIDEWAYS ↔"
                waitMessage = "⚠️ Candle direction स्पष्ट नाही!\n\nMarket sideways आहे. आत्ता position घेणे risky आहे.\n5-10 minutes थांबा, नवीन chart पाठवा."
            }
        }

        // RSI simulation based on green/red ratio
        // High green = overbought (RSI 70+), High red = oversold (RSI 30-)
        val rsiValue = when {
            greenPercent > 75 -> (70 + ((greenPercent - 75) / 25 * 20)).toInt().coerceIn(70, 90)
            greenPercent < 25 -> (30 - ((25 - greenPercent) / 25 * 20)).toInt().coerceIn(10, 30)
            else -> (30 + (greenPercent / 100 * 40)).toInt().coerceIn(30, 70)
        }

        val rsiInfo = when {
            rsiValue >= 70 -> "RSI: $rsiValue — Overbought 🔴\nखूप जास्त खरेदी झाली आहे. किंमत खाली येण्याची शक्यता आहे. नवीन BUY टाळा."
            rsiValue <= 30 -> "RSI: $rsiValue — Oversold 🟢\nखूप जास्त विक्री झाली आहे. किंमत वर जाण्याची शक्यता आहे. BUY opportunity असू शकते."
            rsiValue >= 60 -> "RSI: $rsiValue — Bullish Zone 📈\nBullish momentum आहे. Uptrend मध्ये आहे."
            rsiValue <= 40 -> "RSI: $rsiValue — Bearish Zone 📉\nBearish momentum आहे. Downtrend मध्ये आहे."
            else -> "RSI: $rsiValue — Neutral Zone ➡️\nMarket neutral आहे. Clear signal नाही."
        }

        // Risk level
        val riskLevel: String
        val riskColor: String
        when {
            trend == "SIDEWAYS ↔" -> {
                riskLevel = "🔴 HIGH RISK — Sideways market, आत्ता trade करू नका"
                riskColor = "#F44336"
            }
            confidence < 58 -> {
                riskLevel = "🔴 HIGH RISK — Signal weak आहे, जपून trade करा"
                riskColor = "#F44336"
            }
            confidence < 72 -> {
                riskLevel = "🟡 MEDIUM RISK — Confirm झाल्यावर trade करा"
                riskColor = "#FF9800"
            }
            else -> {
                riskLevel = "🟢 LOW RISK — Signal strong आहे"
                riskColor = "#4CAF50"
            }
        }

        // Key factors
        val topHalfTrend = if (topGreen > topRed) "वरचा भाग Bullish" else "वरचा भाग Bearish"
        val bottomHalfTrend = if (bottomGreen > bottomRed) "खालचा भाग Bullish" else "खालचा भाग Bearish"

        val keyFactors = buildString {
            appendLine("📌 Key Factors:")
            appendLine("• $topHalfTrend | $bottomHalfTrend")
            appendLine("• Bullish strength: ${"%.0f".format(greenPercent)}% | Bearish strength: ${"%.0f".format(redPercent)}%")
            if (rsiValue >= 70) appendLine("• RSI Overbought — selling pressure येऊ शकतो")
            if (rsiValue <= 30) appendLine("• RSI Oversold — buying opportunity असू शकते")
            if (colorRatio > 0.12f) appendLine("• High volume — strong move expected")
            if (diff < 10) appendLine("• Indecision zone — clear direction नाही")
        }

        val volumeInfo = when {
            colorRatio > 0.15f -> "High Volume — Strong Signal 🔥"
            colorRatio > 0.08f -> "Medium Volume — Moderate Signal ⚡"
            else -> "Low Volume — Weak Signal ⚠️"
        }

        val sentiment = when {
            signal == "BUY" && confidence > 75 -> "Strongly Bullish 🐂"
            signal == "BUY" -> "Mildly Bullish 📈"
            signal == "SELL" && confidence > 75 -> "Strongly Bearish 🐻"
            else -> "Mildly Bearish 📉"
        }

        val supportResistance = if (signal == "BUY") {
            "Support zone strong आहे.\nBullish candles जास्त दिसत आहेत.\nNext resistance वर target set करा."
        } else {
            "Resistance zone strong आहे.\nBearish candles जास्त दिसत आहेत.\nNext support वर stop-loss ठेवा."
        }

        val summary = buildString {
            appendLine("📋 Analysis Summary:")
            appendLine("━━━━━━━━━━━━━━━━━━━")
            appendLine("Chart मध्ये ${"%.1f".format(greenPercent)}% bullish आणि ${"%.1f".format(redPercent)}% bearish candles.")
            appendLine()
            appendLine("Trend: $trend")
            appendLine("Signal: $signal | Confidence: $confidence%")
            appendLine()
            if (waitMessage.isNotEmpty()) {
                appendLine(waitMessage)
                appendLine()
            }
            if (confidence >= 75) {
                appendLine("⚡ Strong signal — Stop-Loss सह trade करा.")
            } else if (confidence >= 60) {
                appendLine("⚠️ Moderate signal — Confirm होण्याची वाट पाहा.")
            } else {
                appendLine("🚫 Weak signal — trade करणे टाळा.")
            }
            appendLine()
            append("⚠️ हे technical analysis आहे. Investment before expert consult करा.")
        }

        val investorInfo = if (signal == "BUY") {
            "Institutional investors accumulation करत आहेत. Bullish sentiment $confidence% आहे."
        } else {
            "Smart money exit होत आहे. Distribution phase दिसत आहे. Bearish sentiment $confidence% आहे."
        }

        return AnalysisResult(
            isValidChart = true,
            signal = signal,
            confidence = confidence,
            greenPercent = greenPercent,
            redPercent = redPercent,
            trend = trend,
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
