package com.wife.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CallDao {
    @Insert
    void insert(CallEntity call);

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    List<CallEntity> getAllCalls();
}
