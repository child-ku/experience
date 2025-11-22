# 录音软件

一个功能完整的Android录音软件，支持录音、播放、编辑备注和删除录音等功能。

## 功能特点

1. **录音列表**：主页显示所有录音文件，包括录音时长、录音时间和备注内容
2. **下拉刷新**：使用XCPullToLoadMoreListView实现下拉刷新功能
3. **录音操作**：点击录音记录可以播放录音，支持编辑备注和删除录音
4. **悬浮控制**：使用FloatWindow实现悬浮按钮，支持开始、暂停和结束录音

## 技术实现

- **编译版本**：compileSdkVersion 32
- **支持版本**：minSdkVersion 26 到 targetSdkVersion 29
- **Gradle版本**：7.5
- **Gradle Build Tools**：7.4.2
- **依赖库**：
  - XCPullToLoadMoreListView：https://github.com/jczmdeveloper/XCPullToLoadMoreListView
  - FloatWindow：https://github.com/yhaolpz/FloatWindow

## 权限要求

- RECORD_AUDIO：录音权限
- WRITE_EXTERNAL_STORAGE：写入存储权限
- READ_EXTERNAL_STORAGE：读取存储权限
- SYSTEM_ALERT_WINDOW：悬浮窗权限
- FOREGROUND_SERVICE：前台服务权限

## 使用方法

1. 安装应用后，首次启动会请求必要的权限
2. 点击悬浮窗的"开始"按钮开始录音
3. 点击"暂停"按钮暂停录音，点击"停止"按钮结束录音
4. 主页会显示所有录音文件，点击文件可以播放
5. 点击"编辑"按钮可以修改录音备注
6. 点击"删除"按钮可以删除录音文件
7. 下拉列表可以刷新录音文件列表

## 项目结构

```
├── app
│   ├── src
│   │   ├── main
│   │   │   ├── java/com/example/audiorecorder
│   │   │   │   ├── MainActivity.java          # 主界面
│   │   │   │   ├── RecordingService.java     # 录音服务
│   │   │   │   ├── RecordingItem.java        # 录音数据模型
│   │   │   │   └── RecordingAdapter.java     # 列表适配器
│   │   │   ├── res
│   │   │   │   ├── layout
│   │   │   │   │   ├── activity_main.xml     # 主界面布局
│   │   │   │   │   ├── item_recording.xml    # 列表项布局
│   │   │   │   │   └── float_window.xml      # 悬浮窗布局
│   │   │   │   ├── drawable
│   │   │   │   │   ├── float_window_background.xml  # 悬浮窗背景
│   │   │   │   │   ├── button_background.xml        # 按钮背景
│   │   │   │   │   └── ic_mic.xml                   # 麦克风图标
│   │   │   │   └── values
│   │   │   │       ├── colors.xml            # 颜色定义
│   │   │   │       ├── strings.xml           # 字符串定义
│   │   │   │       └── styles.xml            # 样式定义
│   │   │   └── AndroidManifest.xml           # 应用配置
│   └── build.gradle                          # 模块配置
├── build.gradle                              # 项目配置
├── settings.gradle                           # 项目设置
└── gradle.properties                         # Gradle属性
```

## 注意事项

- 录音文件默认保存在SD卡的AudioRecorder目录下
- 录音格式为MP3
- 悬浮窗需要用户手动授权
- 建议在使用前检查所有权限是否已授予