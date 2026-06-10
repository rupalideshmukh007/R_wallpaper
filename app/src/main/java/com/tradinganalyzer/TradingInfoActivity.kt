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
        binding.tvTradingContent.text = getInfo()
    }

    private fun getInfo(): String {
        val sb = StringBuilder()
        sb.append(part1())
        sb.append(part2())
        sb.append(part3())
        sb.append(part4())
        return sb.toString()
    }

    private fun part1(): String {
        val sb = StringBuilder()
        sb.append("TRADING - माहिती\n===========================\n\n")
        sb.append("1. CANDLESTICK\n---------------------------\n")
        sb.append("Open - सुरू होण्याची किंमत\n")
        sb.append("Close - बंद होण्याची किंमत\n")
        sb.append("High - सर्वात जास्त किंमत\n")
        sb.append("Low - सर्वात कमी किंमत\n\n")
        sb.append("GREEN = किंमत वाढली = BUY\n")
        sb.append("RED = किंमत घसरली = SELL\n\n")
        sb.append("2. RSI\n---------------------------\n")
        sb.append("RSI 70+ = Overbought - खूप खरेदी झाली\n")
        sb.append("किंमत खाली येण्याची शक्यता. BUY टाळा.\n\n")
        sb.append("RSI 30- = Oversold - खूप विक्री झाली\n")
        sb.append("किंमत वर जाण्याची शक्यता. BUY opportunity.\n\n")
        sb.append("RSI 40-60 = Neutral. Wait करा.\n\n")
        sb.append("3. CANDLESTICK PATTERNS\n---------------------------\n")
        sb.append("DOJI: Open = Close. Market uncertain.\n\n")
        sb.append("HAMMER: लांब lower wick, वर छोटी body.\n")
        sb.append("Downtrend नंतर = Bullish reversal.\n\n")
        sb.append("SHOOTING STAR: लांब upper wick, खाली body.\n")
        sb.append("Uptrend नंतर = Bearish reversal.\n\n")
        sb.append("BULLISH ENGULFING: मोठी green मागील red ला cover.\n")
        sb.append("= Strong BUY\n\n")
        sb.append("BEARISH ENGULFING: मोठी red मागील green ला cover.\n")
        sb.append("= Strong SELL\n\n")
        sb.append("MORNING STAR: Red + छोटी + Green = BUY\n")
        sb.append("EVENING STAR: Green + छोटी + Red = SELL\n")
        sb.append("THREE WHITE SOLDIERS: 3 green candles = Strong BUY\n")
        sb.append("THREE BLACK CROWS: 3 red candles = Strong SELL\n\n")
        return sb.toString()
    }

    private fun part2(): String {
        val sb = StringBuilder()
        sb.append("4. SUPPORT & RESISTANCE\n---------------------------\n")
        sb.append("Support: किंमत खाली येताना जिथे थांबते.\n")
        sb.append("Resistance: किंमत वर जाताना जिथे थांबते.\n\n")
        sb.append("Support तोडले = SELL. Resistance तोडले = BUY.\n")
        sb.append("Support जवळ BUY. Resistance जवळ SELL.\n\n")
        sb.append("5. VOLUME\n---------------------------\n")
        sb.append("High Volume + Green = Strong BUY\n")
        sb.append("High Volume + Red = Strong SELL\n")
        sb.append("Low Volume = Weak signal.\n")
        sb.append("Breakout + High Volume = Confirm signal.\n\n")
        sb.append("6. MOVING AVERAGES\n---------------------------\n")
        sb.append("Price > MA 200 = Bullish. Price < MA 200 = Bearish.\n")
        sb.append("EMA 9 crosses EMA 21 वर = BUY.\n")
        sb.append("EMA 9 crosses EMA 21 खाली = SELL.\n")
        sb.append("Golden Cross: MA 50 crosses MA 200 वर = Strong BUY.\n")
        sb.append("Death Cross: MA 50 crosses MA 200 खाली = Strong SELL.\n\n")
        sb.append("7. MACD\n---------------------------\n")
        sb.append("MACD Line वर Signal Line = BUY.\n")
        sb.append("MACD Line खाली Signal Line = SELL.\n")
        sb.append("Histogram Positive = Bullish. Negative = Bearish.\n\n")
        sb.append("8. BOLLINGER BANDS\n---------------------------\n")
        sb.append("Lower Band touch = Oversold = BUY.\n")
        sb.append("Upper Band touch = Overbought = SELL.\n")
        sb.append("Bands squeeze = मोठी move येणार.\n\n")
        return sb.toString()
    }

    private fun part3(): String {
        val sb = StringBuilder()
        sb.append("9. TREND\n---------------------------\n")
        sb.append("Uptrend: Higher Highs + Higher Lows = BUY dips.\n")
        sb.append("Downtrend: Lower Highs + Lower Lows = SELL rallies.\n")
        sb.append("Sideways = Wait करा.\n\n")
        sb.append("10. TIMEFRAMES\n---------------------------\n")
        sb.append("1-5 Min = Scalping.\n")
        sb.append("15 Min - 1 Hour = Intraday (Best).\n")
        sb.append("4 Hour - Daily = Swing trading.\n")
        sb.append("Weekly/Monthly = Long-term.\n\n")
        sb.append("11. RISK MANAGEMENT\n---------------------------\n")
        sb.append("Stop Loss नेहमी लावा.\n")
        sb.append("एका trade मध्ये 1-2% capital risk.\n")
        sb.append("Risk:Reward कमीत कमी 1:2.\n")
        sb.append("1000 risk = 2000 profit target.\n")
        sb.append("3-5 stocks मध्ये divide करा.\n\n")
        sb.append("12. INTRADAY RULES\n---------------------------\n")
        sb.append("9:30 नंतर trade सुरू करा.\n")
        sb.append("3:00 च्या आधी positions close करा.\n")
        sb.append("News वेळी trade नको.\n")
        sb.append("दिवसाला 2-3 trades पुरे.\n")
        sb.append("Loss नंतर revenge trade नको.\n\n")
        sb.append("13. OPTIONS BASICS\n---------------------------\n")
        sb.append("Call CE: Stock वर जाईल = Call Buy.\n")
        sb.append("Put PE: Stock खाली जाईल = Put Buy.\n")
        sb.append("Strike Price: Option ची किंमत level.\n")
        sb.append("Premium: Option ची किंमत.\n")
        sb.append("Theta Decay: Premium रोज कमी होतो.\n\n")
        return sb.toString()
    }

    private fun part4(): String {
        val sb = StringBuilder()
        sb.append("14. CHART PATTERNS\n---------------------------\n")
        sb.append("Head & Shoulders = Bearish reversal.\n")
        sb.append("Inverse H&S = Bullish reversal.\n")
        sb.append("Double Top: दोनदा same level वर परतली = SELL.\n")
        sb.append("Double Bottom: दोनदा bounce झाली = BUY.\n")
        sb.append("Ascending Triangle = BUY breakout.\n")
        sb.append("Descending Triangle = SELL breakdown.\n")
        sb.append("Cup & Handle = Strong Bullish.\n\n")
        sb.append("15. PSYCHOLOGY\n---------------------------\n")
        sb.append("FOMO: Train गेल्यावर मागे धावू नका.\n")
        sb.append("Greed: Target hit = बाहेर पडा.\n")
        sb.append("Revenge Trading: Loss नंतर break घ्या.\n")
        sb.append("Journal: प्रत्येक trade लिहा.\n\n")
        sb.append("16. QUICK SIGNALS\n---------------------------\n")
        sb.append("Strong BUY:\n")
        sb.append("- Green candle + High volume\n")
        sb.append("- RSI 30 खाली\n")
        sb.append("- Support वर bounce\n\n")
        sb.append("Strong SELL:\n")
        sb.append("- Red candle + High volume\n")
        sb.append("- RSI 70 वर\n")
        sb.append("- Resistance rejection\n\n")
        sb.append("Trade टाळा:\n")
        sb.append("- RSI 40-60\n")
        sb.append("- Sideways market\n")
        sb.append("- Low volume\n")
        sb.append("- News वेळी\n\n")
        sb.append("===========================\n")
        sb.append("DISCLAIMER\n")
        sb.append("Trading मध्ये risk असतो.\n")
        sb.append("Loan घेऊन trade करू नका.\n")
        sb.append("Professional advisor consult करा.\n\n")
        sb.append("सुरुवात:\n")
        sb.append("1. Paper trading - 1 महिना practice\n")
        sb.append("2. 5000-10000 ने सुरुवात करा\n")
        sb.append("3. एकाच stock वर focus\n")
        sb.append("4. Daily journal ठेवा\n")
        return sb.toString()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
