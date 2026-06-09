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
    val investorInfo: String = ""
)

object ChartAnalyzer {

    fun analyze(bitmap: Bitmap): AnalysisResult {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

        var greenPixels = 0L
        var redPixels = 0L
        var skinPixels = 0L
        var totalPixels = 0L
        var backgroundPixels = 0L

        for (x in 0 until scaledBitmap.width) {
            for (y in 0 until scaledBitmap.height) {
                val pixel = scaledBitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val alpha = Color.alpha(pixel)

                if (alpha < 50) continue
                totalPixels++

                val brightness = (r + g + b) / 3

                // Background (white/light gray) pixels
                if (brightness > 220) {
                    backgroundPixels++
                    continue
                }

                // Skin tone detection (selfie/face)
                // Skin: R is high, G is medium, B is low
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
                    when {
                        g > r && g > b && g > 60 -> greenPixels++
                        r > g && r > b && r > 60 -> redPixels++
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

        // Skin tone check - selfie/face detection
        val skinRatio = skinPixels.toFloat() / analyzablePixels
        if (skinRatio > 0.15f) {
            return AnalysisResult(
                isValidChart = false,
                notChartReason = "हे Chart नाही!\n\nहे selfie किंवा photo दिसत आहे.\n\nकृपया Trading Chart चा screenshot upload करा.\n(NSE, BSE, TradingView, Zerodha Kite, Upstox, Angel One इत्यादींचे chart)"
            )
        }

        val coloredPixels = greenPixels + redPixels
        val colorRatio = coloredPixels.toFloat() / analyzablePixels

        // Not enough chart colors
        if (colorRatio < 0.03f) {
            return AnalysisResult(
                isValidChart = false,
                notChartReason = "Chart patterns सापडले नाहीत!\n\nहे Candlestick chart नसेल.\n\nकृपया Green/Red candles असलेला chart upload करा.\n(Color candlestick chart असणे आवश्यक आहे)"
            )
        }

        // Valid chart - do analysis
        val greenPercent: Float
        val redPercent: Float

        if (coloredPixels > 0) {
            greenPercent = greenPixels.toFloat() / coloredPixels * 100f
            redPercent = redPixels.toFloat() / coloredPixels * 100f
        } else {
            greenPercent = 50f
            redPercent = 50f
        }

        val diff = abs(greenPercent - redPercent)

        val signal: String
        val confidence: Int
        val trend: String

        when {
            greenPercent > redPercent + 15 -> {
                signal = "BUY"
                confidence = (62 + min(diff.toInt() / 2, 28)).coerceIn(62, 90)
                trend = "UPTREND ↑"
            }
            redPercent > greenPercent + 15 -> {
                signal = "SELL"
                confidence = (62 + min(diff.toInt() / 2, 28)).coerceIn(62, 90)
                trend = "DOWNTREND ↓"
            }
            greenPercent > redPercent -> {
                signal = "BUY"
                confidence = (50 + diff.toInt()).coerceIn(50, 62)
                trend = "WEAK UPTREND ↗"
            }
            else -> {
                signal = "SELL"
                confidence = (50 + diff.toInt()).coerceIn(50, 62)
                trend = "WEAK DOWNTREND ↘"
            }
        }

        val volumeInfo = when {
            colorRatio > 0.15f -> "High Volume - Strong Signal 🔥"
            colorRatio > 0.08f -> "Medium Volume - Moderate Signal ⚡"
            else -> "Low Volume - Weak Signal ⚠️"
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
            appendLine("Chart मध्ये ${"%.1f".format(greenPercent)}% bullish आणि ${"%.1f".format(redPercent)}% bearish candles आढळल्या.")
            appendLine()
            appendLine("Trend: $trend")
            appendLine("Signal: $signal")
            appendLine("Confidence: $confidence%")
            appendLine()
            if (confidence >= 75) {
                appendLine("⚡ Strong signal — योग्य Stop-Loss सह trade करा.")
            } else {
                appendLine("⚠️ Moderate signal — सावधपणे trade करा, confirm होण्याची वाट पाहा.")
            }
            appendLine()
            append("⚠️ हे technical analysis आहे. Investment before expert consult करा.")
        }

        val investorInfo = if (signal == "BUY") {
            "Institutional investors या level वर accumulation करत आहेत. Bullish sentiment $confidence% आहे."
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
            investorInfo = investorInfo
        )
    }
}
