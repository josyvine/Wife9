package com.wife.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(MessageEntity message);

    @Query("SELECT * FROM messages WHERE (sender = :peer AND receiver = :self) OR (sender = :self AND receiver = :peer) ORDER BY timestamp ASC")
    List<MessageEntity> getChatHistory(String peer, String self);

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    List<MessageEntity> getAllMessages();
}
