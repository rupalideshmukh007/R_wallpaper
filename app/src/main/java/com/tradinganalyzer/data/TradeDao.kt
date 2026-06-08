package com.tradinganalyzer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TradeDao {

    @Insert
    suspend fun insert(record: TradeRecord): Long

    @Query("SELECT * FROM trade_records ORDER BY id DESC")
    suspend fun getAll(): List<TradeRecord>

    @Query("UPDATE trade_records SET isCorrect = :isCorrect WHERE id = :id")
    suspend fun updateFeedback(id: Long, isCorrect: Boolean)

    @Query("SELECT COUNT(*) FROM trade_records WHERE isCorrect = 1")
    suspend fun getCorrectCount(): Int

    @Query("SELECT COUNT(*) FROM trade_records WHERE isCorrect = 0")
    suspend fun getWrongCount(): Int
}
