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
📈 TRADING — संपूर्ण माहिती
═══════════════════════════════════════

🕯️ 1. CANDLESTICK म्हणजे काय?
───────────────────────────────
Candlestick हे chart वर एक candle सारखे दिसते.
यात 4 गोष्टी असतात:
• Open  — बाजार सुरू होण्याची किंमत
• Close — बाजार बंद होण्याची किंमत
• High  — सर्वात जास्त किंमत
• Low   — सर्वात कमी किंमत

🟢 GREEN CANDLE (Bullish):
Close > Open = किंमत वाढली = BUY signal

🔴 RED CANDLE (Bearish):
Close < Open = किंमत घसरली = SELL signal

═══════════════════════════════════════

📊 2. RSI — Relative Strength Index
───────────────────────────────
RSI हे indicator 0 ते 100 च्या दरम्यान असतो.

🔴 RSI 70+ = Overbought
खूप जास्त खरेदी झाली आहे.
किंमत खाली येण्याची शक्यता जास्त.
नवीन BUY position टाळा.

🟢 RSI 30- = Oversold
खूप जास्त विक्री झाली आहे.
किंमत वर जाण्याची शक्यता जास्त.
BUY opportunity असू शकते.

➡️ RSI 40-60 = Neutral
कोणताही strong signal नाही.
Wait करा.

RSI + Candle pattern एकत्र वापरल्यास accuracy वाढते.

═══════════════════════════════════════

🎯 3. IMPORTANT CANDLESTICK PATTERNS
───────────────────────────────

DOJI:
Open आणि Close जवळजवळ सारखे.
Market indecision दाखवतो.
Trend बदलण्याचा signal असू शकतो.

HAMMER:
लांब lower wick, छोटी body वर.
Downtrend नंतर येतो.
= Bullish reversal signal ✅

SHOOTING STAR:
लांब upper wick, छोटी body खाली.
Uptrend नंतर येतो.
= Bearish reversal signal ✅

BULLISH ENGULFING:
मोठी green candle, मागील red ला पूर्णपणे cover करते.
= Strong BUY signal ✅

BEARISH ENGULFING:
मोठी red candle, मागील green ला cover करते.
= Strong SELL signal ✅

MORNING STAR:
3 candles — Red, छोटी, Green.
Downtrend नंतर येतो.
= Strong BUY signal ✅

EVENING STAR:
3 candles — Green, छोटी, Red.
Uptrend नंतर येतो.
= Strong SELL signal ✅

THREE WHITE SOLDIERS:
3 लागोपाठ मोठ्या green candles.
= Very Strong BUY ✅✅

THREE BLACK CROWS:
3 लागोपाठ मोठ्या red candles.
= Very Strong SELL ✅✅

═══════════════════════════════════════

📉 4. SUPPORT & RESISTANCE
───────────────────────────────
Support: किंमत खाली येताना जिथे थांबते ती level.
Resistance: किंमत वर जाताना जिथे थांबते ती level.

Rules:
✅ Support तोडले तर = SELL (Breakdown)
✅ Resistance तोडले तर = BUY (Breakout)
✅ Support जवळ BUY करा
✅ Resistance जवळ SELL करा

Support/Resistance कसे ओळखावे:
• Chart वर किंमत वारंवार जिथे थांबते ती level
• Round numbers (100, 500, 1000) = strong levels
• Previous High/Low = important levels

═══════════════════════════════════════

📊 5. VOLUME ANALYSIS
───────────────────────────────
Volume = किती shares/contracts trade झाले

High Volume + Green Candle = Strong BUY 🔥
High Volume + Red Candle = Strong SELL 🔥
Low Volume = Weak signal — विश्वास ठेवू नका ⚠️

Volume Rules:
• Breakout + High Volume = Confirm signal
• Breakout + Low Volume = Fake breakout असू शकतो
• Volume वाढत असेल तर trend strong आहे
• Volume कमी होत असेल तर trend weak होत आहे

