package com.tradinganalyzer

import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tradinganalyzer.analysis.AnalysisResult
import com.tradinganalyzer.analysis.ChartAnalyzer
import com.tradinganalyzer.data.AppDatabase
import com.tradinganalyzer.data.TradeRecord
import com.tradinganalyzer.databinding.ActivityAnalysisBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisBinding
    private var recordId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Chart Analysis"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val imageUriString = intent.getStringExtra("IMAGE_URI")
        if (imageUriString == null) {
            Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)
        binding.imageView.setImageURI(imageUri)
        analyzeChart(imageUri)

        binding.btnRight.setOnClickListener { saveFeedback(true) }
        binding.btnWrong.setOnClickListener { saveFeedback(false) }
    }

    private fun analyzeChart(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardResult.visibility = View.GONE
        binding.cardFeedback.visibility = View.GONE
        binding.cardNotChart.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)
                }

                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AnalysisActivity, "Image load failed", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val result = withContext(Dispatchers.Default) {
                    ChartAnalyzer.analyze(bitmap)
                }

                withContext(Dispatchers.Main) {
                    if (result.isValidChart) {
                        displayResult(result)
                    } else {
                        showNotChartError(result.notChartReason)
                    }
                }

                if (result.isValidChart) {
                    recordId = withContext(Dispatchers.IO) {
                        val db = AppDatabase.getInstance(applicationContext)
                        val record = TradeRecord(
                            date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                            signal = result.signal,
                            confidence = result.confidence,
                            greenPercent = result.greenPercent,
                            redPercent = result.redPercent,
                            trend = result.trend,
                            isCorrect = null
                        )
                        db.tradeDao().insert(record)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AnalysisActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun showNotChartError(reason: String) {
        binding.cardNotChart.visibility = View.VISIBLE
        binding.cardResult.visibility = View.GONE
        binding.cardFeedback.visibility = View.GONE
        binding.tvNotChartReason.text = reason
    }

    private fun displayResult(result: AnalysisResult) {
        binding.cardResult.visibility = View.VISIBLE
        binding.cardFeedback.visibility = View.VISIBLE
        binding.cardNotChart.visibility = View.GONE

        binding.tvSignal.text = result.signal
        if (result.signal == "BUY") {
            binding.tvSignal.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            binding.tvSignal.setTextColor(Color.parseColor("#F44336"))
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

    private fun saveFeedback(isCorrect: Boolean) {
        if (recordId == -1L) return
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(applicationContext).tradeDao().updateFeedback(recordId, isCorrect)
            withContext(Dispatchers.Main) {
                val msg = if (isCorrect) "✅ Marked as CORRECT!" else "❌ Marked as WRONG!"
                Toast.makeText(this@AnalysisActivity, msg, Toast.LENGTH_SHORT).show()
                binding.cardFeedback.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
