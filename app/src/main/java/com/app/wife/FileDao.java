package com.wife.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FileDao {
    @Insert
    void insert(FileEntity file);

    @Query("SELECT * FROM files ORDER BY timestamp DESC")
    List<FileEntity> getAllFiles();
}
