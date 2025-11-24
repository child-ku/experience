# Voice Recorder

An Android voice recording application with floating window control.

## Features

1. **Recording List**: Display all recording records with duration, time, and notes.
2. **Pull to Refresh**: Support pull-down refresh to load more records.
3. **Recording Control**: Use floating window to start, pause, and stop recording.
4. **Playback**: Play recorded audio files.
5. **Edit Notes**: Edit notes for recording records.
6. **Delete**: Delete unwanted recording records.

## Dependencies

- **XCPullToLoadMoreListView**: For pull-down refresh functionality.
- **FloatWindow**: For floating window control.
- **Room**: For local database storage.

## Build Configuration

- **compileSdkVersion**: 32
- **minSdkVersion**: 26
- **targetSdkVersion**: 29
- **Gradle**: 7.5
- **Gradle Build Tools**: 7.4.2

## Installation

1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the application.

## Usage

1. **Start Recording**: Click the "Start" button in the floating window.
2. **Pause Recording**: Click the "Pause" button in the floating window.
3. **Stop Recording**: Click the "Stop" button in the floating window.
4. **Play Recording**: Click on a recording record in the list and select "Play".
5. **Edit Note**: Click on a recording record in the list and select "Edit Note".
6. **Delete Recording**: Click on a recording record in the list and select "Delete".

## Permissions

- RECORD_AUDIO: For audio recording.
- WRITE_EXTERNAL_STORAGE: For saving recording files.
- READ_EXTERNAL_STORAGE: For reading recording files.
- SYSTEM_ALERT_WINDOW: For displaying floating window.

## License

MIT
