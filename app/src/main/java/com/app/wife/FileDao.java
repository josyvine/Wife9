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

    // Query to delete a file transfer entry from the database by its primary ID key
    @Query("DELETE FROM files WHERE id = :fileId")
    void deleteById(long fileId);
}