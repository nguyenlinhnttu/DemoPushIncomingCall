package com.android.demopushincomingcall.firebase;


import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.android.demopushincomingcall.IncomingActivity;
import com.android.demopushincomingcall.MyApplication;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by NguyenLinh on 01,October,2018
 */
public class FirebaseMsgService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification() == null ? "" : remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification() == null ? "" : remoteMessage.getNotification().getBody();
            Log.i("onMessageReceived", remoteMessage.getNotification().toString());
            Log.i("onMessageReceived", remoteMessage.getNotification().getBody());
            // sendNotification(title, body);
        } else {
            //In background we use getData.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && MyApplication.Companion.isBackground() ) {
                startForegroundService(new Intent(this, HeadsUpNotificationService.class));
            } else {
                final Intent intent = new Intent(this, IncomingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(ConfigKey.FCM_DATA_KEY, remoteMessage);
                intent.setAction("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.LAUNCHER");
                getApplication().startActivity(intent);
                Log.i("onMessageReceived", "Notification");
            }
        }
    }

}