═══════════════════════════════════════

📈 6. MOVING AVERAGES (MA)
───────────────────────────────
Moving Average = किमतींची average line

MA Types:
• SMA 20 = 20 दिवसांची simple average
• EMA 9/21 = Fast moving average
• EMA 50/200 = Slow moving average

Rules:
✅ Price > MA 200 = Long-term Bullish
✅ Price < MA 200 = Long-term Bearish
✅ EMA 9 crosses EMA 21 वर = BUY
✅ EMA 9 crosses EMA 21 खाली = SELL

Golden Cross:
MA 50 crosses MA 200 वर = Strong BUY signal

Death Cross:
MA 50 crosses MA 200 खाली = Strong SELL signal

═══════════════════════════════════════

⚡ 7. MACD Indicator
───────────────────────────────
MACD = Moving Average Convergence Divergence

MACD Line वर Signal Line = BUY 📈
MACD Line खाली Signal Line = SELL 📉

Histogram:
Positive (वर) = Bullish momentum
Negative (खाली) = Bearish momentum

MACD + RSI एकत्र = Best combination

═══════════════════════════════════════

📊 8. BOLLINGER BANDS
───────────────────────────────
3 lines — Upper, Middle (MA 20), Lower

Rules:
✅ Price touches Lower Band = Oversold = BUY
✅ Price touches Upper Band = Overbought = SELL
✅ Bands squeeze = Big move येणार आहे
✅ Bands wide = High volatility

Bollinger Squeeze:
Bands अचानक narrow झाल्यावर मोठी move येते.
Direction कळत नाही — दोन्ही बाजूंसाठी तयार राहा.

═══════════════════════════════════════

🎯 9. TREND ANALYSIS
───────────────────────────────
Uptrend:
Higher Highs + Higher Lows
= BUY the dips (घसरणीत खरेदी करा)

Downtrend:
Lower Highs + Lower Lows
= SELL the rallies (वाढीत विक्री करा)

Sideways:
No clear direction
= Wait करा, trade नको

Trend Lines:
Uptrend line तोडले = Trend बदलतोय
Downtrend line तोडले = Reversal येतोय

═══════════════════════════════════════

💰 10. TIMEFRAMES
───────────────────────────────
1 Minute / 5 Minute:
= Scalping (खूप short trades)
= जास्त noise, experience लागतो

15 Minute / 1 Hour:
= Intraday trading साठी best
= Clear signals मिळतात

4 Hour / Daily:
= Swing trading (2-5 दिवस)
= Most reliable signals

Weekly / Monthly:
= Long-term investment
= Strong trends दिसतात

Rule: जास्त timeframe = जास्त reliable signal

═══════════════════════════════════════

🛡️ 11. RISK MANAGEMENT (सर्वात महत्त्वाचे!)
───────────────────────────────

Stop Loss:
नेहमी stop loss लावा.
Rule: एका trade मध्ये जास्तीत जास्त 1-2% capital risk.

Position Sizing:
एका trade मध्ये total capital चे जास्तीत जास्त 5%.
Example: 1 lakh capital → एका trade मध्ये 5000 पेक्षा जास्त नको.

Risk:Reward Ratio:
कमीत कमी 1:2 असावे.
म्हणजे 1000 risk → 2000 profit target.
1:3 असेल तर आणखी चांगले.

Diversification:
सगळे पैसे एकाच stock मध्ये नको.
3-5 different stocks मध्ये divide करा.

═══════════════════════════════════════

📱 12. INTRADAY TRADING RULES
───────────────────────────────
✅ सकाळी 9:30 नंतर trade सुरू करा
   (Market opening volatility टाळा)
✅ 3:00 च्या आधी सगळ्या positions close करा
✅ News वेळी trade करू नका (Budget, RBI, Results)
✅ दिवसाला जास्तीत जास्त 2-3 trades
✅ Loss झाल्यावर revenge trade करू नका
✅ Target hit झाल्यावर greed करू नका

