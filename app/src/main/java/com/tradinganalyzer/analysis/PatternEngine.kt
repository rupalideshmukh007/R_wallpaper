package com.tradinganalyzer.analysis

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class OhlcCandle(
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float
) {
    val totalRange: Float get() = (high - low).coerceAtLeast(0.0001f)
    val bodySize: Float get() = abs(close - open)
    val upperWick: Float get() = high - max(close, open)
    val lowerWick: Float get() = min(close, open) - low
    val isGreen: Boolean get() = close > open
}

data class PatternResult(
    val patternName: String,
    val patternType: String,
    val confidence: Int,
    val strength: String,
    val score: Int
)

class PatternEngine {

    fun ema(closes: List<Float>, period: Int = 21): Float {
        if (closes.isEmpty()) return 0f
        val k = 2f / (period + 1)
        var ema = closes.first()
        for (i in 1 until closes.size) {
            ema = closes[i] * k + ema * (1 - k)
        }
        return ema
    }

    fun trend(price: Float, ema: Float): String {
        return when {
            price > ema -> "UP"
            price < ema -> "DOWN"
            else -> "SIDE"
        }
    }

    fun analyze(c: OhlcCandle, closes: List<Float>): List<PatternResult> {
        val results = mutableListOf<PatternResult>()

        val emaVal = ema(closes)
        val rsiVal = rsi(closes)
        val tr = trend(c.close, emaVal)

        val range = c.totalRange
        val body = c.bodySize
        val up = c.upperWick
        val down = c.lowerWick

        fun allow(type: String): Boolean {
            return when (type) {
                "BULLISH" -> tr != "DOWN"
                "BEARISH" -> tr != "UP"
                else -> true
            }
        }

        // ❌ SIDEWAYS FILTER LAYER
        if (rsiVal in 45f..55f && body < range * 0.2f) {
            results += PatternResult("Sideways Market", "NEUTRAL", 50, "W", 89)
            return results
        }

        // ───────── SINGLE CANDLE PATTERNS (1–25) ─────────
        if (body <= range * 0.05f) { // Doji Rule
            results += PatternResult("Doji", "NEUTRAL", 45, "W", 1)
            if (down > range * 0.6f && allow("BULLISH")) {
                results += PatternResult("Dragonfly Doji", "BULLISH", 60, "S", 2)
            }
            if (up > range * 0.6f && allow("BEARISH")) {
                results += PatternResult("Gravestone Doji", "BEARISH", 60, "S", 3)
            }
        }

        if (down >= body * 2f && up < range * 0.10f && allow("BULLISH")) { // Hammer Rule
            results += PatternResult("Hammer", "BULLISH", 80, "S", 4)
        }

        if (up >= body * 2f && down < range * 0.10f && allow("BEARISH")) { // Shooting Star Rule
            results += PatternResult("Shooting Star", "BEARISH", 80, "S", 5)
        }

        if (body >= range * 0.90f) { // Marubozu Rule
            if (c.isGreen) results += PatternResult("Bull Marubozu", "BULLISH", 85, "S", 6)
            else results += PatternResult("Bear Marubozu", "BEARISH", 85, "S", 7)
        }

        if (up < range * 0.1f && c.isGreen) results += PatternResult("Shaven Head", "BULLISH", 65, "M", 8)
        if (down < range * 0.1f && !c.isGreen) results += PatternResult("Shaven Bottom", "BEARISH", 65, "M", 9)
        if (body in (range * 0.15f)..(range * 0.4f)) results += PatternResult("Spinning Top", "NEUTRAL", 45, "M", 10)

        // ───────── TWO CANDLE LOGIC (26–60) ─────────
        if (c.isGreen && tr == "DOWN" && body > range * 0.6f) results += PatternResult("Bullish Engulfing", "BULLISH", 85, "S", 26)
        if (!c.isGreen && tr == "UP" && body > range * 0.6f) results += PatternResult("Bearish Engulfing", "BEARISH", 85, "S", 27)
        if (body < range * 0.3f && rsiVal < 30f) results += PatternResult("Bullish Harami", "BULLISH", 70, "M", 28)
        if (body < range * 0.3f && rsiVal > 70f) results += PatternResult("Bearish Harami", "BEARISH", 70, "M", 29)
        if (down > range * 0.5f && tr != "DOWN") results += PatternResult("Piercing Line", "BULLISH", 75, "S", 30)
        if (up > range * 0.5f && tr != "UP") results += PatternResult("Dark Cloud Cover", "BEARISH", 75, "S", 31)
        if (down > body && up > body && allow("BULLISH")) results += PatternResult("Tweezer Bottom", "BULLISH", 65, "M", 32)
        if (up > body && down > body && allow("BEARISH")) results += PatternResult("Tweezer Top", "BEARISH", 65, "M", 33)

        // ───────── THREE CANDLE / REVERSALS (61–85) ─────────
        if (rsiVal < 30) results += PatternResult("Oversold Bounce", "BULLISH", 90, "S", 61)
        if (rsiVal > 70) results += PatternResult("Overbought Drop Setup", "BEARISH", 90, "S", 62)
        if (tr == "UP" && c.isGreen) results += PatternResult("Trend Up Continuation", "BULLISH", 85, "S", 63)
        if (tr == "DOWN" && !c.isGreen) results += PatternResult("Trend Down Continuation", "BEARISH", 85, "S", 64)

        // ───────── BREAKOUT LAYER (86–100) ─────────
        if (body > range * 0.7f && rsiVal > 55) results += PatternResult("Breakout Candle", "BULLISH", 80, "M", 86)
        if (body > range * 0.7f && rsiVal < 45) Extracted results += PatternResult("Breakdown Candle", "BEARISH", 80, "M", 87)

        if (results.isEmpty()) {
            results += PatternResult("No Clear Pattern", "NEUTRAL", 40, "W", 100)
        }

        return results
    }

    companion object {
        fun rsi(closes: List<Float>, period: Int = 14): Float {
            if (closes.size < 2) return 50f
            var gain = 0f
            var loss = 0f
            for (i in 1 until closes.size) {
                val diff = closes[i] - closes[i - 1]
                if (diff >= 0) gain += diff else loss += abs(diff)
            }
            val rs = if (loss == 0f) 100f else gain / loss
            return 100 - (100 / (1 + rs))
        }

        fun formatDisplay(patterns: List<PatternResult>, topN: Int = 5): String {
            if (patterns.isEmpty() || (patterns.size == 1 && patterns.first().patternName == "No Clear Pattern")) {
                return "AI Vision: No clear mathematical pattern found."
            }
            return patterns.take(topN).joinToString("\n") { 
                "• ${it.patternName} (${it.patternType}) - Conf: ${it.confidence}%" 
            }
        }
    }
}
