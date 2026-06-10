package com.tradinganalyzer.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tradinganalyzer.data.TradeRecord
import com.tradinganalyzer.databinding.ItemHistoryBinding

class HistoryAdapter(private val records: List<TradeRecord>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        with(holder.binding) {
            tvDate.text = record.date
            tvSignal.text = record.signal
            tvConfidence.text = "Confidence: ${record.confidence}%"
            tvTrend.text = record.trend
            tvSignal.setTextColor(
                if (record.signal == "BUY") Color.parseColor("#4CAF50")
                else Color.parseColor("#F44336")
            )
            tvFeedback.text = when (record.isCorrect) {
                true -> "Correct"
                false -> "Wrong"
                null -> "Pending"
            }
        }
    }

    override fun getItemCount() = records.size
    }
