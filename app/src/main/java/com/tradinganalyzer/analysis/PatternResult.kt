package com.tradinganalyzer.analysis

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PatternResult — PatternEngine detect करतो, ChartAnalyzer वापरतो
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
data class PatternResult(
    val name: String,           // "Cup and Handle", "Hammer", etc.
    val patternType: String,    // "Bullish" / "Bearish"
    val confidence: Int,        // 0–100 (ChartAnalyzer adjust करतो)
    val strength: String,       // "Strong" / "Moderate" / "Weak"
    val score: Int              // raw pattern score (0–100)
)
