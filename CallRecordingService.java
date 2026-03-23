
```java name=app/src/main/java/com/stealthrecorder/CallRecordingService.java
package com.stealthrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.IOException;

public class CallRecordingService extends Service {
    private static final String TAG = "CallRecordingService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "call_recorder_channel";
    private TelephonyManager telephonyManager;
    private MediaRecorder mediaRecorder;
    private File recordingFile;
    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Recorder Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for call recording service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Stealth Call Recorder")
                .setContentText("تسجيل المكالمات يعمل في الخلفية")
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    if (isRecording) stopRecording();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (!isRecording) startRecording(phoneNumber);
                    break;
            }
        }
    };

    private void startRecording(String phoneNumber) {
        try {
            File recordingsDir = new File(getFilesDir(), "call_recordings");
            if (!recordingsDir.exists()) {
                if (!recordingsDir.mkdirs()) {
                    Log.e(TAG, "Failed to create recordings directory");
                    return;
                }
            }
            String safePhoneNumber = phoneNumber != null ? phoneNumber.replaceAll("[^0-9]", "") : "unknown";
            String fileName = "call_" + System.currentTimeMillis() + "_" + safePhoneNumber + ".mp4";
            recordingFile = new File(recordingsDir, fileName);

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(recordingFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Log.d(TAG, "Recording started: " + recordingFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "startRecording failed", e);
        } catch (RuntimeException e) {
            Log.e(TAG, "MediaRecorder runtime error", e);
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                Log.d(TAG, "Recording stopped: " + recordingFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "stopRecording failed", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        if (mediaRecorder != null) {
            mediaRecorder.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
```
