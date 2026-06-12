package com.tradinganalyzer.analysis

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class CandleInfo(
    val index: Int,
    val isGreen: Boolean,
    val isDoji: Boolean,
    val highNorm: Float,
    val lowNorm: Float,
    val openNorm: Float,
    val closeNorm: Float,
    val pixelVolume: Int,
    val bodySize: Float,
    val closePosition: Float,
    val upperWickRatio: Float,
    val lowerWickRatio: Float,
    val bodyRatio: Float,
    val candleStrengthScore: Int
)

data class CandleEngineResult(
    val signal: String,
    val confidence: Int,
    val trend: String,
    val trendStrength: Int,
    val nearSupport: Boolean,
    val nearResistance: Boolean,
    val isBreakout: Boolean,
    val isFakeout: Boolean,
    val volumeConfirmed: Boolean,
    val rsiEstimate: Int,
    val ema9Above: Boolean,
    val ema20Above: Boolean,
    val ema50Above: Boolean,
    val ema200Above: Boolean,
    val ema200Valid: Boolean,
    val goldenCross: Boolean,
    val deathCross: Boolean,
    val closePositionStr: String,
    val currentCandleScore: Int,
    val wickAnalysis: String,
    val patternContext: String,
    val currentCandleStr: String,
    val detailsStr: String,
    val candleCount: Int
)

object CandleEngine {

