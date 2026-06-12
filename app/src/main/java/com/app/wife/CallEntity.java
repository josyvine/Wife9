package com.wife.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "calls")
public class CallEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String peer;
    private String type; // "Voice" or "Video"
    private long duration; // in seconds
    private long timestamp;

    public CallEntity() {}

    public CallEntity(String peer, String type, long duration, long timestamp) {
        this.peer = peer;
        this.type = type;
        this.duration = duration;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPeer() { return peer; }
    public void setPeer(String peer) { this.peer = peer; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
