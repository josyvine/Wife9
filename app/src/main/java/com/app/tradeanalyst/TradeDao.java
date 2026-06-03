package com.tradeanalyst.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TradeDao {
    @Query("SELECT * FROM paper_trades ORDER BY timestamp DESC")
    List<PaperTradeTransaction> getAllPaperTrades();

    @Insert
    void insertPaperTrade(PaperTradeTransaction trade);

    @Query("SELECT * FROM analysis_history ORDER BY timestamp DESC")
    List<AnalysisHistoryEntity> getAllAnalysisHistory();

    @Insert
    void insertAnalysisHistory(AnalysisHistoryEntity history);

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    List<ConversationEntity> getAllConversations();

    @Insert
    void insertConversation(ConversationEntity conversation);

    @Query("SELECT * FROM price_alerts ORDER BY id DESC")
    List<PriceAlertEntity> getAllPriceAlerts();

    @Query("SELECT * FROM price_alerts WHERE isActive = 1")
    List<PriceAlertEntity> getActivePriceAlerts();

    @Insert
    void insertPriceAlert(PriceAlertEntity alert);

    @Query("UPDATE price_alerts SET isActive = :active WHERE id = :id")
    void setPriceAlertActive(int id, boolean active);

    @Query("DELETE FROM price_alerts WHERE id = :id")
    void deletePriceAlert(int id);
}
