package com.example.voicerecorder;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.Screen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW
    };

    private ListView recordingListView;
    private RecordingAdapter recordingAdapter;
    private List<Recording> recordingList = new ArrayList<>();

    private RecordingService recordingService;
    private boolean isServiceBound = false;

    private View floatView;
    private Button btnRecordControl;
    private TextView tvRecordingTime;
    private Handler handler = new Handler();
    private Runnable timeUpdateRunnable;
    private long startTimeMillis = 0;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecordingService.LocalBinder binder = (RecordingService.LocalBinder) service;
            recordingService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            recordingService = null;
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordingListView = findViewById(R.id.recordingListView);
        recordingAdapter = new RecordingAdapter(this, recordingList);
        recordingListView.setAdapter(recordingAdapter);

        // 绑定录音服务
        Intent serviceIntent = new Intent(this, RecordingService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // 检查权限
        if (checkPermissions()) {
            initFloatWindow();
        } else {
            requestPermissions();
        }
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
                initFloatWindow();
            } else {
                Toast.makeText(this, "需要所有权限才能正常使用应用", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initFloatWindow() {
        floatView = LayoutInflater.from(this).inflate(R.layout.float_recording_control, null);
        btnRecordControl = floatView.findViewById(R.id.btnRecordControl);
        tvRecordingTime = floatView.findViewById(R.id.tvRecordingTime);

        btnRecordControl.setOnClickListener(v -> {
            if (!isServiceBound || recordingService == null) {
                Toast.makeText(MainActivity.this, "服务未绑定", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!recordingService.isRecording()) {
                // 开始录音
                boolean started = recordingService.startRecording();
                if (started) {
                    btnRecordControl.setText(R.string.pause_recording);
                    tvRecordingTime.setVisibility(View.VISIBLE);
                    startTimeMillis = System.currentTimeMillis();
                    startTimer();
                } else {
                    Toast.makeText(MainActivity.this, "录音开始失败", Toast.LENGTH_SHORT).show();
                }
            } else if (recordingService.isPaused()) {
                // 继续录音
                boolean resumed = recordingService.resumeRecording();
                if (resumed) {
                    btnRecordControl.setText(R.string.pause_recording);
                    startTimeMillis = System.currentTimeMillis() - recordingService.stopRecording().getDuration(); // 这里需要调整
                    startTimer();
                } else {
                    Toast.makeText(MainActivity.this, "录音继续失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 暂停录音
                boolean paused = recordingService.pauseRecording();
                if (paused) {
                    btnRecordControl.setText(R.string.resume_recording);
                    stopTimer();
                } else {
                    Toast.makeText(MainActivity.this, "录音暂停失败", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 长按停止录音
        btnRecordControl.setOnLongClickListener(v -> {
            if (!isServiceBound || recordingService == null || !recordingService.isRecording()) {
                return false;
            }

            stopTimer();
            Recording recording = recordingService.stopRecording();
            if (recording != null) {
                recordingList.add(0, recording);
                recordingAdapter.notifyDataSetChanged();
                btnRecordControl.setText(R.string.start_recording);
                tvRecordingTime.setVisibility(View.GONE);
                tvRecordingTime.setText("00:00:00");

                // 显示编辑备注对话框
                showEditNoteDialog(recording);
            } else {
                Toast.makeText(MainActivity.this, "录音停止失败", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // 初始化悬浮窗
        FloatWindow.with(getApplicationContext())
                .setView(floatView)
                .setWidth(Screen.width, 0.3f)
                .setHeight(Screen.height, 0.1f)
                .setX(Screen.width, 0.7f)
                .setY(Screen.height, 0.5f)
                .setMoveType(MoveType.slide)
                .build();
    }

    private void startTimer() {
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTimeMillis;
                tvRecordingTime.setText(formatTime(elapsedTime));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timeUpdateRunnable);
    }

    private void stopTimer() {
        if (timeUpdateRunnable != null) {
            handler.removeCallbacks(timeUpdateRunnable);
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void showEditNoteDialog(Recording recording) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_note);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_note, null);
        EditText etNote = view.findViewById(R.id.etNote);
        etNote.setText(recording.getNote());
        etNote.setHint(R.string.note_hint);

        builder.setView(view);
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String note = etNote.getText().toString().trim();
            recording.setNote(note);
            recordingAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private class RecordingAdapter extends ArrayAdapter<Recording> {
        public RecordingAdapter(Context context, List<Recording> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_recording, parent, false);
            }

            Recording recording = getItem(position);
            if (recording == null) return convertView;

            TextView tvDuration = convertView.findViewById(R.id.tvDuration);
            TextView tvTime = convertView.findViewById(R.id.tvTime);
            TextView tvNote = convertView.findViewById(R.id.tvNote);
            Button btnPlay = convertView.findViewById(R.id.btnPlay);
            Button btnEdit = convertView.findViewById(R.id.btnEdit);
            Button btnDelete = convertView.findViewById(R.id.btnDelete);

            tvDuration.setText(formatTime(recording.getDuration()));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            tvTime.setText(sdf.format(recording.getRecordingTime()));
            tvNote.setText(recording.getNote());

            btnPlay.setOnClickListener(v -> {
                // 播放录音逻辑
                Toast.makeText(getContext(), "播放录音", Toast.LENGTH_SHORT).show();
            });

            btnEdit.setOnClickListener(v -> {
                showEditNoteDialog(recording);
            });

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.delete_recording)
                        .setMessage(R.string.confirm_delete)
                        .setPositiveButton(R.string.save, (dialog, which) -> {
                            recordingList.remove(position);
                            notifyDataSetChanged();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });

            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        stopTimer();
        FloatWindow.destroy();
    }
}
