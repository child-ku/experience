package com.example.voicerecorder.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.voicerecorder.model.Recording;

import java.util.List;

@Dao
public interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    List<Recording> getAllRecordings();

    @Insert
    void insertRecording(Recording recording);

    @Update
    void updateRecording(Recording recording);

    @Delete
    void deleteRecording(Recording recording);
}
