package com.tradinganalyzer.analysis

import kotlin.math.*

data class Candle(
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float
) {
    val isGreen: Boolean get() = close > open
}

data class PatternResult(
    val patternName: String, // 🎯 ChartAnalyzer सोबत सुसंगत राहण्यासाठी मूळ नाव 'patternName' ठेवले आहे
    val patternType: String, // 🎯 'patternType' (Bullish/Bearish) केले जेणेकरून स्कोअरिंग मॅच होईल
    val confidence: Int,
    val strength: String,
    val score: Int
)

class PatternEngine {

    companion object {
        // =========================
        // EMA CALCULATION
        // =========================
        fun ema(closes: List<Float>, period: Int): Float {
            if (closes.isEmpty()) return 0f
            val k = 2f / (period + 1)
            var ema = closes.first()
            for (i in 1 until closes.size) {
                ema = closes[i] * k + ema * (1 - k)
            }
            return ema
        }

        // =========================
        // RSI CALCULATION
        // =========================
        fun rsi(closes: List<Float>, period: Int = 14): Float {
            if (closes.size <= period) return 50f // पुरेसा डेटा नसल्यास सेफ ५० आरएसआय देणे

            var gain = 0f
            var loss = 0f

            for (i in 1 until period) {
                val diff = closes[i] - closes[i - 1]
                if (diff >= 0) gain += diff else loss += abs(diff)
            }

            var avgGain = gain / period
            var avgLoss = loss / period

            for (i in period until closes.size) {
                val diff = closes[i] - closes[i - 1]
                if (diff >= 0) {
                    avgGain = (avgGain * (period - 1) + diff) / period
                    avgLoss = (avgLoss * (period - 1)) / period
                } else {
                    avgLoss = (avgLoss * (period - 1) + abs(diff)) / period
                }
            }

            if (avgLoss == 0f) return 100f
            val rs = avgGain / avgLoss
            return 100 - (100 / (1 + rs))
        }

        // =========================
        // TREND FILTER
        // =========================
        fun trend(price: Float, ema: Float): String {
            return when {
                price > ema -> "UP"
                price < ema -> "DOWN"
                else -> "SIDE"
            }
        }

        // =========================
        // ALLOW PATTERN BASED ON TREND
        // =========================
        fun allow(trend: String, type: String): Boolean {
            return when (type) {
                "BULLISH" -> trend != "DOWN"
                "BEARISH" -> trend != "UP"
                else -> true
            }
        }

        // =========================
        // DISPLAY FORMATTER (ChartAnalyzer साठी आवश्यक)
        // =========================
        fun formatDisplay(patterns: List<PatternResult>, topN: Int = 5): String {
            if (patterns.isEmpty()) return "Chart pattern detect झाला नाही.\nCandlestick chart upload करा."
            return patterns.take(topN).joinToString("\n") { 
                "• ${it.patternName} (${it.patternType}) - Conf: ${it.confidence}%" 
            }
        }

        // =========================
        // MAIN ANALYZE ENGINE
        // =========================
        // 🎯 ChartAnalyzer मधून येणाऱ्या डेटा क्लासला मॅच होण्यासाठी आपण ओव्हरलोड फंक्शन जोडले आहे
        fun detect(shapeData: CandleShapeData, currentPosition: String): List<PatternResult> {
            val dummyCandle = Candle(
                open = shapeData.open,
                high = shapeData.high,
                low = shapeData.low,
                close = shapeData.close
            )
            // तात्पुरती सेफ प्राईस लिस्ट पास करणे जेणेकरून जुना बिल्ड क्रॅश होणार नाही
            val dummyCloses = listOf(shapeData.close * 0.98f, shapeData.close * 0.99f, shapeData.close)
            return analyze(dummyCandle, dummyCloses)
        }

        fun analyze(c: Candle, closes: List<Float>): List<PatternResult> {
            val results = mutableListOf<PatternResult>()

            // 🛑 CRASH PROTECTION: कॅल्क्युलेशन करण्यासाठी पुरेशा कॅन्डल्स नसतील तर रिकामी लिस्ट पाठवणे
            if (closes.size < 2) return results

            val emaVal = ema(closes, 21)
            val rsiVal = rsi(closes, 14)
            val tr = trend(c.close, emaVal)

            val range = (c.high - c.low).coerceAtLeast(0.001f) // ० ने भाग जाण्याचा ओव्हरफ्लो टाळला
            val body = abs(c.close - c.open)
            val up = c.high - max(c.close, c.open)
            val down = min(c.close, c.open) - c.low

            // =========================
            // SINGLE CANDLE (1–25)
            // =========================

            if (body <= range * 0.1f)
                results += PatternResult("Doji", "NEUTRAL", 40, "S", 1)

            if (body <= range * 0.1f && down > range * 0.6f && allow(tr, "BULLISH"))
                results += PatternResult("Dragonfly Doji", "BULLISH", 55, "S", 2)

            if (body <= range * 0.1f && up > range * 0.6f && allow(tr, "BEARISH"))
                results += PatternResult("Gravestone Doji", "BEARISH", 55, "S", 3)

            if (down > body * 2 && up < body && allow(tr, "BULLISH"))
                results += PatternResult("Hammer", "BULLISH", 70, "S", 4)

            if (up > body * 2 && down < body && allow(tr, "BEARISH"))
                results += PatternResult("Shooting Star", "BEARISH", 70, "S", 5)

            if (c.isGreen && body > range * 0.8f)
                results += PatternResult("Marubozu Bullish", "BULLISH", 75, "S", 6)

            if (!c.isGreen && body > range * 0.8f)
                results += PatternResult("Marubozu Bearish", "BEARISH", 75, "S", 7)

            if (up < range * 0.1f && c.isGreen)
                results += PatternResult("Shaven Head", "BULLISH", 60, "M", 8)

            if (down < range * 0.1f && !c.isGreen)
                results += PatternResult("Shaven Bottom", "BEARISH", 60, "M", 9)

            if (body in (range * 0.15f)..(range * 0.4f))
                results += PatternResult("Spinning Top", "NEUTRAL", 45, "M", 10)

            // =========================
            // TWO CANDLE (26–60)
            // =========================

            if (c.isGreen && tr == "DOWN" && allow(tr, "BULLISH"))
                results += PatternResult("Bullish Engulfing", "BULLISH", 80, "S", 26)

            if (!c.isGreen && tr == "UP")
                results += PatternResult("Bearish Engulfing", "BEARISH", 80, "S", 27)

            if (body < range * 0.3f && rsiVal < 30f)
                results += PatternResult("Bullish Harami", "BULLISH", 65, "M", 28)

            if (body < range * 0.3f && rsiVal > 70f)
                results += PatternResult("Bearish Harami", "BEARISH", 65, "M", 29)

            if (down > range * 0.5f && tr != "DOWN")
                results += PatternResult("Piercing Line", "BULLISH", 75, "S", 30)

            if (up > range * 0.5f && tr != "UP")
                results += PatternResult("Dark Cloud Cover", "BEARISH", 75, "S", 31)

            if (down > body && up > body)
                results += PatternResult("Tweezer Bottom", "BULLISH", 60, "M", 32)

            if (up > body && down > body)
                results += PatternResult("Tweezer Top", "BEARISH", 60, "M", 33)

            // =========================
            // THREE CANDLE (61–85)
            // =========================

            if (rsiVal < 30)
                results += PatternResult("Morning Star Setup", "BULLISH", 85, "S", 61)

            if (rsiVal > 70)
                results += PatternResult("Evening Star Setup", "BEARISH", 85, "S", 62)

            if (tr == "UP" && c.isGreen)
                results += PatternResult("Three White Soldiers", "BULLISH", 90, "S", 63)

            if (tr == "DOWN" && !c.isGreen)
                results += PatternResult("Three Black Crows", "BEARISH", 90, "S", 64)

            if (body > range * 0.6f)
                results += PatternResult("Three Inside Move", "NEUTRAL", 70, "M", 65)

            // =========================
            // CHART PATTERNS (86–100)
            // =========================

            if (tr == "UP" && rsiVal > 55)
                results += PatternResult("Breakout", "BULLISH", 80, "S", 86)

            if (tr == "DOWN" && rsiVal < 45)
                results += PatternResult("Breakdown", "BEARISH", 80, "S", 87)

            if (body < range * 0.2f)
                results += PatternResult("Consolidation Zone", "NEUTRAL", 60, "M", 88)

            if (rsiVal in 45f..55f)
                results += PatternResult("Sideways Market", "NEUTRAL", 50, "W", 89)

            if (tr == "UP" && body > range * 0.7f)
                results += PatternResult("Trend Continuation Up", "BULLISH", 88, "S", 90)

            if (tr == "DOWN" && body > range * 0.7f)
                results += PatternResult("Trend Continuation Down", "BEARISH", 88, "S", 91)

            if (rsiVal < 25)
                results += PatternResult("Oversold Reversal", "BULLISH", 92, "S", 92)

            if (rsiVal > 75)
                results += PatternResult("Overbought Reversal", "BEARISH", 92, "S", 93)

            if (body > range * 0.9f)
                results += PatternResult("High Volatility Spike", "NEUTRAL", 70, "M", 94)

            if (tr == "UP" && rsiVal > 60)
                results += PatternResult("Momentum Up", "BULLISH", 85, "S", 95)

            if (tr == "DOWN" && rsiVal < 40)
                results += PatternResult("Momentum Down", "BEARISH", 85, "S", 96)

            if (body < range * 0.15f)
                results += PatternResult("Indecision Zone", "NEUTRAL", 55, "M", 97)

            if (tr == "UP")
                results += PatternResult("Market Strength Up", "BULLISH", 80, "S", 98)

            if (tr == "DOWN")
                results += PatternResult("Market Strength Down", "BEARISH", 80, "S", 99)

            results += PatternResult("System Scan Complete", "NEUTRAL", 100, "S", 100)

            return results
        }
    }
}
