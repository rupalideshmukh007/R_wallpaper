package com.tradinganalyzer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tradinganalyzer.adapter.HistoryAdapter
import com.tradinganalyzer.data.AppDatabase
import com.tradinganalyzer.databinding.ActivityHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Analysis History"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(applicationContext).tradeDao().getAll()
            }

            if (records.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.cardStats.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.cardStats.visibility = View.VISIBLE

                val adapter = HistoryAdapter(records)
                binding.recyclerView.adapter = adapter

                // Stats
                val total = records.size
                val withFeedback = records.filter { it.isCorrect != null }
                val correct = withFeedback.count { it.isCorrect == true }
                val wrong = withFeedback.count { it.isCorrect == false }
                val accuracy = if (withFeedback.isNotEmpty()) {
                    (correct.toFloat() / withFeedback.size * 100).toInt()
                } else 0

                binding.tvTotalAnalysis.text = "Total Analysis: $total"
                binding.tvCorrect.text = "✅ Correct: $correct"
                binding.tvWrong.text = "❌ Wrong: $wrong"
                binding.tvAccuracy.text = "🎯 Accuracy: $accuracy%"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
