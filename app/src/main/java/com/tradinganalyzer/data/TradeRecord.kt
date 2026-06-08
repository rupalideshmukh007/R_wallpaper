package com.tradinganalyzer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_records")
data class TradeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val signal: String,
    val confidence: Int,
    val greenPercent: Float,
    val redPercent: Float,
    val trend: String,
    val isCorrect: Boolean?
)
