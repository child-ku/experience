package com.example.audiorecorder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.PermissionListener;
import com.yhao.floatwindow.Screen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW
    };

    private XRecyclerView mRecyclerView;
    private RecordingAdapter mAdapter;
    private List<RecordingItem> mRecordingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查权限
        if (!checkPermissions()) {
            requestPermissions();
        }

        // 初始化列表
        initRecyclerView();

        // 加载录音文件
        loadRecordingFiles();

        // 初始化悬浮窗
        initFloatWindow();
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
            if (!allGranted) {
                Toast.makeText(this, "需要所有权限才能正常使用", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initRecyclerView() {
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setPullRefreshEnabled(true);
        mRecyclerView.setLoadingMoreEnabled(true);

        mRecordingList = new ArrayList<>();
        mAdapter = new RecordingAdapter(this, mRecordingList);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                // 下拉刷新
                loadRecordingFiles();
                mRecyclerView.refreshComplete();
            }

            @Override
            public void onLoadMore() {
                // 上拉加载更多（这里不需要，因为是本地文件）
                mRecyclerView.loadMoreComplete();
            }
        });

        // 设置列表项点击事件
        mAdapter.setOnItemClickListener(new RecordingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecordingItem item) {
                // 播放录音
                playRecording(item);
            }

            @Override
            public void onEditClick(RecordingItem item) {
                // 编辑备注
                showEditDialog(item);
            }

            @Override
            public void onDeleteClick(RecordingItem item) {
                // 删除录音
                showDeleteDialog(item);
            }
        });
    }

    private void loadRecordingFiles() {
        mRecordingList.clear();
        File directory = new File(Environment.getExternalStorageDirectory(), "AudioRecorder");
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".mp3") || file.getName().endsWith(".3gp")) {
                        RecordingItem item = new RecordingItem();
                        item.setFilePath(file.getAbsolutePath());
                        item.setFileName(file.getName());
                        item.setDuration(getDurationFromFileName(file.getName()));
                        item.setTime(getTimeFromFileName(file.getName()));
                        item.setNote(getNoteFromFileName(file.getName()));
                        mRecordingList.add(item);
                    }
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private long getDurationFromFileName(String fileName) {
        // 从文件名提取时长（格式：recording_YYYYMMDD_HHmmss_duration_note.mp3）
        try {
            String[] parts = fileName.split("_");
            if (parts.length >= 4) {
                return Long.parseLong(parts[3]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getTimeFromFileName(String fileName) {
        // 从文件名提取时间（格式：recording_YYYYMMDD_HHmmss_duration_note.mp3）
        try {
            String[] parts = fileName.split("_");
            if (parts.length >= 3) {
                String datePart = parts[1];
                String timePart = parts[2];
                return datePart.substring(0, 4) + "/" + datePart.substring(4, 6) + "/" + datePart.substring(6, 8) +
                        " " + timePart.substring(0, 2) + ":" + timePart.substring(2, 4) + ":" + timePart.substring(4, 6);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }

    private String getNoteFromFileName(String fileName) {
        // 从文件名提取备注（格式：recording_YYYYMMDD_HHmmss_duration_note.mp3）
        try {
            String[] parts = fileName.split("_");
            if (parts.length >= 5) {
                String notePart = fileName.substring(fileName.indexOf(parts[4]));
                return notePart.substring(0, notePart.lastIndexOf("."));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void initFloatWindow() {
        View floatView = View.inflate(this, R.layout.float_window, null);
        FloatWindow.with(getApplicationContext())
                .setView(floatView)
                .setWidth(Screen.width, 0.2f)
                .setHeight(Screen.width, 0.2f)
                .setX(Screen.width, 0.8f)
                .setY(Screen.height, 0.3f)
                .setMoveType(MoveType.slide)
                .setPermissionListener(new PermissionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "悬浮窗权限获取成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFail() {
                        Toast.makeText(MainActivity.this, "悬浮窗权限获取失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        // 设置悬浮窗按钮点击事件
        floatView.findViewById(R.id.btn_start).setOnClickListener(v -> {
            startRecording();
        });

        floatView.findViewById(R.id.btn_pause).setOnClickListener(v -> {
            pauseRecording();
        });

        floatView.findViewById(R.id.btn_stop).setOnClickListener(v -> {
            stopRecording();
        });
    }

    private void startRecording() {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction(RecordingService.ACTION_START);
        startService(intent);
        Toast.makeText(this, "开始录音", Toast.LENGTH_SHORT).show();
    }

    private void pauseRecording() {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction(RecordingService.ACTION_PAUSE);
        startService(intent);
        Toast.makeText(this, "暂停录音", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction(RecordingService.ACTION_STOP);
        startService(intent);
        Toast.makeText(this, "停止录音", Toast.LENGTH_SHORT).show();
        // 重新加载列表
        loadRecordingFiles();
    }

    private void playRecording(RecordingItem item) {
        // 播放录音逻辑
        Toast.makeText(this, "播放录音: " + item.getFileName(), Toast.LENGTH_SHORT).show();
    }

    private void showEditDialog(RecordingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑备注");

        final EditText input = new EditText(this);
        input.setText(item.getNote());
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String newNote = input.getText().toString();
            // 更新文件名
            renameFile(item, newNote);
            // 重新加载列表
            loadRecordingFiles();
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void renameFile(RecordingItem item, String newNote) {
        File oldFile = new File(item.getFilePath());
        if (oldFile.exists()) {
            // 创建新文件名
            String[] parts = oldFile.getName().split("_");
            if (parts.length >= 4) {
                String newFileName = parts[0] + "_" + parts[1] + "_" + parts[2] + "_" + parts[3] + "_" + newNote + ".mp3";
                File newFile = new File(oldFile.getParent(), newFileName);
                if (oldFile.renameTo(newFile)) {
                    Toast.makeText(this, "备注更新成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "备注更新失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showDeleteDialog(RecordingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("删除录音");
        builder.setMessage("确定要删除这条录音吗？");

        builder.setPositiveButton("确定", (dialog, which) -> {
            File file = new File(item.getFilePath());
            if (file.exists() && file.delete()) {
                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                // 重新加载列表
                loadRecordingFiles();
            } else {
                Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}