    fun analyze(
        bitmap: Bitmap,
        scanX1: Int, scanY1: Int, scanX2: Int, scanY2: Int
    ): CandleEngineResult {

        val candles = detectCandles(bitmap, scanX1, scanY1, scanX2, scanY2)

        if (candles.size < 3) {
            return CandleEngineResult(
                signal = "NEUTRAL", confidence = 30,
                trend = "Sideways", trendStrength = 0,
                nearSupport = false, nearResistance = false,
                isBreakout = false, isFakeout = false,
                volumeConfirmed = false, rsiEstimate = 50,
                ema9Above = false, ema20Above = false,
                ema50Above = false, ema200Above = false, ema200Valid = false,
                goldenCross = false, deathCross = false,
                closePositionStr = "N/A", currentCandleScore = 50,
                wickAnalysis = "N/A",
                patternContext = "Sideways",
                currentCandleStr = "पुरेसे candles detect झाले नाहीत (${candles.size})",
                detailsStr = "कमी candles — cleaner candlestick chart upload करा.",
                candleCount = candles.size
            )
        }

        val last     = candles.last()
        val recent20 = candles.takeLast(20)
        val recent50 = candles.takeLast(50)
        val closes   = candles.map { 1f - it.closeNorm }
        val lastClose = closes.last()

        // EMA 9 / 20 / 50 / 200
        val ema9List   = calcEma(closes, 9)
        val ema20List  = calcEma(closes, 20)
        val ema50List  = calcEma(closes, 50)
        val ema200List = calcEma(closes, 200)

        val ema9        = ema9List.lastOrNull()   ?: lastClose
        val ema20       = ema20List.lastOrNull()  ?: lastClose
        val ema50       = ema50List.lastOrNull()  ?: ema20
        val ema200Valid = ema200List.isNotEmpty()
        val ema200      = ema200List.lastOrNull() ?: ema50

        val ema9Above   = lastClose > ema9
        val ema20Above  = lastClose > ema20
        val ema50Above  = lastClose > ema50
        val ema200Above = lastClose > ema200

        // Golden Cross / Death Cross
        val goldenCross: Boolean
        val deathCross: Boolean
        if (ema9List.size >= 2 && ema20List.size >= 2) {
            val prevE9  = ema9List[ema9List.size - 2]
            val prevE20 = ema20List[ema20List.size - 2]
            goldenCross = prevE9 <= prevE20 && ema9 > ema20
            deathCross  = prevE9 >= prevE20 && ema9 < ema20
        } else {
            goldenCross = false; deathCross = false
        }

        // Trend (50-candle Higher Highs/Lows + EMA)
        val bullRatio50 = recent50.count { it.isGreen }.toFloat() / recent50.size.coerceAtLeast(1)
        val half50  = recent50.size / 2
        val first50 = recent50.take(half50)
        val sec50   = recent50.takeLast(half50)
        val fhHigh  = first50.map { 1f - it.highNorm }.average().toFloat()
        val shHigh  = sec50.map   { 1f - it.highNorm }.average().toFloat()
        val fhLow   = first50.map { 1f - it.lowNorm  }.average().toFloat()
        val shLow   = sec50.map   { 1f - it.lowNorm  }.average().toFloat()
        val higherHighs = shHigh > fhHigh + 0.01f
        val higherLows  = shLow  > fhLow  + 0.005f
        val lowerLows   = shLow  < fhLow  - 0.005f
        val lowerHighs  = shHigh < fhHigh - 0.01f

        val trend = when {
            (higherHighs && higherLows) ||
            (bullRatio50 > 0.60f && ema9Above && ema20Above && ema50Above) -> "Bullish"
            (lowerLows && lowerHighs) ||
            (bullRatio50 < 0.40f && !ema9Above && !ema20Above && !ema50Above) -> "Bearish"
            else -> "Sideways"
        }
        val trendStrength = when (trend) {
            "Bullish" -> ((bullRatio50 - 0.5f) * 200f).toInt().coerceIn(10, 100)
            "Bearish" -> ((0.5f - bullRatio50) * 200f).toInt().coerceIn(10, 100)
            else      -> (abs(bullRatio50 - 0.5f) * 100f).toInt().coerceIn(0, 40)
        }

        // Support / Resistance
        val supportLevel   = recent20.map { it.lowNorm  }.sortedDescending().take(5).average().toFloat()
        val resistLevel    = recent20.map { it.highNorm }.sorted().take(5).average().toFloat()
        val nearSupport    = abs(last.lowNorm  - supportLevel) < 0.045f
        val nearResistance = abs(last.highNorm - resistLevel)  < 0.045f

        // Volume
        val avgVol          = recent20.map { it.pixelVolume }.average()
        val volumeConfirmed = last.pixelVolume > avgVol * 1.15

        // Breakout / Fakeout
        val prev5     = recent20.dropLast(1).takeLast(5)
        val prevHigh5 = prev5.minOfOrNull { it.highNorm } ?: last.highNorm
        val prevLow5  = prev5.maxOfOrNull { it.lowNorm  } ?: last.lowNorm
        val brokeUp   = last.isGreen  && last.highNorm < prevHigh5 - 0.01f
        val brokeDown = !last.isGreen && last.lowNorm  > prevLow5  + 0.01f
        val isBreakout = (brokeUp || brokeDown) && volumeConfirmed
        val isFakeout  = (brokeUp || brokeDown) && !volumeConfirmed

        // RSI (14-period)
        val rsiEstimate = calcRSI(closes.takeLast(14))

        // Candle Position
        val cp = last.closePosition
        val closePositionStr = when {
            cp >= 0.80f -> "⬆️ Strong Close ${(cp * 100).toInt()}% — near HIGH (Bullish)"
            cp >= 0.60f -> "↗️ Good Close ${(cp * 100).toInt()}% — upper half (Mildly Bullish)"
            cp >= 0.40f -> "↔️ Neutral Close ${(cp * 100).toInt()}% — middle of range"
            cp >= 0.20f -> "↘️ Weak Close ${(cp * 100).toInt()}% — lower half (Mildly Bearish)"
            else        -> "⬇️ Weak Close ${(cp * 100).toInt()}% — near LOW (Bearish)"
        }

        // Wick Analysis
        val wickAnalysis = when {
            last.upperWickRatio > 0.50f && last.lowerWickRatio < 0.20f ->
                "🕯️ Heavy Upper Wick (${(last.upperWickRatio * 100).toInt()}%) — Selling pressure at top"
            last.lowerWickRatio > 0.50f && last.upperWickRatio < 0.20f ->
                "🕯️ Heavy Lower Wick (${(last.lowerWickRatio * 100).toInt()}%) — Buying pressure at bottom"
            last.upperWickRatio > 0.35f && last.lowerWickRatio > 0.35f ->
                "🕯️ Long Wicks Both Sides — Indecision / High volatility"
            last.bodyRatio > 0.70f ->
                "🕯️ Big Body (${(last.bodyRatio * 100).toInt()}%) — Strong directional move"
            last.isDoji ->
                "🕯️ Doji — Open ≈ Close, Market uncertain"
            else ->
                "🕯️ Normal candle — Body ${(last.bodyRatio * 100).toInt()}%"
        }

        // Pattern Context
        val patternContext = when {
            goldenCross                          -> "🌟 Golden Cross (EMA9 > EMA20) — Strong BUY"
            deathCross                           -> "💀 Death Cross (EMA9 < EMA20) — Strong SELL"
            trend == "Bullish" && nearSupport    -> "Uptrend + Support Bounce"
            trend == "Bullish" && isBreakout     -> "Uptrend + Breakout ✅"
            trend == "Bullish" && nearResistance -> "Uptrend → Resistance (Caution)"
            trend == "Bearish" && nearResistance -> "Downtrend + Resistance Reject"
            trend == "Bearish" && isBreakout     -> "Downtrend + Breakdown ✅"
            trend == "Bearish" && nearSupport    -> "Downtrend → Support (Caution)"
            trend == "Sideways" && isBreakout    -> "Range Breakout ✅"
            isFakeout                            -> "Fakeout ⚠️ — Volume confirm नाही"
            trend == "Bullish"                   -> "Uptrend (${(bullRatio50 * 100).toInt()}% bullish)"
            trend == "Bearish"                   -> "Downtrend (${((1f - bullRatio50) * 100).toInt()}% bearish)"
            else                                 -> "Sideways / Consolidation"
        }

        // Current Candle
        val lastType = when {
            last.isDoji                              -> "Doji (Uncertain)"
            last.isGreen && last.bodyRatio > 0.70f  -> "Strong Bullish (Big body)"
            last.isGreen && cp >= 0.70f              -> "Bullish (Strong close near high)"
            last.isGreen                             -> "Bullish"
            !last.isGreen && last.bodyRatio > 0.70f -> "Strong Bearish (Big body)"
            !last.isGreen && cp <= 0.30f             -> "Bearish (Weak close near low)"
            else                                     -> "Bearish"
        }
        val currentCandleStr = "$lastType | Vol: ${if (volumeConfirmed) "✅ High" else "⚠️ Weak"}"

        // Signal Scoring
        var buy = 0; var sell = 0
        when (trend) { "Bullish" -> buy += 30; "Bearish" -> sell += 30 }
        if (ema9Above)   buy  += 8  else sell += 8
        if (ema20Above)  buy  += 7  else sell += 7
        if (ema50Above)  buy  += 7  else sell += 7
        if (ema200Above) buy  += 8  else sell += 8
        if (goldenCross) buy  += 20
        if (deathCross)  sell += 20
        if (nearSupport)    buy  += 15
        if (nearResistance) sell += 15
        if (brokeUp   && volumeConfirmed) buy  += 20
        if (brokeDown && volumeConfirmed) sell += 20
        if (isFakeout) { buy -= 8; sell -= 8 }
        if (volumeConfirmed && last.isGreen)  buy  += 8
        if (volumeConfirmed && !last.isGreen) sell += 8
        when {
            rsiEstimate < 30 -> buy  += 12
            rsiEstimate > 70 -> sell += 12
            rsiEstimate < 45 -> buy  += 5
            rsiEstimate > 55 -> sell += 5
        }
        val cpScore = ((cp - 0.5f) * 2f * 15f).toInt()
        if (cpScore > 0) buy += cpScore else sell += -cpScore
        if (last.upperWickRatio > 0.50f) sell += 8
        if (last.lowerWickRatio > 0.50f) buy  += 8
        if (last.bodyRatio > 0.70f) { if (last.isGreen) buy += 5 else sell += 5 }
        if (!last.isDoji) { if (last.isGreen) buy += 3 else sell += 3 }
        buy  = buy.coerceAtLeast(0)
        sell = sell.coerceAtLeast(0)

        val total      = (buy + sell).coerceAtLeast(1)
        val signal     = if (buy >= sell) "BUY" else "SELL"
        val winScore   = if (signal == "BUY") buy else sell
        val confidence = (winScore.toFloat() / total * 100f).toInt().coerceIn(30, 95)

        val detailsStr = buildString {
            appendLine("🕯️ Candles Detected  : ${candles.size}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📈 Trend (50 candles) : $trend ($trendStrength%)")
            appendLine("   🟢 Bullish : ${(bullRatio50 * 100).toInt()}%  🔴 Bearish : ${((1f - bullRatio50) * 100).toInt()}%")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📊 EMA9   : ${if (ema9Above)   "Above ✅" else "Below ⚠️"}")
            appendLine("📊 EMA20  : ${if (ema20Above)  "Above ✅" else "Below ⚠️"}")
            appendLine("📊 EMA50  : ${if (ema50Above)  "Above ✅" else "Below ⚠️"}")
            if (ema200Valid)
                appendLine("📊 EMA200 : ${if (ema200Above) "Above ✅ (Long-term Bull)" else "Below ⚠️ (Long-term Bear)"}")
            else
                appendLine("📊 EMA200 : ⚠️ ${candles.size} candles (200+ needed)")
            if (goldenCross) appendLine("🌟 GOLDEN CROSS — EMA9 > EMA20 → Strong BUY")
            if (deathCross)  appendLine("💀 DEATH CROSS  — EMA9 < EMA20 → Strong SELL")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🕯️ Current Candle:")
            appendLine("   Type     : $lastType")
            appendLine("   $closePositionStr")
            appendLine("   $wickAnalysis")
            appendLine("   Strength : ${last.candleStrengthScore} / 100")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🛡️  Support    : ${if (nearSupport)    "YES — Buy zone ✅" else "No"}")
            appendLine("🚧 Resistance  : ${if (nearResistance) "YES — Sell zone ⚠️" else "No"}")
            appendLine("💥 Breakout    : ${if (isBreakout) "YES ✅" else "No"}")
            appendLine("⚠️  Fakeout    : ${if (isFakeout)  "YES — Volume नाही ⚠️" else "No"}")
            appendLine("📦 Volume      : ${if (volumeConfirmed) "✅ High (${(last.pixelVolume.toFloat() / avgVol * 100).toInt()}% of avg)" else "⚠️ Low (${(last.pixelVolume.toFloat() / avgVol * 100).toInt()}% of avg)"}")
            appendLine("📊 RSI         : $rsiEstimate ${when { rsiEstimate > 70 -> "(Overbought 🔴)"; rsiEstimate < 30 -> "(Oversold 🟢)"; else -> "(Normal ✅)" }}")
            append("🎯 Context     : $patternContext")
        }

        return CandleEngineResult(
            signal = signal, confidence = confidence,
            trend = trend, trendStrength = trendStrength,
            nearSupport = nearSupport, nearResistance = nearResistance,
            isBreakout = isBreakout, isFakeout = isFakeout,
            volumeConfirmed = volumeConfirmed, rsiEstimate = rsiEstimate,
            ema9Above = ema9Above, ema20Above = ema20Above,
            ema50Above = ema50Above,
            ema200Above = ema200Above, ema200Valid = ema200Valid,
            goldenCross = goldenCross, deathCross = deathCross,
            closePositionStr = closePositionStr,
            currentCandleScore = last.candleStrengthScore,
            wickAnalysis = wickAnalysis,
            patternContext = patternContext,
            currentCandleStr = currentCandleStr,
            detailsStr = detailsStr,
            candleCount = candles.size
        )
    }