Best Intraday Stocks:
• High volume असलेले stocks
• F&O मध्ये असलेले stocks
• Nifty 50 चे stocks

═══════════════════════════════════════

📊 13. OPTIONS TRADING BASICS
───────────────────────────────
Call Option (CE):
Stock वर जाईल असे वाटते तेव्हा Call Buy करा.
= Limited risk, unlimited profit

Put Option (PE):
Stock खाली जाईल असे वाटते तेव्हा Put Buy करा.
= Limited risk, unlimited profit

Option Selling:
Call/Put विकणे = जास्त profit पण unlimited risk.
Experience असल्याशिवाय करू नका.

Important Terms:
• Strike Price: ज्या किमतीवर option आहे
• Expiry: Option संपण्याची तारीख
• Premium: Option ची किंमत
• ITM: In the Money (profitable)
• OTM: Out of the Money (not profitable yet)
• ATM: At the Money

Theta Decay:
Option चा premium रोज कमी होत जातो.
= Option buyers साठी नुकसानदायक
= Option sellers साठी फायदेशीर

═══════════════════════════════════════

🔍 14. CHART PATTERNS
───────────────────────────────
Head and Shoulders:
= Bearish reversal pattern
= Strong SELL signal

Inverse Head and Shoulders:
= Bullish reversal pattern
= Strong BUY signal

Double Top:
किंमत दोनदा same level वर जाऊन परतली.
= Bearish signal, SELL करा

Double Bottom:
किंमत दोनदा same level ला येऊन वर गेली.
= Bullish signal, BUY करा

Triangle Patterns:
Ascending Triangle = BUY breakout
Descending Triangle = SELL breakdown
Symmetrical = Wait करा

Cup and Handle:
= Strong Bullish pattern
= Breakout नंतर BUY करा

Flag Pattern:
Strong move नंतर consolidation, मग पुन्हा same direction.
= Trend continuation pattern

═══════════════════════════════════════

💡 15. TRADING PSYCHOLOGY
───────────────────────────────
सर्वात मोठा शत्रू = स्वतःचे मन

FOMO (Fear of Missing Out):
Train निघून गेल्यावर मागे धावू नका.
पुढची train येईल — wait करा.

Greed:
Target hit झाल्यावर बाहेर पडा.
"आणखी थोडे" म्हणत राहू नका.

Fear:
Stop loss लावा आणि trust ठेवा.
Loss चा fear असेल तर trade करू नका.

Revenge Trading:
Loss नंतर लगेच मोठा trade करू नका.
Break घ्या, calm व्हा.

Overtrading:
जास्त trades = जास्त commission + जास्त mistakes.
Quality > Quantity.

Journal:
प्रत्येक trade लिहा:
Entry, Exit, Reason, Result.
Pattern कळेल — mistakes कमी होतील.

═══════════════════════════════════════

📌 16. QUICK REFERENCE — SIGNALS SUMMARY
───────────────────────────────
Strong BUY:
• Green candle + High volume
• RSI 30 खाली (oversold)
• Support वर bounce
• Bullish Engulfing pattern
• MACD crossover वर

Strong SELL:
• Red candle + High volume
• RSI 70 वर (overbought)
• Resistance वर rejection
• Bearish Engulfing pattern
• MACD crossover खाली

Avoid Trading:
• RSI 40-60 (neutral)
• Sideways market
• News वेळी
• Low volume
• Market opening 15 min

═══════════════════════════════════════

⚠️ IMPORTANT DISCLAIMER
───────────────────────────────
• Trading मध्ये risk असतो
• Past performance future guarantee नाही
• कधीही loan घेऊन trade करू नका
• हे app educational purpose साठी आहे
• Professional advisor consult करा

🎯 सुरुवात कशी करावी?
1. Paper trading ने 1 महिना practice करा
2. छोट्या amount ने सुरुवात **...**

_This response is too long to display in full._
