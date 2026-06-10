package com.tradinganalyzer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tradinganalyzer.databinding.ActivityTradingInfoBinding

class TradingInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTradingInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTradingInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Trading Information"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.tvTradingContent.text = buildTradingInfo()
    }

    private fun buildTradingInfo(): String {
        val sb = StringBuilder()
        sb.append("TRADING - संपूर्ण माहिती\n")
        sb.append("===================================\n\n")
        sb.append("1. CANDLESTICK म्हणजे काय?\n")
        sb.append("-----------------------------------\n")
        sb.append("Candlestick = chart वरील एक candle\n")
        sb.append("Open  - बाजार सुरू होण्याची किंमत\n")
        sb.append("Close - बाजार बंद होण्याची किंमत\n")
        sb.append("High  - सर्वात जास्त किंमत\n")
        sb.append("Low   - सर्वात कमी किंमत\n\n")
        sb.append("GREEN CANDLE: Close > Open = किंमत वाढली = BUY\n")
        sb.append("RED CANDLE: Close < Open = किंमत घसरली = SELL\n\n")
        sb.append("===================================\n\n")
        sb.append("2. RSI - Relative Strength Index\n")
        sb.append("-----------------------------------\n")
        sb.append("RSI 0 ते 100 च्या दरम्यान असतो.\n\n")
        sb.append("RSI 70+ = Overbought\n")
        sb.append("खूप खरेदी झाली. किंमत खाली येण्याची शक्यता.\n")
        sb.append("नवीन BUY टाळा.\n\n")
        sb.append("RSI 30- = Oversold\n")
        sb.append("खूप विक्री झाली. किंमत वर जाण्याची शक्यता.\n")
        sb.append("BUY opportunity.\n\n")
        sb.append("RSI 40-60 = Neutral. Wait करा.\n\n")
        sb.append("===================================\n\n")
        sb.append("3. CANDLESTICK PATTERNS\n")
        sb.append("-----------------------------------\n")
        sb.append("DOJI: Open = Close. Market uncertain.\n\n")
        sb.append("HAMMER: लांब lower wick, वर छोटी body.\n")
        sb.append("Downtrend नंतर = Bullish reversal\n\n")
        sb.append("SHOOTING STAR: लांब upper wick, खाली छोटी body.\n")
        sb.append("Uptrend नंतर = Bearish reversal\n\n")
        sb.append("BULLISH ENGULFING: मोठी green candle मागील red ला cover.\n")
        sb.append("= Strong BUY\n\n")
        sb.append("BEARISH ENGULFING: मोठी red candle मागील green ला cover.\n")
        sb.append("= Strong SELL\n\n")
        sb.append("MORNING STAR: Red, छोटी, Green = Strong BUY\n\n")
        sb.append("EVENING STAR: Green, छोटी, Red = Strong SELL\n\n")
        sb.append("THREE WHITE SOLDIERS: 3 मोठ्या green candles = Very Strong BUY\n\n")
        sb.append("THREE BLACK CROWS: 3 मोठ्या red candles = Very Strong SELL\n\n")
        sb.append("===================================\n\n")
        sb.append("4. SUPPORT & RESISTANCE\n")
        sb.append("-----------------------------------\n")
        sb.append("Support: किंमत खाली येताना जिथे थांबते.\n")
        sb.append("Resistance: किंमत वर जाताना जिथे थांबते.\n\n")
        sb.append("Support तोडले = SELL (Breakdown)\n")
        sb.append("Resistance तोडले = BUY (Breakout)\n")
        sb.append("Support जवळ BUY करा.\n")
        sb.append("Resistance जवळ SELL करा.\n\n")
        sb.append("Support/Resistance कसे ओळखावे:\n")
        sb.append("- किंमत वारंवार जिथे थांबते ती level\n")
        sb.append("- Round numbers (100, 500, 1000)\n")
        sb.append("- Previous High/Low\n\n")
        sb.append("===================================\n\n")
        sb.append("5. VOLUME ANALYSIS\n")
        sb.append("-----------------------------------\n")
        sb.append("Volume = किती shares trade झाले\n\n")
        sb.append("High Volume + Green = Strong BUY\n")
        sb.append("High Volume + Red = Strong SELL\n")
        sb.append("Low Volume = Weak signal\n\n")
        sb.append("Breakout + High Volume = Confirm signal\n")
        sb.append("Breakout + Low Volume = Fake breakout असू शकतो\n\n")
        sb.append("===================================\n\n")
        sb.append("6. MOVING AVERAGES\n")
        sb.append("-----------------------------------\n")
        sb.append("SMA 20 = 20 दिवसांची average\n")
        sb.append("EMA 9/21 = Fast average\n")
        sb.append("EMA 50/200 = Slow average\n\n")
        sb.append("Price > MA 200 = Long-term Bullish\n")
        sb.append("Price < MA 200 = Long-term Bearish\n")
        sb.append("EMA 9 crosses EMA 21 वर = BUY\n")
        sb.append("EMA 9 crosses EMA 21 खाली = SELL\n\n")
        sb.append("Golden Cross: MA 50 crosses MA 200 वर = Strong BUY\n")
        sb.append("Death Cross: MA 50 crosses MA 200 खाली = Strong SELL\n\n")
        sb.append("===================================\n\n")
        sb.append("7. MACD\n")
        sb.append("-----------------------------------\n")
        sb.append("MACD Line व **...**

_This response is too long to display in full._
