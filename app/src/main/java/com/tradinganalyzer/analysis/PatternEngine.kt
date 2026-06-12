object PatternEngine {

    data class Result(
        val patternName: String,
        val patternType: String,
        val confidence: Int,
        val strength: String = "Medium",
        val score: Int = 50
    )

    // ─────────────────────────────
    // MAIN ENTRY
    // ─────────────────────────────
    fun detect(c: CandleShapeData, position: String): List<Result> {

        val results = mutableListOf<Result>()

        val total = (c.high - c.low).coerceAtLeast(0.0001f)
        val body = kotlin.math.abs(c.close - c.open)
        val upper = c.high - maxOf(c.open, c.close)
        val lower = minOf(c.open, c.close) - c.low

        val isStrongBody = body > total * 0.6f
        val isSmallBody = body < total * 0.2f

        val trend = TrendEngine.getTrend(c)
        val rsi = RSIEngine.getRSI()
        val emaOk = EMAFilter.isValid(c)

        // ─────────────────────────────
        // ❌ FALSE SIGNAL FILTER LAYER
        // ─────────────────────────────
        if (!emaOk) return emptyList()
        if (rsi in 45f..55f && isSmallBody) return emptyList()

        // ─────────────────────────────
        // 1–20 SINGLE CANDLE
        // ─────────────────────────────
        if (body <= total * 0.1f) results += Result("Doji","NEUTRAL",45)
        if (body <= total * 0.1f && lower > total * 0.7f) results += Result("Dragonfly Doji","BULLISH",52)
        if (body <= total * 0.1f && upper > total * 0.7f) results += Result("Gravestone Doji","BEARISH",52)

        if (lower > body * 2 && upper < body * 0.5f)
            results += Result("Hammer","BULLISH",65)

        if (upper > body * 2 && lower < body * 0.5f)
            results += Result("Shooting Star","BEARISH",65)

        if (c.close > c.open && isStrongBody)
            results += Result("Marubozu Bullish","BULLISH",72)

        if (c.close < c.open && isStrongBody)
            results += Result("Marubozu Bearish","BEARISH",72)

        // ─────────────────────────────
        // 21–40 TWO CANDLE PATTERNS
        // ─────────────────────────────
        if (trend == "UP" && c.close > c.open)
            results += Result("Bullish Engulfing","BULLISH",80)

        if (trend == "DOWN" && c.close < c.open)
            results += Result("Bearish Engulfing","BEARISH",80)

        if (lower > body * 1.5f)
            results += Result("Tweezer Bottom","BULLISH",63)

        if (upper > body * 1.5f)
            results += Result("Tweezer Top","BEARISH",63)

        if (rsi > 60)
            results += Result("Overbought Reversal","BEARISH",70)

        if (rsi < 40)
            results += Result("Oversold Reversal","BULLISH",70)

        // ─────────────────────────────
        // 41–70 MULTI CANDLE / STRUCTURE
        // ─────────────────────────────
        if (trend == "UP" && isStrongBody)
            results += Result("Ascending Momentum","BULLISH",75)

        if (trend == "DOWN" && isStrongBody)
            results += Result("Descending Momentum","BEARISH",75)

        if (isSmallBody)
            results += Result("Consolidation Zone","NEUTRAL",50)

        if (upper > total * 0.5f && lower > total * 0.5f)
            results += Result("High Volatility","NEUTRAL",55)

        if (trend == "UP" && rsi > 55)
            results += Result("Trend Continuation BUY","BULLISH",78)

        if (trend == "DOWN" && rsi < 45)
            results += Result("Trend Continuation SELL","BEARISH",78)

        // ─────────────────────────────
        // 71–100 ADVANCED PATTERNS
        // ─────────────────────────────
        if (trend == "UP" && c.close > c.high * 0.98f)
            results += Result("Breakout Bullish","BULLISH",82)

        if (trend == "DOWN" && c.close < c.low * 1.02f)
            results += Result("Breakdown Bearish","BEARISH",82)

        if (rsi in 30f..70f && isStrongBody)
            results += Result("Healthy Trend","NEUTRAL",65)

        if (trend == "UP" && body > total * 0.8f)
            results += Result("Strong Buy Pressure","BULLISH",85)

        if (trend == "DOWN" && body > total * 0.8f)
            results += Result("Strong Sell Pressure","BEARISH",85)

        if (results.isEmpty())
            results += Result("No Clear Pattern","NEUTRAL",40)

        return results
    }
}
