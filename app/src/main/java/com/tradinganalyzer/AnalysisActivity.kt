    private fun displayResult(result: AnalysisResult) {
        binding.cardResult.visibility = View.VISIBLE
        binding.cardFeedback.visibility = View.VISIBLE

        binding.tvSignal.text = result.signal
        if (result.signal == "BUY") {
            binding.tvSignal.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            binding.tvSignal.setTextColor(android.graphics.Color.parseColor("#F44336"))
        }

        binding.tvConfidence.text = "Confidence: ${result.confidence}%"
        binding.progressConfidence.progress = result.confidence
        binding.tvCandleDirection.text = "📊 Candle Direction: ${result.trend}"
        binding.tvGreenPercent.text = "🟢 Bullish Candles: ${String.format("%.1f", result.greenPercent)}%"
        binding.tvRedPercent.text = "🔴 Bearish Candles: ${String.format("%.1f", result.redPercent)}%"
        binding.tvVolumeInfo.text = "📈 Volume Analysis: ${result.volumeInfo}"
        binding.tvSentiment.text = "💹 Market Sentiment: ${result.sentiment}"
        binding.tvSupportResistance.text = "🎯 Key Levels:\n${result.supportResistance}"
        binding.tvInvestorInfo.text = "👥 ${result.investorInfo}"
        binding.tvSummary.text = result.summary
    }
