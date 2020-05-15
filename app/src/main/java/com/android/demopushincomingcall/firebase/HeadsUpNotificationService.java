package com.android.demopushincomingcall.firebase;

/**
 * Created by NguyenLinh on 14,May,2020
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.demopushincomingcall.IncomingActivity;
import com.android.demopushincomingcall.R;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static androidx.core.app.NotificationCompat.DEFAULT_SOUND;
import static androidx.core.app.NotificationCompat.DEFAULT_VIBRATE;

public class HeadsUpNotificationService extends Service {
    private Timer timer = new Timer();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle data = null;
        if (intent != null && intent.getExtras() != null) {
            data = intent.getBundleExtra(ConfigKey.FCM_DATA_KEY);
        }
        try {
            Intent receiveCallAction = new Intent(getApplication(), HeadsUpNotificationActionReceiver.class);
            receiveCallAction.putExtra(ConfigKey.CALL_RESPONSE_ACTION_KEY, ConfigKey.CALL_RECEIVE_ACTION);
            receiveCallAction.putExtra(ConfigKey.FCM_DATA_KEY, data);
            receiveCallAction.setAction("RECEIVE_CALL");

            Intent cancelCallAction = new Intent(getApplication(), HeadsUpNotificationActionReceiver.class);
            cancelCallAction.putExtra(ConfigKey.CALL_RESPONSE_ACTION_KEY, ConfigKey.CALL_CANCEL_ACTION);
            cancelCallAction.putExtra(ConfigKey.FCM_DATA_KEY, data);
            cancelCallAction.setAction("CANCEL_CALL");

            PendingIntent receiveCallPendingIntent = PendingIntent.getBroadcast(getApplication(), 1200, receiveCallAction, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent cancelCallPendingIntent = PendingIntent.getBroadcast(getApplication(), 1201, cancelCallAction, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent fullScreenIntent = new Intent(this, IncomingActivity.class);
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                    fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            createChannel();
            // setFullScreenIntent
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#setFullScreenIntent(android.app.PendingIntent,%20boolean)
            NotificationCompat.Builder notificationBuilder = null;
            notificationBuilder = new NotificationCompat.Builder(this, ConfigKey.CHANNEL_ID)
                    .setContentText("Test Call ")
                    .setContentTitle("Incoming Voice Call")
                    .setSmallIcon(android.R.drawable.sym_call_incoming)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .addAction(android.R.drawable.sym_action_call, "Receive Call", receiveCallPendingIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel call", cancelCallPendingIntent)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(DEFAULT_SOUND | DEFAULT_VIBRATE)
                    .setSound(Uri.parse("android.resource://" + getApplication().getPackageName() + "/" + R.raw.voip_ringtone));
            //.setFullScreenIntent(fullScreenPendingIntent, false);

            Notification incomingCallNotification = null;
            if (notificationBuilder != null) {
                incomingCallNotification = notificationBuilder.build();
            }
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = Build.VERSION.SDK_INT >= 20 ? pm.isInteractive() : pm.isScreenOn();
            Log.e("screen on.......", "" + isScreenOn);
            if (!isScreenOn) {
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "MyLock");
                wl.acquire(10000);
                PowerManager.WakeLock wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyCpuLock");

                wl_cpu.acquire(10000);
            }
            startForeground(9999, incomingCallNotification);
            TimerTask task = new TimerTask() {

                @Override
                public void run() {
                    NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(9999);
                    Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    sendBroadcast(it);
                    stopSelf();
                    Log.d("TimerTask", "Cancel Notification");
                }
            };

            timer.schedule(task, 15000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        timer.cancel();
    }

    /*
      Create noticiation channel if OS version is greater than or eqaul to Oreo
    */
    public void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(ConfigKey.CHANNEL_ID, ConfigKey.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(ConfigKey.CHANNEL_NAME);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setSound(Uri.parse("android.resource://" + getApplication().getPackageName() + "/" + R.raw.voip_ringtone),
                    new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setLegacyStreamType(AudioManager.STREAM_RING)
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build());
            Objects.requireNonNull(getApplication().getSystemService(NotificationManager.class)).createNotificationChannel(channel);
        }
    }
}
