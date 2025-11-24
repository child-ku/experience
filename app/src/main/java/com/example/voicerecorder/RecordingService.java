package com.example.voicerecorder;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {
    private static final String TAG = "RecordingService";
    private MediaRecorder mediaRecorder;
    private String currentFilePath;
    private boolean isRecording = false;
    private boolean isPaused = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case "START_RECORDING":
                    startRecording();
                    break;
                case "PAUSE_RECORDING":
                    pauseRecording();
                    break;
                case "STOP_RECORDING":
                    stopRecording();
                    break;
            }
        }
        return START_STICKY;
    }

    private void startRecording() {
        if (isRecording) {
            return;
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        currentFilePath = getOutputFilePath();
        mediaRecorder.setOutputFile(currentFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            isPaused = false;
            Log.d(TAG, "Recording started: " + currentFilePath);
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed", e);
        }
    }

    private void pauseRecording() {
        if (isRecording && !isPaused && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            mediaRecorder.pause();
            isPaused = true;
            Log.d(TAG, "Recording paused");
        }
    }

    private void stopRecording() {
        if (isRecording) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            isPaused = false;
            Log.d(TAG, "Recording stopped: " + currentFilePath);

            // Broadcast the result
            Intent intent = new Intent("RECORDING_COMPLETED");
            intent.putExtra("filePath", currentFilePath);
            sendBroadcast(intent);
        }
    }

    private String getOutputFilePath() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "RECORD_" + timeStamp + ".mp4";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        storageDir.mkdirs();
        return new File(storageDir, fileName).getAbsolutePath();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
}
