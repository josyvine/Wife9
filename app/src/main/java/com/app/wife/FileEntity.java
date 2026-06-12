package com.wife.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "files")
public class FileEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String filename;
    private long size;
    private String path;
    private long timestamp;

    public FileEntity() {}

    public FileEntity(String filename, long size, String path, long timestamp) {
        this.filename = filename;
        this.size = size;
        this.path = path;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
