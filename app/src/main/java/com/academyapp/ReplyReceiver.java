package com.academyapp;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.RemoteInput;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import android.util.Log;

public class ReplyReceiver extends BroadcastReceiver {
    public static final String KEY_REPLY_TEXT = "key_text_reply";
    public static final String KEY_CONV_ID = "convId";
    public static final String KEY_TO_USER_ID = "toUserId";
    public static final String KEY_NOTIF_ID = "notifId";
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            CharSequence replyText = remoteInput.getCharSequence("key_text_reply");
            String message = replyText != null ? replyText.toString() : "";
            String convId = intent.getStringExtra(KEY_CONV_ID);
            String toUserId = intent.getStringExtra(KEY_TO_USER_ID);
            int notifId = intent.getIntExtra(KEY_NOTIF_ID, 0);

            if (!message.isEmpty() && toUserId != null) {
                new Thread(() -> {
                    try {
                        URL url = new URL("https://moon-dash-theta.vercel.app/api/send-dm-notify");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        String jsonInputString = "{\"toUserId\":\"" + toUserId + "\",\"fromUserId\":\"native\",\"fromName\":\"الطالب\",\"text\":\"" + message + "\",\"conversationId\":\"" + convId + "\"}";
                        try (OutputStream os = conn.getOutputStream()) {
                            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);
                        }
                        conn.getResponseCode();
                    } catch (Exception e) {
                        Log.e("ReplyReceiver", "Error sending reply", e);
                    }
                }).start();
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && notifId != 0) nm.cancel(notifId);
            }
        }
    }
}
