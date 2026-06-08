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

        binding.tvTradingContent.text = getTradingInfo()
    }

    private fun getTradingInfo(): String {
        return """
📈 TRADING BASICS - संपूर्ण माहिती
═══════════════════════════════════

🕯️ CANDLESTICK म्हणजे काय?
Candlestick हे chart वर एक candle सारखे दिसते. यात 4 गोष्टी असतात:
• Open (बाजार सुरू होण्याची किंमत)
• Close (बाजार बंद होण्याची किंमत)
• High (सर्वात जास्त किंमत)
• Low (सर्वात कमी किंमत)

🟢 GREEN CANDLE (Bullish):
Close > Open म्हणजे किंमत वाढली = BUY signal

🔴 RED CANDLE (Bearish):
Close < Open म्हणजे किंमत घसरली = SELL signal

═══════════════════════════════════
📊 IMPORTANT PATTERNS

1. DOJI (डोजी):
Open आणि Close जवळजवळ सारखे असतात.
Market indecision दाखवतो - trend बदलू शकतो.

2. HAMMER (हॅमर):
लांब lower wick, छोटी body.
Downtrend नंतर येतो = Bullish reversal.

3. SHOOTING STAR:
लांब upper wick, छोटी body.
Uptrend नंतर येतो = Bearish reversal.

4. ENGULFING:
Bullish Engulfing: मोठी green candle, मागील red ला cover करते = BUY
Bearish Engulfing: मोठी red candle, मागील green ला cover करते = SELL

═══════════════════════════════════
📉 SUPPORT & RESISTANCE

Support: किंमत खाली येत असताना थांबते ती level
Resistance: किंमत वर जाताना थांबते ती level

Rule: Support तोडले तर = SELL
      Resistance तोडले तर = BUY

═══════════════════════════════════
📊 VOLUME म्हणजे काय?

Volume = किती shares/contracts trade झाले

High Volume + Green Candle = Strong BUY
High Volume + Red Candle = Strong SELL
Low Volume = Weak signal - धोका

═══════════════════════════════════
🎯 TREND म्हणजे काय?

Uptrend: Higher Highs + Higher Lows = BUY the dips
Downtrend: Lower Highs + Lower Lows = SELL the rallies
Sideways: No clear direction = Wait

═══════════════════════════════════
⚡ RISK MANAGEMENT (सर्वात महत्त्वाचे!)

1. Stop Loss: तुमचा maximum loss fix करा
   Rule: कधीही 2% पेक्षा जास्त risk नको एका trade मध्ये

2. Risk:Reward Ratio:
   कमीत कमी 1:2 असावे
   म्हणजे 100 risk → 200 profit

3. Position Sizing:
   एका trade मध्ये total capital चे जास्तीत जास्त 5%

4. Diversification:
   सगळे पैसे एकाच stock मध्ये नको

═══════════════════════════════════
📱 TECHNICAL INDICATORS

RSI (Relative Strength Index):
• 0-30 = Oversold = BUY opportunity
• 70-100 = Overbought = SELL opportunity
• 30-70 = Normal zone

MACD:
• MACD line crosses Signal line वर = BUY
• MACD line crosses Signal line खाली = SELL

Moving Average (MA):
• Price > MA = Bullish trend
• Price < MA = Bearish trend
• MA crossover = Trend change

Bollinger Bands:
• Price touches lower band = BUY
• Price touches upper band = SELL

═══════════════════════════════════
⚠️ IMPORTANT WARNINGS

1. Trading मध्ये risk असतो - carefully करा
2. कधीही loan घेऊन trade करू नका
3. FOMO (Fear of Missing Out) टाळा
4. एकट्याने सगळ्या signals वर विश्वास ठेवू नका
5. Paper trading ने practice करा आधी
6. News आणि events कडे लक्ष द्या

═══════════════════════════════════
💡 BEGINNER TIPS

✅ छोट्या amount ने सुरुवात करा
✅ Stop loss नेहमी लावा
✅ Chart जास्त वेळ study करा
✅ एकाच sector मध्ये focus करा सुरुवातीला
✅ Journal ठेवा - प्रत्येक trade record करा
❌ Tips वर आंधळेपणाने विश्वास ठेवू नका
❌ एकाच रात्रीत श्रीमंत होण्याचा विचार नको
        """.trimIndent()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
