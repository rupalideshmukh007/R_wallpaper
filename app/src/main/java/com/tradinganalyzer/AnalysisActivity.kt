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
        binding.progressBar.visibility  = View.VISIBLE
        binding.cardResult.visibility   = View.GONE
        binding.cardFeedback.visibility = View.GONE
        binding.cardNotChart.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val stream: InputStream? = contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(stream)
                }
                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AnalysisActivity, "Image load failed", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val result = withContext(Dispatchers.Default) { ChartAnalyzer.analyze(bitmap) }
                withContext(Dispatchers.Main) {
                    if (result.isValidChart) displayResult(result) else showError(result.notChartReason)
                }
                if (result.isValidChart) {
                    recordId = withContext(Dispatchers.IO) {
                        AppDatabase.getInstance(applicationContext).tradeDao().insert(
                            TradeRecord(
                                date         = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                                signal       = result.signal,
                                confidence   = result.confidence,
                                greenPercent = result.greenPercent,
                                redPercent   = result.redPercent,
                                trend        = result.trend,
                                isCorrect    = null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AnalysisActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) { binding.progressBar.visibility = View.GONE }
            }
        }
    }

    private fun showError(reason: String) {
        binding.cardNotChart.visibility = View.VISIBLE
        binding.cardResult.visibility   = View.GONE
        binding.cardFeedback.visibility = View.GONE
        binding.tvNotChartReason.text   = reason
    }

    private fun displayResult(r: AnalysisResult) {
        binding.cardResult.visibility   = View.VISIBLE
        binding.cardFeedback.visibility = View.VISIBLE
        binding.cardNotChart.visibility = View.GONE

        // ── BUY / SELL signal ──────────────────────────────────────────
        binding.tvSignal.text = r.signal
        binding.tvSignal.setTextColor(
            if (r.signal == "BUY") Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        )

        // ── CHANGE 3 & 4 ──────────────────────────────────────────────
        if (r.hasPatterns) {
            // Pattern आहे → Pattern 90% + Pixel 10% mixed % दाखवा
            binding.tvPatternLabel.text = "📊 Pattern Analysis (90%) + Pixel (10%)"
            binding.tvBuyChance.text    = "BUY\n${String.format("%.1f", r.displayBuyPercent)}%"
            binding.tvSellChance.text   = "SELL\n${String.format("%.1f", r.displaySellPercent)}%"

            // CHANGE 4: Pixel % वेगळं खाली दाखवा
            binding.tvPixelStats.visibility = View.VISIBLE
            binding.tvPixelStats.text =
                "📸 Pixel Only — BUY: ${String.format("%.1f", r.greenPercent)}%  |  " +
                "SELL: ${String.format("%.1f", r.redPercent)}%"
        } else {
            // Pattern नाही → फक्त Pixel %
            binding.tvPatternLabel.text = "📸 Pixel Analysis"
            binding.tvBuyChance.text    = "BUY\n${String.format("%.1f", r.greenPercent)}%"
            binding.tvSellChance.text   = "SELL\n${String.format("%.1f", r.redPercent)}%"
            binding.tvPixelStats.visibility = View.GONE
        }
        binding.tvBuyChance.setTextColor(Color.parseColor("#4CAF50"))
        binding.tvSellChance.setTextColor(Color.parseColor("#F44336"))

        // ── CHANGE 2: Signal Strength = Pattern only ──────────────────
        binding.progressConfidence.progress = r.confidence
        binding.tvConfidence.text =
            if (r.hasPatterns) "Pattern Signal Strength: ${r.confidence}%"
            else               "Pixel Signal Strength: ${r.confidence}%"

        // ── Risk level ────────────────────────────────────────────────
        binding.tvRiskLevel.text = r.riskLevel
        try { binding.tvRiskLevel.setTextColor(Color.parseColor(r.riskColor)) }
        catch (e: Exception) { binding.tvRiskLevel.setTextColor(Color.parseColor("#FF9800")) }

        // ── Wait message ──────────────────────────────────────────────
        if (r.waitMessage.isNotEmpty()) {
            binding.tvWaitMessage.visibility = View.VISIBLE
            binding.tvWaitMessage.text       = r.waitMessage
        } else {
            binding.tvWaitMessage.visibility = View.GONE
        }

        // ── Signal reason ─────────────────────────────────────────────
        if (r.keyFactors.isNotEmpty()) {
            binding.tvSignalReason.visibility = View.VISIBLE
            binding.tvSignalReason.text       = "📌 Reason: ${r.keyFactors}"
        } else {
            binding.tvSignalReason.visibility = View.GONE
        }

        // ── Entry / SL / Target + Pattern list ───────────────────────
        val fullSummary = buildString {
            append(r.supportResistance)
            appendLine(); appendLine()
            if (r.detectedPatterns.isNotEmpty()) append(r.patternDisplay)
            else append("🔍 Chart pattern detect झाला नाही.\nCandlestick chart upload करा.")
        }
        binding.tvSummary.text = fullSummary
    }

    private fun saveFeedback(isCorrect: Boolean) {
        if (recordId == -1L) return
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(applicationContext).tradeDao().updateFeedback(recordId, isCorrect)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AnalysisActivity,
                    if (isCorrect) "Correct marked!" else "Wrong marked!", Toast.LENGTH_SHORT).show()
                binding.cardFeedback.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
