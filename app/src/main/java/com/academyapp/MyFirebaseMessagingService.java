package com.academyapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String type = remoteMessage.getData().get("type");
        if ("incoming_call".equals(type)) {
            buildIncomingCallNotification(remoteMessage);
        } else if ("message".equals(type)) {
            buildMessageNotification(remoteMessage);
        } else if (remoteMessage.getNotification() != null) {
            buildStandardNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }
    }

    private void buildIncomingCallNotification(RemoteMessage remoteMessage) {
        String callerName = remoteMessage.getData().get("callerName");
        String channelName = remoteMessage.getData().get("channelName");
        boolean isVideo = Boolean.parseBoolean(remoteMessage.getData().get("isVideo"));
        
        Intent fullScreenIntent = new Intent(this, MainActivity.class);
        fullScreenIntent.putExtra("incomingCall", true);
        fullScreenIntent.putExtra("channelName", channelName);
        fullScreenIntent.putExtra("isVideo", isVideo);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        Intent rejectIntent = new Intent(this, CallActionReceiver.class);
        rejectIntent.setAction("com.academyapp.REJECT_CALL");
        rejectIntent.putExtra("channelName", channelName);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(this, 1, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        String channelId = "incoming_calls";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH);
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            channel.setSound(ringtoneUri, null);
            channel.enableVibration(true);
            nm.createNotificationChannel(channel);
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("مكالمة واردة من " + callerName)
            .setContentText(isVideo ? "مكالمة فيديو" : "مكالمة صوتية")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_call, "رد", fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_delete, "رفض", rejectPendingIntent);
            
        nm.notify(999, builder.build());
    }

    private void buildMessageNotification(RemoteMessage remoteMessage) {
        buildStandardNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"));
    }

    private void buildStandardNotification(String title, String body) {
        String channelId = "messages";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Messages", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }
        
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title != null ? title : "رسالة جديدة")
            .setContentText(body != null ? body : "")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
            
        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}
