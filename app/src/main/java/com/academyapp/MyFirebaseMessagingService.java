package com.academyapp;
import android.content.Context;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.text.Html;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.net.Uri;
import android.media.RingtoneManager;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "mrahmed_notifications";
    private static final String CHANNEL_NAME = "إشعارات MR AHMED";
    private static final String CHANNEL_DESC = "إشعارات الحصص والبث والامتحانات";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        if (remoteMessage.getData().containsKey("type") && "incoming_call".equals(remoteMessage.getData().get("type"))) {
            buildIncomingCallNotification(remoteMessage);
        } else {
            showGeneralNotification(remoteMessage);
        }
    }

    private void showNotification(String title, String body, String imageUrl, String linkType, String linkId, String type, String fromUserId, String fromName, String conversationId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.setLightColor(0xFF6366F1);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 150, 250});
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (!linkType.isEmpty()) intent.putExtra("linkType", linkType);
        if (!linkId.isEmpty()) intent.putExtra("linkId", linkId);
        if (!fromUserId.isEmpty()) intent.putExtra("fromUserId", fromUserId);

        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // عنوان مميز بلون + خط كبير
        String styledTitle = title;
        if (type.equals("dm") || type.equals("public_chat")) {
            styledTitle = "💬 " + title;
        } else if (linkType.equals("lesson")) {
            styledTitle = "📹 " + title;
        } else if (linkType.equals("exam")) {
            styledTitle = "📝 " + title;
        } else if (linkType.equals("recit")) {
            styledTitle = "🎤 " + title;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(styledTitle)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(body)
                .setBigContentTitle(styledTitle))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(Color.parseColor("#EC4899"))
            .setColorized(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(pendingIntent);

        // زر الرد للرسائل
        if (type.equals("dm") || type.equals("public_chat")) {
            RemoteInput remoteInput = new RemoteInput.Builder(ReplyReceiver.KEY_REPLY_TEXT)
                .setLabel("اكتب رد سريع...")
                .build();

            Intent replyIntent = new Intent(this, ReplyReceiver.class);
            replyIntent.putExtra("conversationId", conversationId);
            replyIntent.putExtra("toUserId", fromUserId);
            replyIntent.putExtra("isPublicChat", type.equals("public_chat"));
            replyIntent.putExtra("notifId", requestCode);

            PendingIntent replyPending = PendingIntent.getBroadcast(
                this, requestCode + 1, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

            NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher, "رد", replyPending)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

            builder.addAction(replyAction);
        }

        // زر فتح التطبيق
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
            this, requestCode + 2, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action openAction = new NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher, "فتح", openPending).build();
        builder.addAction(openAction);

        // صورة كبيرة
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Bitmap bitmap = getBitmapFromURL(imageUrl);
            if (bitmap != null) {
                builder.setLargeIcon(bitmap);
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon((Bitmap) null)
                    .setBigContentTitle(styledTitle)
                    .setSummaryText(body));
            }
        }

        try {
            NotificationManagerCompat.from(this).notify(requestCode, builder.build());
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            input.close();
            connection.disconnect();
            return myBitmap;
        } catch (Exception e) { return null; }
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
        rejectIntent.putExtra("notifId", 999);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(this, 1, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        String channelId = "incoming_calls";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH);
            // تشغيل نغمة رنين الهاتف الافتراضية
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
    

    private void showGeneralNotification(RemoteMessage remoteMessage) {
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");
        
        if (title == null && remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }
        
        if (title == null) title = "MR AHMED";
        if (body == null) body = "لديك إشعار جديد";

        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_ONE_SHOT | android.app.PendingIntent.FLAG_IMMUTABLE);

        String channelId = "mrahmed_notifications";
        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent);

        android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "General Notifications", android.app.NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
    
}