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
import androidx.core.app.RemoteInput;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String type = remoteMessage.getData().get("type");
        if ("incoming_call".equals(type)) {
            buildIncomingCallNotification(remoteMessage);
        } else {
            buildMessageNotification(remoteMessage);
        }
    }

    private void buildMessageNotification(RemoteMessage remoteMessage) {
        String title = remoteMessage.getData().get("fromName");
        String body = remoteMessage.getData().get("body");
        String fromUid = remoteMessage.getData().get("fromUid");

        String channelId = "messages_channel";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Messages", NotificationManager.IMPORTANCE_HIGH);
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            channel.setSound(sound, null);
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // زرار الرد
        RemoteInput remoteInput = new RemoteInput.Builder("key_text_reply").setLabel("رد سريع...").build();
        Intent replyIntent = new Intent(this, ReplyReceiver.class);
        replyIntent.putExtra("toUserId", fromUid);
        replyIntent.putExtra("fromName", title);
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(this, 1, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send, "رد", replyPendingIntent)
                .addRemoteInput(remoteInput).build();

        // زرار الكتم
        Intent muteIntent = new Intent(this, MuteReceiver.class);
        PendingIntent mutePendingIntent = PendingIntent.getBroadcast(this, 2, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title != null ? title : "رسالة جديدة")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(replyAction)
            .addAction(android.R.drawable.ic_lock_silent_mode, "كتم", mutePendingIntent);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void buildIncomingCallNotification(RemoteMessage remoteMessage) {
        // ... (كود المكالمات القديم الخاص بك) ...
    }
}