    private fun detectCandles(
        bitmap: Bitmap,
        scanX1: Int, scanY1: Int, scanX2: Int, scanY2: Int
    ): List<CandleInfo> {
        val zoneW = (scanX2 - scanX1).coerceAtLeast(1)
        val zoneH = (scanY2 - scanY1).coerceAtLeast(1)
        val colGreen = IntArray(zoneW)
        val colRed   = IntArray(zoneW)
        val colTopY  = IntArray(zoneW) { zoneH }
        val colBotY  = IntArray(zoneW) { 0 }

        for (x in 0 until zoneW) {
            val absX = scanX1 + x
            if (absX >= bitmap.width) continue
            for (y in 0 until zoneH) {
                val absY = scanY1 + y
                if (absY >= bitmap.height) continue
                val px = bitmap.getPixel(absX, absY)
                if (Color.alpha(px) < 50) continue
                val r = Color.red(px); val g = Color.green(px); val b = Color.blue(px)
                if ((r + g + b) / 3 > 212) continue
                val maxC = max(r, max(g, b)); val minC = min(r, min(g, b))
                val sat  = if (maxC > 0) (maxC - minC).toFloat() / maxC else 0f
                if (sat < 0.28f) continue
                val isTeal  = g > 55 && b > 55 && g > r * 1.4f && b > r * 1.4f
                val isPureG = g > 100 && g > r * 2.8f && g > b * 2.8f
                val isGreen = isTeal || isPureG
                val isRed   = r > 55 && r > g * 1.5f && r > b * 1.5f
                val isBlue  = b > r * 1.3f && b > g * 1.3f
                if (isBlue && !isGreen) continue
                if (!isGreen && !isRed) continue
                if (isGreen) {
                    colGreen[x]++
                    if (y < colTopY[x]) colTopY[x] = y
                    if (y > colBotY[x]) colBotY[x] = y
                } else {
                    colRed[x]++
                    if (y < colTopY[x]) colTopY[x] = y
                    if (y > colBotY[x]) colBotY[x] = y
                }
            }
        }

        val candles = mutableListOf<CandleInfo>()
        var x = 0; var idx = 0
        while (x < zoneW) {
            val gC = colGreen[x]; val rC = colRed[x]
            if (gC + rC < 3) { x++; continue }
            val isGreen = gC >= rC
            var endX = x + 1
            while (endX < zoneW && endX - x < 35) {
                val eg = colGreen[endX]; val er = colRed[endX]
                if (eg + er < 2) { endX++; break }
                if ((eg >= er) != isGreen && abs(eg - er) > 5) break
                endX++
            }
            var totPix = 0; var topY = zoneH; var botY = 0
            for (cx in x until endX) {
                totPix += colGreen[cx] + colRed[cx]
                if (colTopY[cx] < topY) topY = colTopY[cx]
                if (colBotY[cx] > botY) botY = colBotY[cx]
            }
            if (totPix < 5) { x = endX; continue }

            val highNorm = topY.toFloat() / zoneH
            val lowNorm  = botY.toFloat() / zoneH

            val density = IntArray(zoneH)
            for (cx in x until endX) {
                val absX2 = scanX1 + cx
                if (absX2 >= bitmap.width) continue
                for (y in topY..botY) {
                    val absY2 = scanY1 + y
                    if (absY2 >= bitmap.height) continue
                    val px2 = bitmap.getPixel(absX2, absY2)
                    val r2 = Color.red(px2); val g2 = Color.green(px2); val b2 = Color.blue(px2)
                    val isTeal2  = g2 > 55 && b2 > 55 && g2 > r2 * 1.4f && b2 > r2 * 1.4f
                    val isPureG2 = g2 > 100 && g2 > r2 * 2.8f && g2 > b2 * 2.8f
                    val isG2 = isTeal2 || isPureG2
                    val isR2 = r2 > 55 && r2 > g2 * 1.5f && r2 > b2 * 1.5f
                    if (isG2 || isR2) density[y]++
                }
            }
            val maxD   = density.maxOrNull() ?: 1
            val thresh = maxD * 0.55
            var bodyTop = topY; var bodyBot = botY
            for (y in topY..botY)       { if (density[y] >= thresh) { bodyTop = y; break } }
            for (y in botY downTo topY) { if (density[y] >= thresh) { bodyBot = y; break } }

            val bTopN   = bodyTop.toFloat() / zoneH
            val bBotN   = bodyBot.toFloat() / zoneH
            val bodyNorm = (bBotN - bTopN).coerceAtLeast(0f)
            val openNorm  = if (isGreen) bBotN else bTopN
            val closeNorm = if (isGreen) bTopN else bBotN

            val priceHigh  = 1f - highNorm; val priceLow   = 1f - lowNorm
            val priceOpen  = 1f - openNorm; val priceClose = 1f - closeNorm
            val totalRange = (priceHigh - priceLow).coerceAtLeast(0.001f)
            val closePosition = ((priceClose - priceLow) / totalRange).coerceIn(0f, 1f)
            val bodyHi = max(priceOpen, priceClose); val bodyLo = min(priceOpen, priceClose)
            val upperWick = (priceHigh - bodyHi).coerceAtLeast(0f)
            val lowerWick = (bodyLo    - priceLow).coerceAtLeast(0f)
            val bodyPrSz  = (bodyHi    - bodyLo).coerceAtLeast(0f)
            val upperWickRatio = (upperWick / totalRange).coerceIn(0f, 1f)
            val lowerWickRatio = (lowerWick / totalRange).coerceIn(0f, 1f)
            val bodyRatio      = (bodyPrSz  / totalRange).coerceIn(0f, 1f)
            val candleStrengthScore = if (isGreen)
                ((closePosition * 50f) + (bodyRatio * 30f) - (upperWickRatio * 20f)).toInt().coerceIn(0, 100)
            else
                (((1f - closePosition) * 50f) + (bodyRatio * 30f) - (lowerWickRatio * 20f)).toInt().coerceIn(0, 100)
            candles += CandleInfo(
                index = idx++, isGreen = isGreen, isDoji = bodyNorm < 0.015f,
                highNorm = highNorm, lowNorm = lowNorm,
                openNorm = openNorm, closeNorm = closeNorm,
                pixelVolume = totPix, bodySize = bodyNorm,
                closePosition = closePosition,
                upperWickRatio = upperWickRatio, lowerWickRatio = lowerWickRatio,
                bodyRatio = bodyRatio, candleStrengthScore = candleStrengthScore
            )
            x = endX
        }
        return candles
    }
    private fun calcEma(prices: List<Float>, period: Int): List<Float> {
        if (prices.size < period) return emptyList()
        val k = 2f / (period + 1)
        var v = prices.take(period).average().toFloat()
        val r = mutableListOf(v)
        for (i in period until prices.size) { v = prices[i] * k + v * (1f - k); r.add(v) }
        return r
    }
    private fun calcRSI(closes: List<Float>): Int {
        if (closes.size < 2) return 50
        var gains = 0f; var losses = 0f
        for (i in 1 until closes.size) {
            val d = closes[i] - closes[i - 1]
            if (d > 0) gains += d else losses += -d
        }
        if (losses == 0f) return 100
        if (gains  == 0f) return 0
        return (100f - 100f / (1f + gains / losses)).toInt().coerceIn(0, 100)
    }
                       }
