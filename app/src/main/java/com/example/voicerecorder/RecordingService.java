package com.example.voicerecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {
    private static final String TAG = "RecordingService";
    private static final String CHANNEL_ID = "RecordingServiceChannel";

    private MediaRecorder mediaRecorder;
    private String currentFilePath;
    private long startTimeMillis = 0;
    private long pausedTimeMillis = 0;
    private boolean isRecording = false;
    private boolean isPaused = false;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        RecordingService getService() {
            return RecordingService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recording Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("录音中")
                .setContentText("正在录音...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(1, notification);
    }

    public boolean startRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            currentFilePath = getRecordingFilePath();
            mediaRecorder.setOutputFile(currentFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            startTimeMillis = System.currentTimeMillis();
            isRecording = true;
            isPaused = false;

            startForegroundService();

            Log.d(TAG, "Recording started: " + currentFilePath);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            return false;
        }
    }

    public boolean pauseRecording() {
        if (isRecording && !isPaused && mediaRecorder != null) {
            try {
                mediaRecorder.pause();
                pausedTimeMillis = System.currentTimeMillis() - startTimeMillis;
                isPaused = true;
                Log.d(TAG, "Recording paused");
                return true;
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to pause recording", e);
                return false;
            }
        }
        return false;
    }

    public boolean resumeRecording() {
        if (isRecording && isPaused && mediaRecorder != null) {
            try {
                mediaRecorder.resume();
                startTimeMillis = System.currentTimeMillis() - pausedTimeMillis;
                isPaused = false;
                Log.d(TAG, "Recording resumed");
                return true;
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to resume recording", e);
                return false;
            }
        }
        return false;
    }

    public Recording stopRecording() {
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                long duration = System.currentTimeMillis() - startTimeMillis;
                isRecording = false;
                isPaused = false;

                stopForeground(true);

                Date recordingTime = new Date();
                Recording recording = new Recording(
                        String.valueOf(System.currentTimeMillis()),
                        currentFilePath,
                        duration,
                        recordingTime,
                        ""
                );

                Log.d(TAG, "Recording stopped: " + currentFilePath + ", duration: " + duration);
                return recording;
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to stop recording", e);
                return null;
            }
        }
        return null;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPaused() {
        return isPaused;
    }

    private String getRecordingFilePath() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "REC_" + timeStamp + ".mp4";
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
