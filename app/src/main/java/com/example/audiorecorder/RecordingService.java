package com.example.audiorecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
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

    public static final String ACTION_START = "com.example.audiorecorder.ACTION_START";
    public static final String ACTION_PAUSE = "com.example.audiorecorder.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.example.audiorecorder.ACTION_STOP";

    private static final String CHANNEL_ID = "RecordingServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private long startTimeMillis = 0;
    private long pausedTimeMillis = 0;
    private String mCurrentFilePath;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START:
                    startRecording();
                    break;
                case ACTION_PAUSE:
                    pauseRecording();
                    break;
                case ACTION_STOP:
                    stopRecording();
                    break;
            }
        }
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
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("录音中")
                .setContentText("点击返回应用")
                .setSmallIcon(R.drawable.ic_mic)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void startRecording() {
        if (isRecording) {
            return;
        }

        try {
            // 创建录音目录
            File directory = new File(Environment.getExternalStorageDirectory(), "AudioRecorder");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 创建录音文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "recording_" + timeStamp + "_0_" + ".mp3";
            mCurrentFilePath = directory.getAbsolutePath() + File.separator + fileName;

            // 初始化MediaRecorder
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setOutputFile(mCurrentFilePath);

            mMediaRecorder.prepare();
            mMediaRecorder.start();

            isRecording = true;
            isPaused = false;
            startTimeMillis = System.currentTimeMillis();

            // 启动前台服务
            startForegroundService();

            Log.d("RecordingService", "开始录音: " + mCurrentFilePath);

        } catch (IOException e) {
            Log.e("RecordingService", "录音失败: " + e.getMessage());
            e.printStackTrace();
            stopSelf();
        }
    }

    private void pauseRecording() {
        if (isRecording && !isPaused) {
            mMediaRecorder.pause();
            isPaused = true;
            pausedTimeMillis = System.currentTimeMillis() - startTimeMillis;
            Log.d("RecordingService", "暂停录音");
        }
    }

    private void resumeRecording() {
        if (isRecording && isPaused) {
            mMediaRecorder.resume();
            isPaused = false;
            startTimeMillis = System.currentTimeMillis() - pausedTimeMillis;
            Log.d("RecordingService", "恢复录音");
        }
    }

    private void stopRecording() {
        if (isRecording) {
            try {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;

                isRecording = false;
                isPaused = false;

                // 计算录音时长
                long durationMillis = System.currentTimeMillis() - startTimeMillis;

                // 重命名文件，添加时长
                if (mCurrentFilePath != null) {
                    File oldFile = new File(mCurrentFilePath);
                    if (oldFile.exists()) {
                        String newFileName = oldFile.getName().replace("_0_", "_" + durationMillis + "_");
                        File newFile = new File(oldFile.getParent(), newFileName);
                        if (oldFile.renameTo(newFile)) {
                            Log.d("RecordingService", "录音文件重命名成功: " + newFile.getAbsolutePath());
                        }
                    }
                }

                Log.d("RecordingService", "停止录音，时长: " + durationMillis + "ms");

            } catch (IllegalStateException e) {
                Log.e("RecordingService", "停止录音失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }
}