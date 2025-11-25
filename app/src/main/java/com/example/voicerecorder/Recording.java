package com.example.voicerecorder;

import java.io.Serializable;
import java.util.Date;

public class Recording implements Serializable {
    private String id;
    private String filePath;
    private long duration;
    private Date recordingTime;
    private String note;

    public Recording() {
    }

    public Recording(String id, String filePath, long duration, Date recordingTime, String note) {
        this.id = id;
        this.filePath = filePath;
        this.duration = duration;
        this.recordingTime = recordingTime;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Date getRecordingTime() {
        return recordingTime;
    }

    public void setRecordingTime(Date recordingTime) {
        this.recordingTime = recordingTime;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
