package com.tradinganalyzer.analysis

data class AnalysisResult(
    val signal: String,           // "BUY" or "SELL"
    val confidence: Int,          // 0-100
    val greenPercent: Float,      // % of bullish area
    val redPercent: Float,        // % of bearish area
    val trend: String,            // "UPTREND" / "DOWNTREND" / "SIDEWAYS"
    val volumeInfo: String,
    val sentiment: String,
    val supportResistance: String,
    val summary: String,
    val investorInfo: String
)
