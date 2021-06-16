package com.monkeybits.edilcloud;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Map;

public class MyFireBaseNotification extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("--->notification rcv","remote"+remoteMessage);
        if (remoteMessage.getNotification()!=null){
            generateNotification(remoteMessage);
        }
        super.onMessageReceived(remoteMessage);

    }


    private void generateNotification(RemoteMessage remoteMessage) {
        Map<String, String> params = remoteMessage.getData();
        JSONObject data = new JSONObject(params);

        Intent intent = new Intent(this, MainActivity.class);
        if (data.has("redirect_url")){
            intent.putExtra("url",data.optString("redirect_url"));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

//        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/" + R.raw.edilcloud);
        Uri soundUri =  Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.edilcloud);

        Notification notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentText(remoteMessage.getNotification().getBody())
                .setContentTitle(remoteMessage.getNotification().getTitle())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setContentIntent(pendingIntent).build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    getString(R.string.default_notification_channel_id),
                    "name",
                    NotificationManager.IMPORTANCE_HIGH
            );
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes. CONTENT_TYPE_SONIFICATION )
                    .setUsage(AudioAttributes. USAGE_ALARM )
                    .build() ;
            channel.setSound(soundUri,audioAttributes);
            notificationManager.createNotificationChannel(channel);

            int importance = notificationManager.getImportance();
            boolean soundAllowed = importance < 0 || importance >= NotificationManager.IMPORTANCE_DEFAULT;
            Log.d("--->sound",""+soundAllowed);
        }
        notificationManager.notify(0, notificationBuilder);

    }

}

//mWebView.post(new Runnable() {
//                @Override
//                public void run() {
//                    mWebView.loadUrl(data.optString("redirect_url"));
//                }
//            });
