package com.example.voicerecorder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.voicerecorder.database.AppDatabase;
import com.example.voicerecorder.model.Recording;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.Screen;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW
    };

    private ListView recordingListView;
    private RecordingAdapter recordingAdapter;
    private List<Recording> recordings;
    private AppDatabase database;
    private ExecutorService executorService;
    private Handler mainHandler;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private int currentPlayingPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordingListView = findViewById(R.id.recordingListView);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        database = AppDatabase.getInstance(this);

        // Check permissions
        if (checkPermissions()) {
            setupFloatWindow();
            loadRecordings();
        } else {
            requestPermissions();
        }

        // Register broadcast receiver for recording completion
        registerReceiver(recordingCompletionReceiver, new IntentFilter("RECORDING_COMPLETED"));
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                setupFloatWindow();
                loadRecordings();
            } else {
                Toast.makeText(this, "Permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupFloatWindow() {
        View floatView = LayoutInflater.from(this).inflate(R.layout.float_recording_control, null);
        Button startButton = floatView.findViewById(R.id.startButton);
        Button pauseButton = floatView.findViewById(R.id.pauseButton);
        Button stopButton = floatView.findViewById(R.id.stopButton);

        FloatWindow.with(getApplicationContext())
                .setView(floatView)
                .setWidth(Screen.width, 0.8f)
                .setHeight(Screen.height, 0.1f)
                .setX(Screen.width, 0.1f)
                .setY(Screen.height, 0.8f)
                .setMoveType(MoveType.slide)
                .build();

        startButton.setOnClickListener(v -> startRecording());
        pauseButton.setOnClickListener(v -> pauseRecording());
        stopButton.setOnClickListener(v -> stopRecording());
    }

    private void startRecording() {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction("START_RECORDING");
        startService(intent);
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
    }

    private void pauseRecording() {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction("PAUSE_RECORDING");
        startService(intent);
        Toast.makeText(this, "Recording paused", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction("STOP_RECORDING");
        startService(intent);
    }

    private void loadRecordings() {
        executorService.execute(() -> {
            recordings = database.recordingDao().getAllRecordings();
            mainHandler.post(() -> {
                recordingAdapter = new RecordingAdapter(MainActivity.this, recordings);
                recordingListView.setAdapter(recordingAdapter);

                recordingListView.setOnItemClickListener((parent, view, position, id) -> {
                    Recording recording = recordings.get(position);
                    showRecordingOptions(recording, position);
                });
            });
        });
    }

    private void showRecordingOptions(Recording recording, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Options")
                .setItems(new String[]{"Play", "Edit Note", "Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            playRecording(recording, position);
                            break;
                        case 1:
                            editNote(recording);
                            break;
                        case 2:
                            deleteRecording(recording);
                            break;
                    }
                });
        builder.create().show();
    }

    private void playRecording(Recording recording, int position) {
        if (isPlaying) {
            if (currentPlayingPosition == position) {
                mediaPlayer.stop();
                mediaPlayer.release();
                isPlaying = false;
                currentPlayingPosition = -1;
                Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
                return;
            } else {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(recording.getFilePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            currentPlayingPosition = position;
            Toast.makeText(this, "Playing", Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                currentPlayingPosition = -1;
                Toast.makeText(this, "Playback completed", Toast.LENGTH_SHORT).show();
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error playing recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void editNote(Recording recording) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_note, null);
        EditText noteEditText = view.findViewById(R.id.noteEditText);
        noteEditText.setText(recording.getNote());

        builder.setView(view)
                .setTitle("Edit Note")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newNote = noteEditText.getText().toString().trim();
                    recording.setNote(newNote);
                    executorService.execute(() -> {
                        database.recordingDao().updateRecording(recording);
                        mainHandler.post(() -> {
                            recordingAdapter.notifyDataSetChanged();
                            Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void deleteRecording(Recording recording) {
        executorService.execute(() -> {
            database.recordingDao().deleteRecording(recording);
            mainHandler.post(() -> {
                recordings.remove(recording);
                recordingAdapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Recording deleted", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private BroadcastReceiver recordingCompletionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String filePath = intent.getStringExtra("filePath");
            if (filePath != null) {
                // Create a new recording with default note
                Recording recording = new Recording(filePath, 0, System.currentTimeMillis(), "");
                executorService.execute(() -> {
                    database.recordingDao().insertRecording(recording);
                    mainHandler.post(() -> {
                        recordings.add(0, recording);
                        recordingAdapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "Recording saved", Toast.LENGTH_SHORT).show();
                    });
                });
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(recordingCompletionReceiver);
        if (executorService != null) {
            executorService.shutdown();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}
